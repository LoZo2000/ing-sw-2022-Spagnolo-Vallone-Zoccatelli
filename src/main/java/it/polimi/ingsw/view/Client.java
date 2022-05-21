package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.Action;
import it.polimi.ingsw.controller.Location;
import it.polimi.ingsw.messages.*;
import it.polimi.ingsw.model.*;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {

    private String ip;
    private int port;
    private Scanner stdin;

    private Message message;
    private String nickname;
    private int numPlayers, priority, studentId, studentId2, arrivalId, departureId, MNmovement, position, numCard, action, lo, rule;
    private Location arrivalType, departureType;
    private Color chosenColor;
    private boolean completeRules;
    private final Map<Action, String> movesDescription;
    private int activeCard = -1;
    private Action requestedAction = null;
    private GameReport gameReport;
    private boolean detailedVisual;

    private Boolean finished;

    public Client(String ip, int port){
        this.ip = ip;
        this.port = port;
        this.detailedVisual = false;
        this.finished = false;

        movesDescription = new HashMap<>();
        movesDescription.put(Action.MOVESTUDENT, "to move a students from the CARD to the CANTEEN of the player or an ISLAND, depending on the active card.");
        movesDescription.put(Action.EXCHANGESTUDENT, "to exchange two students between the CARD and the ENTRANCE or between the ENTRANCE and the CANTEEN.");
        movesDescription.put(Action.BLOCK_ISLAND, "to put a token on an Island that block the influence calculation when Mother Nature arrives on that island.");
        movesDescription.put(Action.BLOCK_COLOR, "to choose a color that won't be considered during the influence calculation in this turn.");
        movesDescription.put(Action.PUT_BACK, "to choose a color of students: up to 3 students of that color for each player will be put back in the bag.");
        movesDescription.put(Action.ISLAND_INFLUENCE, "to choose an Island of which will be calculated the influence without moving Mother Nature.");
    }

    public void run() throws IOException {
        Socket socket = new Socket(ip, port);
        System.out.println("Connection established");
        Scanner socketIn = new Scanner(socket.getInputStream());
        InputStream inputStream = socket.getInputStream();
        ObjectInputStream objectInputStream;
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = null;
        GameReport report;
        boolean activeClient = true;

        //AnsiConsole.systemInstall();

        activeCard = -1;

        stdin = new Scanner(System.in);
        try{
            do {
                if(gameReport == null || (gameReport.getError() != null && gameReport.getError().equals("This nickname is already taken"))) {
                    message = getInputStart();
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(message);
                }
                objectInputStream = new ObjectInputStream(inputStream);
                report = (GameReport) objectInputStream.readObject();
                showBoard(report);

                if(report.getError() == null || !report.getError().equals("This nickname is already taken"))
                    gameReport = report;

                if(report.getFinishedGame())
                    activeClient = false;

            }while(report.getTurnOf() == null || !report.getTurnOf().equals(nickname));

            while (activeClient) {
                message = getInput();
                if (message != null) {
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    objectOutputStream.writeObject(message);
                    do {
                        objectInputStream = new ObjectInputStream(inputStream);
                        report = (GameReport) objectInputStream.readObject();
                        showBoard(report);

                        if(report.getError() == null) {
                            gameReport = report;
                            activeCard = report.getActiveCard();
                            if (activeCard != -1) {
                                requestedAction = report.getRequestedAction();
                            } else {
                                requestedAction = null;
                            }
                        }

                        if(report.getFinishedGame())
                            activeClient = false;

                    }while(!report.getFinishedGame() && !report.getTurnOf().equals(nickname));
                }
            }
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Connection closed from the client side");
        } finally {
            socketIn.close();
            objectOutputStream.close();
            socket.close();
            System.out.println("Thanks for playing!");
            stdin.close();
            AnsiConsole.systemUninstall();
        }
    }

    private Message getInputStart(){
        System.out.println(Ansi.ansi().eraseScreen().toString());
        System.out.println("Welcome in the magic world of Eriantys!\nPlease, insert your nickname:");
        nickname = stdin.nextLine();
        do {
            System.out.println("How many players do you want to play with? (a number between 2 and 4)");
            try{
                numPlayers = Integer.parseInt(stdin.nextLine());
            } catch (NumberFormatException e){
                numPlayers = -1;
            }
        }while(numPlayers<2 || numPlayers>4);

        System.out.println("Do you want to play by simple (type 0) or complete rules (type 1)?");
        rule = stdin.nextInt();
        stdin.nextLine();

        completeRules = rule == 1;

        return new AddMeMessage(nickname, completeRules, numPlayers);
    }

    private Message getInput(){
        System.out.println("Select one of the following options (type an integer):");
        if(activeCard == -1) {
            System.out.println("\t-'1) PLAYCARD' to play an assistant-card");
            System.out.println("\t-'2) MOVESTUDENT' to move a student from your ENTRANCE to your CANTEEN or an ISLAND");
            System.out.println("\t-'3) MOVEMOTHERNATURE' to move Mother Nature");
            System.out.println("\t-'4) SELECTCLOUD' to refill your ENTRANCE with students from a cloud");

            if(completeRules) {
                System.out.println("\t-'5) USEPOWER' to select a character card and activate its power.");
                if(!detailedVisual)
                    System.out.println("\t-'6) DETAILED CARD INFO' to print the detailed information about the draw characters");
                else
                    System.out.println("\t-'6) CLOSE DETAILED CARD INFO' to return to normal view");
            }
        } else{
            System.out.println("\t-'1) "+requestedAction+"' "+movesDescription.get(requestedAction));
            if(!detailedVisual)
                System.out.println("\t-'2) DETAILED CARD INFO' to print the detailed information about the draw characters");
            else
                System.out.println("\t-'2) CLOSE DETAILED CARD INFO' to return to normal view");
        }
        try {
            action = Integer.parseInt(stdin.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("The inserted value isn't a valid number");
            return null;
        }

        if(activeCard == -1) {
            switch (action) {
                case 1:
                    System.out.println("Choose which card do you want to play (priority):");
                    try {
                        priority = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new PlayCardMessage(nickname, priority);
                case 2:
                    System.out.println("Which student do you want to move? (studentId)");
                    try {
                        studentId = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        lo = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    departureType = getLocation(lo);
                    departureId = getLocationId(departureType);
                    //sc.nextLine();
                    System.out.println("Where do you want to move the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                    try {
                        lo = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    arrivalType = getLocation(lo);
                    arrivalId = getLocationId(arrivalType);

                    return new MoveStudentMessage(nickname, studentId, departureType, departureId, arrivalType, arrivalId);
                case 3:
                    System.out.println("Insert Mother Nature's movement:");
                    try {
                        MNmovement = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new MoveMotherNatureMessage(nickname, MNmovement);
                case 4:
                    System.out.println("Select which cloud you have chosen: (position)");
                    try {
                        position = Integer.parseInt(stdin.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("The inserted value isn't a valid number");
                        return null;
                    }
                    return new SelectCloudMessage(nickname, position);
                case 5:
                    if (completeRules) {
                        System.out.print("Choose which card you want to activate (0,1,2): ");
                        numCard = -1;
                        try {
                            numCard = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
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
                            studentId = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            lo = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        departureType = getLocation(lo);
                        departureId = getLocationId(departureType);
                        //sc.nextLine();
                        System.out.println("Where do you want to move the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            lo = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        arrivalType = getLocation(lo);
                        arrivalId = getLocationId(arrivalType);

                        return new MoveStudentMessage(nickname, studentId, departureType, departureId, arrivalType, arrivalId);
                    }
                    case EXCHANGESTUDENT -> {
                        System.out.print("Which student do you want to move? (studentId) ");
                        try {
                            studentId = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }

                        System.out.println("Where is the student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            lo = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        departureType = getLocation(lo);
                        departureId = getLocationId(departureType);

                        System.out.println("Which is the other student do you want to move? (studentId) ");
                        try {
                            studentId2 = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }

                        System.out.println("Where is the this second student? (type 1 for ENTRANCE, 2 for CANTEEN, 3 for ISLAND, 4 for CARD_ISLAND, 5 for CARD_CANTEEN, 6 for CARD_EXCHANGE)");
                        try {
                            lo = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        arrivalType = getLocation(lo);
                        arrivalId = getLocationId(arrivalType);

                        return new ExchangeStudentMessage(nickname, studentId, studentId2, departureType, departureId, arrivalType, arrivalId);
                    }
                    case ISLAND_INFLUENCE -> {
                        System.out.println("Choose island: (islandId)");
                        try {
                            arrivalId = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        return new IslandInfluenceMessage(nickname, arrivalId);
                    }
                    case BLOCK_ISLAND -> {
                        System.out.println("Choose island: (islandId)");
                        try {
                            arrivalId = Integer.parseInt(stdin.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("The inserted value isn't a valid number");
                            return null;
                        }
                        return new BlockIslandMessage(nickname, arrivalId);
                    }
                    case BLOCK_COLOR -> {
                        System.out.println("Choose color: (Color)");
                        String color = stdin.nextLine().toUpperCase();

                        try {
                            chosenColor = Color.valueOf(color);
                        } catch (IllegalArgumentException e) {
                            System.out.println("The inserted value isn't a valid color");
                            return null;
                        }

                        return new BlockColorMessage(nickname, chosenColor);
                    }
                    case PUT_BACK -> {
                        System.out.println("Choose color: (Color)");
                        String color = stdin.nextLine().toUpperCase();

                        try {
                            chosenColor = Color.valueOf(color);
                        } catch (IllegalArgumentException e) {
                            System.out.println("The inserted value isn't a valid color");
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

    private int getLocationId(Location type){
        int id;
        if(type == Location.ISLAND){
            System.out.println("Choose island: (islandId)");
            try {
                id = Integer.parseInt(stdin.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("The inserted value isn't a valid number");
                return -1;
            }
        } else{
            id = -1;
        }

        return id;
    }

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
                System.out.println("Current phase: "+report.getCurrentPhase());
                System.out.println("Current player: "+report.getTurnOf());
            }
            else{
                System.out.println("The game is finished");
                System.out.println("The winner is "+report.getWinner());
            }

        } else if((report.getFinishedGame() && report.getError() != null) || this.gameReport == null) {
            System.err.println(report.getError()+"\n");
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
            col.printAll();

            System.out.println("\n\nFORBIDDEN MOVE: "+report.getError()+"\nInsert a new legal move...");
            System.out.println("Current phase: "+gameReport.getCurrentPhase());
            System.out.println("Current player: "+gameReport.getTurnOf());
        }
    }

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

        System.out.println("Current phase: "+gameReport.getCurrentPhase());
        System.out.println("Current player: "+gameReport.getTurnOf());
    }
}