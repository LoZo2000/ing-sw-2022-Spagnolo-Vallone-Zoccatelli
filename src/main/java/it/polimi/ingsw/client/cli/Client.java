package it.polimi.ingsw.client.cli;

import it.polimi.ingsw.controller.Action;
import it.polimi.ingsw.controller.Location;
import it.polimi.ingsw.controller.Phase;
import it.polimi.ingsw.messages.*;
import it.polimi.ingsw.model.*;
import it.polimi.ingsw.view.GameReport;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * This class represent the cli interface for the client
 */
public class Client {

    private final String ip;
    private final int port;
    private String nickname;
    private boolean completeRules;
    private final Map<Action, String> movesDescription;
    private int activeCard;
    private Action requestedAction;
    private Phase currentPhase = null;
    private GameReport gameReport;
    private boolean detailedVisual;

    //Lock used to synchronize the thread that provides the message that will be sent to server and the thread that receives the messages from server.
    //The thread that reads the System.in stream will always wait for a response.
    private final Object lock;

    //Lock used synchronize the thread that sends pings to server and thread that sends real messages to server
    private final Object lockWrite;
    private final AtomicBoolean started;
    private final AtomicBoolean yourTurn;
    private final AtomicBoolean canWrite;
    private final AtomicBoolean finished;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private final Scanner stdin;

    /**
     * This method is the constructor of the class
     * @param ip is the ip of the server in which run the model
     * @param port is the port of the application on the server
     */
    public Client(String ip, int port){
        this.ip = ip;
        this.port = port;

        this.lock = new Object();
        this.lockWrite = new Object();

        this.started = new AtomicBoolean();
        this.canWrite = new AtomicBoolean(true);
        this.yourTurn = new AtomicBoolean(true);
        this.finished = new AtomicBoolean();
        
        this.stdin = new Scanner(System.in);

        movesDescription = new HashMap<>();
        movesDescription.put(Action.MOVESTUDENT, "to move a students from the CARD to the CANTEEN of the player or an ISLAND, depending on the active card.");
        movesDescription.put(Action.EXCHANGESTUDENT, "to exchange two students between the CARD and the ENTRANCE or between the ENTRANCE and the CANTEEN.");
        movesDescription.put(Action.BLOCK_ISLAND, "to put a token on an Island that block the influence calculation when Mother Nature arrives on that island.");
        movesDescription.put(Action.BLOCK_COLOR, "to choose a color that won't be considered during the influence calculation in this turn.");
        movesDescription.put(Action.PUT_BACK, "to choose a color of students: up to 3 students of that color for each player will be put back in the bag.");
        movesDescription.put(Action.ISLAND_INFLUENCE, "to choose an Island of which will be calculated the influence without moving Mother Nature.");
    }

    private void initGame(){
        this.started.set(false);
        this.canWrite.set(true);
        this.yourTurn.set(true);
        this.finished.set(false);

        this.gameReport = null;
        this.activeCard = -1;
        this.requestedAction = null;
        this.detailedVisual = false;
        this.nickname = null;
        this.completeRules = false;
        this.currentPhase = null;
    }
    
    private String readLine(){
        String line = stdin.nextLine();
        if(finished.get()){
            return "FINISHED GAME";
        } else{
            return line;
        }
    }

    private void sendToServer() {
        Message message = null;
        try{
            while (!finished.get()) {
                while(!canWrite.get()) {
                    synchronized (lock) {
                        if (!yourTurn.get())
                            System.out.println("Not your Turn!");
                        lock.wait();
                    }
                }

                if(!finished.get()) {
                    if (started.get())
                        message = getInput();
                    else
                        message = getInputStart();
                }

                if (message != null && !finished.get()) {
                    canWrite.set(false);
                    try{
                        synchronized (lockWrite) {
                            //ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                            outputStream.reset();
                            outputStream.writeObject(message);
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            outputStream.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void receiveFromServer(){
        try{
            GameReport report = null;
            do {
                //ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                report = (GameReport) inputStream.readObject();

                if(report.getError() == null || !report.getError().equals("PONG")) {
                    showBoard(report);

                    if (report.getError() != null && report.getError().equals("This nickname is already taken")) {
                        synchronized (lock) {
                            yourTurn.set(true);
                            canWrite.set(true);
                            lock.notifyAll();
                        }
                    }
                }

            }while(report.getTurnOf() == null);

            gameReport = report;
            started.set(true);

            if(report.getFinishedGame()) {
                synchronized (lock) {
                    canWrite.set(true);
                    finished.set(true);
                    lock.notifyAll();
                }
            }

            if(report.getTurnOf().equals(nickname)){
                synchronized (lock) {
                    yourTurn.set(true);
                    currentPhase = report.getCurrentPhase();
                    canWrite.set(true);
                    lock.notifyAll();
                }
            } else{
                yourTurn.set(false);
            }

            while(!finished.get()){
                //ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                report = (GameReport) inputStream.readObject();

                if(report.getError() == null || !report.getError().equals("PONG")) {
                    showBoard(report);

                    if(report.getError() == null) {
                        gameReport = report;
                        activeCard = report.getActiveCard();
                        currentPhase = report.getCurrentPhase();
                        if (activeCard != -1) {
                            requestedAction = report.getRequestedAction();
                        } else {
                            requestedAction = null;
                        }
                    }

                    if(report.getFinishedGame()) {
                        synchronized (lock) {
                            canWrite.set(true);
                            finished.set(true);
                            lock.notifyAll();
                        }
                    }

                    if(report.getTurnOf().equals(nickname)){
                        synchronized (lock){
                            yourTurn.set(true);
                            canWrite.set(true);
                            lock.notifyAll();
                        }
                    } else{
                        yourTurn.set(false);
                    }
                }
            }
            inputStream.close();
        } catch(Exception e){
            System.err.println(e.getMessage());
            //e.printStackTrace();
            System.out.println("\nThe server closed the connection, wait a bit before retry starting a new game");
            System.exit(0);
            synchronized (lock){
                finished.set(true);
                canWrite.set(true);
                yourTurn.set(true);
                lock.notifyAll();
            }
        }
    }

    private void ping(){
        boolean run = true;
        while(run){
            try{
                synchronized(lockWrite) {
                    //ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                    outputStream.reset();
                    outputStream.writeObject(new PingMessage());
                    outputStream.flush();
                }
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                //Game finished
                run = false;
            } catch (IOException e) {
                //Socket error
                run = false;
            }
        }
    }

    /**
     * This method start the cli client initializing the thread to receive the input, send the output and ping the server
     * @throws IOException when there is an input/output error
     * @throws InterruptedException when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    public void startClient() throws IOException, InterruptedException {
        boolean repeat;

        AnsiConsole.systemInstall();

        do {
            initGame();

            Socket socket = new Socket(ip, port);

            socket.setSoTimeout(20000);

            //System.out.println("Connection established");
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());

            Thread input = new Thread(this::sendToServer);
            Thread output = new Thread(this::receiveFromServer);
            Thread ping = new Thread(this::ping);

            input.start();
            output.start();

            ping.setDaemon(true);
            ping.start();

            output.join();
            input.join();
            ping.interrupt();

            socket.close();

            System.out.println("Do you want to play another game?");
            System.out.println("1) Yes");
            System.out.println("2) No");
            System.out.print("Selection: ");

            int selection;
            try{
                selection = Integer.parseInt(stdin.nextLine());
            } catch (NumberFormatException e){
                selection = 0;
            }

            repeat = selection == 1;
        }while(repeat);
        System.out.println("\nThanks for Playing!");
    }

    private Message getInputStart() throws IOException {
        int numPlayers, rule;
        System.out.println(Ansi.ansi().eraseScreen().toString());
        System.out.println("Welcome in the magic world of Eriantys!\nPlease, insert your nickname:");
        nickname = readLine();
        do {
            System.out.println("How many players do you want to play with? (a number between 2 and 4)");
            try{
                numPlayers = Integer.parseInt(readLine());
            } catch (NumberFormatException e){
                numPlayers = -1;
                if(finished.get())
                    return null;
            }
        }while(numPlayers <2 || numPlayers >4);

        System.out.println("Do you want to play by simple (type 0) or complete rules (type 1)?");
        try{
            rule = Integer.parseInt(readLine());
        } catch (NumberFormatException e){
            rule = 0;
            if(finished.get())
                return null;
        }

        completeRules = rule == 1;

        return new AddMeMessage(nickname, completeRules, numPlayers);
    }

    private Message getInput() throws IOException {
        int action, studentId, studentId2, arrivalId, departureId;
        int priority, mnMovement, cloudSelected;
        int location;
        Location arrivalType, departureType;
        Color chosenColor;

        System.out.println("Select one of the following options (type an integer):");
        if(activeCard == -1 || requestedAction == Action.EXCHANGESTUDENT) {
            if(currentPhase == Phase.PRETURN)
                System.out.println("    - '1) PLAYCARD' to play an assistant-card");

            if(currentPhase == Phase.MIDDLETURN)
                System.out.println("    - '2) MOVESTUDENT' to move a student from your ENTRANCE to your CANTEEN or an ISLAND");

            if(currentPhase == Phase.MOVEMNTURN)
                System.out.println("    - '3) MOVEMOTHERNATURE' to move Mother Nature");

            if(currentPhase == Phase.ENDTURN)
                System.out.println("    - '4) SELECTCLOUD' to refill your ENTRANCE with students from a cloud");

            if(completeRules) {
                if(currentPhase == Phase.MIDDLETURN || currentPhase == Phase.MOVEMNTURN)
                    System.out.println("    - '5) USEPOWER' to select a character card and activate its power.");
                if(!detailedVisual)
                    System.out.println("    - '6) DETAILED CARD INFO' to print the detailed information about the draw characters");
                else
                    System.out.println("    - '6) CLOSE DETAILED CARD INFO' to return to normal view");

                if(requestedAction == Action.EXCHANGESTUDENT && (currentPhase == Phase.MIDDLETURN || currentPhase == Phase.MOVEMNTURN)){
                    System.out.println("    - '7) EXCHANGESTUDENT' to exchange two students between the CARD and the ENTRANCE or between the ENTRANCE and the CANTEEN.");
                }

            }
        } else{
            System.out.println("    - '1) "+requestedAction+"' "+movesDescription.get(requestedAction));
            if(!detailedVisual)
                System.out.println("    - '2) DETAILED CARD INFO' to print the detailed information about the draw characters");
            else
                System.out.println("    - '2) CLOSE DETAILED CARD INFO' to return to normal view");
        }

        System.out.println("    - '0) CLOSE GAME' to close the client. If you do that the game will end and no winner will be declared");
        System.out.print("Selection: ");
        try {
            action = Integer.parseInt(readLine());
        } catch (NumberFormatException e) {
            if(!finished.get())
                System.out.println("The inserted value isn't a valid number");
            return null;
        }

        if(activeCard == -1 || requestedAction == Action.EXCHANGESTUDENT) {
            switch (action) {
                case 1:
                    System.out.println("Choose which card do you want to play (priority):");
                    try {
                        priority = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new PlayCardMessage(nickname, priority);
                case 2:
                    System.out.println("Which student do you want to move? (studentId)");
                    try {
                        studentId = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        location = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    departureType = getLocation(location);
                    departureId = getLocationId(departureType);
                    //sc.nextLine();
                    System.out.println("Where do you want to move the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        location = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    arrivalType = getLocation(location);
                    arrivalId = getLocationId(arrivalType);

                    return new MoveStudentMessage(nickname, studentId, departureType, departureId, arrivalType, arrivalId);
                case 3:
                    System.out.println("Insert Mother Nature's movement:");
                    try {
                        mnMovement = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new MoveMotherNatureMessage(nickname, mnMovement);
                case 4:
                    System.out.println("Select which cloud you have chosen: (position)");
                    try {
                        cloudSelected = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new SelectCloudMessage(nickname, cloudSelected);
                case 5:
                    if (completeRules) {
                        System.out.print("Choose which card you want to activate (0,1,2): ");
                        int numCard = -1;
                        try {
                            numCard = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        return new UsePowerMessage(nickname, numCard);
                    } else {
                        System.out.println("You can't use characters card with simple rules");
                        return null;
                    }
                case 6:
                    if (completeRules) {
                        if(detailedVisual)
                            showBoard(gameReport);
                        else
                            showCardInfo();
                        detailedVisual = !detailedVisual;
                    } else {
                        System.out.println("You can't use characters card with simple rules");
                    }
                    return null;
                case 7:
                    System.out.print("Which student do you want to move? (studentId) ");
                    try {
                        studentId = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }

                    System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        location = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    departureType = getLocation(location);
                    departureId = getLocationId(departureType);

                    System.out.println("Which is the other student do you want to move? (studentId) ");
                    try {
                        studentId2 = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }

                    System.out.println("Where is the this second student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        location = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    arrivalType = getLocation(location);
                    arrivalId = getLocationId(arrivalType);

                    return new ExchangeStudentMessage(nickname, studentId, studentId2, departureType, departureId, arrivalType, arrivalId);
                case 0:
                    System.out.println("Are you sure you want to close the client? (The game will end for everyone in this match)");
                    System.out.println("    1) Yes");
                    System.out.println("    2) No");
                    int sel = 0;
                    while(sel != 1 && sel != 2) {
                        System.out.print("Selection: ");
                        try {
                            sel = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            sel = 0;
                        }
                    }

                    if(sel == 1){
                        System.exit(0);
                    }
                    return null;
                default:
                    System.out.println("Invalid action!");
                    return null;
            }
        } else{
            if(action == 1) {
                switch (requestedAction) {
                    case MOVESTUDENT -> {
                        System.out.println("Which student do you want to move? (studentId)");
                        try {
                            studentId = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            location = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        departureType = getLocation(location);
                        departureId = getLocationId(departureType);
                        //sc.nextLine();
                        System.out.println("Where do you want to move the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            location = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        arrivalType = getLocation(location);
                        arrivalId = getLocationId(arrivalType);

                        return new MoveStudentMessage(nickname, studentId, departureType, departureId, arrivalType, arrivalId);
                    }
                    case ISLAND_INFLUENCE -> {
                        System.out.println("Choose island: (islandId)");
                        try {
                            arrivalId = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        return new IslandInfluenceMessage(nickname, arrivalId);
                    }
                    case BLOCK_ISLAND -> {
                        System.out.println("Choose island: (islandId)");
                        try {
                            arrivalId = Integer.parseInt(readLine());
                        } catch (NumberFormatException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        return new BlockIslandMessage(nickname, arrivalId);
                    }
                    case BLOCK_COLOR -> {
                        System.out.println("Choose color: (Color)");
                        String color = readLine().toUpperCase();

                        try {
                            chosenColor = Color.valueOf(color);
                        } catch (IllegalArgumentException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }

                        return new BlockColorMessage(nickname, chosenColor);
                    }
                    case PUT_BACK -> {
                        System.out.println("Choose color: (Color)");
                        String color = readLine().toUpperCase();

                        try {
                            chosenColor = Color.valueOf(color);
                        } catch (IllegalArgumentException e) {
                            if(!finished.get())
                                System.out.println("The inserted value isn't a valid number");
                            return null;
                        }

                        return new PutBackMessage(nickname, chosenColor);
                    }
                }
            } else if(action == 2) {
                if(detailedVisual)
                    showBoard(gameReport);
                else
                    showCardInfo();
                detailedVisual = !detailedVisual;
                return null;
            } else if(action == 0){
                System.out.println("Are you sure you want to close the client? (The game will end for everyone in this match)");
                System.out.println("    1) Yes");
                System.out.println("    2) No");
                int sel = 0;
                while(sel != 1 && sel != 2) {
                    System.out.print("Selection: ");
                    try {
                        sel = Integer.parseInt(readLine());
                    } catch (NumberFormatException e) {
                        if(!finished.get())
                            System.out.println("The inserted value isn't a valid number");
                        sel = 0;
                    }
                }

                if(sel == 1){
                    System.exit(0);
                }
                return null;
            }
        }

        return null;
    }

    private Location getLocation(int location){
        switch (location) {
            case 1 -> {
                return Location.ENTRANCE;
            }
            case 2 -> {
                return Location.CANTEEN;
            }
            case 3 -> {
                return Location.ISLAND;
            }
            case 4 -> {
                return Location.CARD_ISLAND;
            }
            case 5 -> {
                return Location.CARD_CANTEEN;
            }
            case 6 -> {
                return Location.CARD_EXCHANGE;
            }
            default -> {
                return null;
            }
        }
    }

    private int getLocationId(Location type) {
        int id;
        if(type == Location.ISLAND){
            System.out.println("Choose island: (islandId)");
            try {
                id = Integer.parseInt(readLine());
            } catch (NumberFormatException e) {
                if(!finished.get())
                    System.out.println("The inserted value isn't a valid number");
                return -1;
            }
        } else{
            id = -1;
        }

        return id;
    }

    /**
     * This method is called to print the board in terminal
     * @param report is the report representing the state of the game
     */
    public void showBoard(GameReport report){
        if(report.getError() == null) {
            Columns col = new Columns();
            col.addColumn(1, "\u001B[1;31mISLANDS\u001B[0m");
            col.addColumn(1, report.getIslandsString());
            col.addColumn(2, "\u001B[1;32mPROFESSORS\u001B[0m");
            col.addColumn(2, report.getProfessorsString());

            col.addColumn(2, "\u001B[1;36mCLOUDS\u001B[0m");
            col.addColumn(2, report.getCloudsString());

            col.addColumn(2, "\u001B[1;97mOPPONENTS\u001B[0m");
            col.addColumn(2, report.getOpponentsString());

            col.addColumn(1, report.getYourDashboardString());

            if (completeRules) {
                col.addColumn(2, "\u001B[1;92mCARDS SHORT INFO\u001B[0m \n");
                col.addColumn(2, report.getShortCharacters());

                col.addColumn(2, "\u001B[1;91mCurrent Rule: \u001B[0m " + report.getActiveRule() + "\n");
            }
            col.printAll();
            if(!report.getFinishedGame()){
                System.out.println("Current phase: "+report.getCurrentPhaseString());
                System.out.println("Current player: "+report.getTurnOf());
            }
            else{
                System.out.println("The game is finished");
                System.out.println("The winner is "+report.getWinner());
            }

        } else if((report.getFinishedGame() && report.getError() != null) || this.gameReport == null) {
            System.out.println("\n\u001B[1;31m" + report.getError()+"\u001B[0m");
            if(yourTurn.get() && started.get()){
                System.out.println("\u001B[1;31mPress ENTER to continue...\u001B[0m");
            }
            System.out.println();
        } else{
            Columns col = new Columns();
            col.addColumn(1, "\u001B[1;31mISLANDS\u001B[0m");
            col.addColumn(1, gameReport.getIslandsString());
            col.addColumn(2, "\u001B[1;32mPROFESSORS\u001B[0m");
            col.addColumn(2, gameReport.getProfessorsString());

            col.addColumn(2, "\u001B[1;36mCLOUDS\u001B[0m");
            col.addColumn(2, gameReport.getCloudsString());

            col.addColumn(2, "\u001B[1;97mOPPONENTS\u001B[0m");
            col.addColumn(2, gameReport.getOpponentsString());

            col.addColumn(1, gameReport.getYourDashboardString());

            if (completeRules) {
                col.addColumn(2, "\u001B[1;92mCARDS SHORT INFO\u001B[0m \n");
                col.addColumn(2, gameReport.getShortCharacters());

                col.addColumn(2, "\u001B[1;91mCurrent Rule: \u001B[0m " + gameReport.getActiveRule() + "\n");
            }

            col.addColumn(1, "\n\u001B[1;31mFORBIDDEN MOVE: "+report.getError()+"\u001B[0m\n");

            col.printAll();

            System.out.println("Current phase: "+gameReport.getCurrentPhaseString());
            System.out.println("Current player: "+gameReport.getTurnOf());
        }
    }

    /**
     * This method is called to print the info of the cards
     */
    public void showCardInfo(){
        Columns col = new Columns();
        col.addColumn(1, "\u001B[1;31mISLANDS\u001B[0m");
        col.addColumn(1, gameReport.getIslandsString());
        col.addColumn(2, "\u001B[1;32mPROFESSORS\u001B[0m");
        col.addColumn(2, gameReport.getProfessorsString());

        col.addColumn(1, gameReport.getYourDashboardString());

        if (completeRules) {
            col.addColumn(2, "\u001B[1;92mCARDS DETAILED INFO\u001B[0m");
            col.addColumn(2, gameReport.getCharacters());

            col.addColumn(2, "\u001B[1;91mCurrent Rule: \u001B[0m " + gameReport.getActiveRule() + "\n");
        }
        col.printAll();

        System.out.println("Current phase: "+gameReport.getCurrentPhaseString());
        System.out.println("Current player: "+gameReport.getTurnOf());
    }
}