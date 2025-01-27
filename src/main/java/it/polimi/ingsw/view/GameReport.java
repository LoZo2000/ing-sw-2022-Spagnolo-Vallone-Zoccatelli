package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.Action;
import it.polimi.ingsw.controller.Phase;
import it.polimi.ingsw.model.*;
import it.polimi.ingsw.model.characters.Character;
import it.polimi.ingsw.model.exceptions.NoActiveCardException;

import java.io.Serializable;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * The class GameReport represent an object containing the state of the game, so is a class principally made of attributes
 * and getters
 */
public class GameReport implements Serializable {
    private final String turnOf;
    private Phase currentPhase;
    private final String namePlayer;
    private final String error;
    private final int remainingMoves;
    private final int remainingExchanges;
    private final int activeCard;
    private final Action requestedAction;
    private final String activeRule;
    private final Boolean finished;
    private final String winner;

    private final List<IslandReport> islands;
    private final List<CloudReport> clouds;
    private final PlayerReport player;
    private final List<OpponentReport> opponents;
    private final List<CharacterReport> characters;
    private final Map<Color, String> professors;



    //Attributes for GUI:
    private int numPlayers = 0;
    private ArrayList<Card> myCards = new ArrayList<>();
    private ArrayList<String> allPlayersNick = new ArrayList<>();
    private ColorTower myColor;
    private ArrayList<ColorTower> allPlayersColor = new ArrayList<>();
    private LinkedList<Island> allIslands = new LinkedList<>();
    private int MT;
    private ArrayList<ArrayList<Student>> studentsOnClouds = new ArrayList<>();
    private ArrayList<Student> myEntrance = new ArrayList<>();
    private ArrayList<ArrayList<Student>> opponentsEntrance = new ArrayList<>();
    private ArrayList<Integer> myCanteen = new ArrayList<>();
    private ArrayList<Integer> lastMyCanteen = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> opponentsCanteen = new ArrayList<>();
    private Card myLastCard;
    private ArrayList<Card> opponentsLastCard = new ArrayList<>();
    private int myTowers;
    private ArrayList<Integer> towersInDash = new ArrayList<>();
    private ArrayList<Character> charId = new ArrayList<>();
    private int myCoins;
    private ArrayList<Integer> opponentsCoins = new ArrayList<>();


    /**
     * Method to build an IslandReport record. This record stores all the important info about an Island
     * @param id is the id of the Island
     * @param students is a map that connect every Color with the number of Students on this Island of that corresponding Color
     * @param prohibitionToken is true if the Island is locked, false otherwise
     * @param numTowers is the number of the Towers on the Island
     * @param owner is the color of the team who owns the Island
     * @param motherNature is true if MotherNature is on the Island, false otherwise
     */
    private record IslandReport (String id, Map<Color, Integer> students, boolean prohibitionToken, int numTowers, ColorTower owner, boolean motherNature) implements Serializable{
        public Map<Color, Integer> students(){
            Map<Color, Integer> newMap = new HashMap<>();
            for(Color c : Color.values()){
                newMap.put(c, students.get(c));
            }
            return newMap;
        }

        @Override
        public String toString(){
            StringBuilder s = new StringBuilder();
            s.append("Id: ").append(id).append("    ");
            if(owner == null)
                s.append("Owner: nobody  ");
            else
                s.append("Owner: ").append(owner).append("    ");
            s.append("Towers: x").append(numTowers).append("    ");

            for(Color c : Color.values()){
                s.append(ansi().fgBright(c.ansiColor).a("(" + c.shortName + ":" + students.get(c) + ")").reset().a("   ").toString());
            }

            if(motherNature)
                s.append("MN");
            if(prohibitionToken)
                s.append(ansi().fgRed().a("BLOCK").reset().toString());

            return s.toString();
        }
    }

    /**
     * Method to build a CloudReport record. This record stores all the important info about a Cloud
     * @param id is the id of the Cloud
     * @param students is the number of Students of that Color on the Cloud
     * @param isFull is true if the Island contains 3/4 Students, false otherwise
     */
    private record CloudReport (int id, Map<Color, Integer> students, boolean isFull) implements Serializable{
        public Map<Color, Integer> students(){
            Map<Color, Integer> newMap = new HashMap<>();
            for(Color c : Color.values()){
                newMap.put(c, students.get(c));
            }
            return newMap;
        }

        @Override
        public String toString(){
            StringBuilder s = new StringBuilder();
            s.append("Cloud ").append(id).append("  =>   ");
            if(!isFull)
                s.append("Already chosen cloud");
            else {
                for(Color c : Color.values()){
                    if (students.get(c) != 0)
                        s.append(ansi().fgBright(c.ansiColor).a(c + ": " + students.get(c)).reset().a("   ").toString());
                }
            }
            return s.toString();
        }
    }

    /**
     * Method to build a OpponentReport record. This record stores all the important info about an Opponent
     * @param nickname is the Nickname of the Opponent
     * @param color is the Color of the Opponent's Team
     * @param lastCard is the last Card played by the Opponent
     * @param studentsEntrance is a Map containing the number of Students of every Color on the Opponent's Entrance
     * @param studentsCanteen is a Map containing the number of Students of every Color on the Opponent's Dashboard
     * @param remainingTowers is the number of Tower the Opponent still has
     * @param coins is the number of Coin the Opponent owns
     */
    private record OpponentReport(String nickname, ColorTower color, Card lastCard, Map<Color, Integer> studentsEntrance, Map<Color, Integer> studentsCanteen, int remainingTowers, int coins) implements Serializable{
        public Map<Color, Integer> studentsEntrance(){
            Map<Color, Integer> newMap = new HashMap<>();
            for(Color c : Color.values()){
                newMap.put(c, studentsEntrance.get(c));
            }
            return newMap;
        }

        public Map<Color, Integer> studentsCanteen(){
            Map<Color, Integer> newMap = new HashMap<>();
            for(Color c : Color.values()){
                newMap.put(c, studentsCanteen.get(c));
            }
            return newMap;
        }

        @Override
        public String toString(){
            StringBuilder s = new StringBuilder();
            s.append(ansi().bold().fgCyan().a("DASHBOARD of " + nickname).reset().a("  (Color: " + color + ")\n").toString());

            s.append(ansi().bold().a("Last Card: ").reset().toString()).append(lastCard).append("   ");
            s.append(ansi().bold().a("Towers: ").reset().toString()).append(remainingTowers).append("   ");

            if(coins != -1){
                s.append(ansi().bold().a("Coins: ").reset().toString()).append(coins).append("\n");
            } else{
                s.append("\n");
            }

            s.append(ansi().bold().a("Entrance:  ").reset().toString());
            for(Color c : Color.values()){
                s.append(ansi().fgBright(c.ansiColor).a(c + ": " + studentsEntrance.get(c)).reset().a("  ").toString());
            }

            s.append("\n");

            s.append(ansi().bold().a("Canteen:  ").reset().toString());
            for(Color c : Color.values()){
                s.append(ansi().fgBright(c.ansiColor).a(c + ": " + studentsCanteen.get(c) + "/10").reset().a("  ").toString());
            }

            return s.toString();
        }
    }

    /**
     * Method to build the Player record. This record stores all the important info about the Player
     * @param color is the color of the Player's Team
     * @param lastCard is the last Card played by the Player
     * @param remainingTowers is the number of Tower the Player still has
     * @param hand is a List containing the Player's Cards
     * @param entranceStudents is a Map containing the number of Students of every Color on the Player's Entrance
     * @param canteenStudents is a Map containing the number of Students of every Color on the Player's Dashboard
     * @param coins is the number of Coin the Opponent owns
     */
    private record PlayerReport(ColorTower color, Card lastCard, int remainingTowers, List<Card> hand, List<Student> entranceStudents, Map<Color, List<Student>> canteenStudents, int coins) implements Serializable{
        public List<Student> entranceStudents(){
            return new ArrayList<>(entranceStudents);
        }

        public Map<Color, List<Student>> canteenStudents(){
            Map<Color, List<Student>> newMap = new HashMap<>();
            for(Color c : Color.values()){
                List<Student> newList = new ArrayList<>(entranceStudents);
                newMap.put(c, newList);
            }
            return newMap;
        }

        @Override
        public String toString(){
            StringBuilder s = new StringBuilder();
            s.append(ansi().bold().fgBrightBlue().a("YOUR DASHBOARD").reset().a("  (Color: " + color + ")\n").toString());

            s.append(ansi().bold().a("Last Card: ").reset().toString()).append(lastCard).append("   ");
            s.append(ansi().bold().a("Towers: ").reset().toString()).append(remainingTowers).append("   ");

            if(coins != -1){
                s.append(ansi().bold().a("Coins: ").reset().toString()).append(coins).append("\n");
            } else{
                s.append("\n");
            }

            s.append(ansi().bold().a("Your hand:  ").reset().toString());

            for(Card c : hand){
                s.append(c).append(" ");
            }

            s.append("\n");

            s.append(ansi().bold().a("Entrance:  ").reset().a(entranceStudents).toString());

            s.append("\n");

            s.append(ansi().bold().a("Canteen:").reset().a("\n").toString());
            for(Color c : Color.values()){
                s.append(ansi().fgBright(c.ansiColor).a("   " + c + ": ").reset().a(canteenStudents.get(c)).a("\n").toString());
            }

            return s.toString();
        }
    }

    /**
     * Method to build the Character record. This record stores the description of the Character Cards
     * @param c is the description of the Character's power
     */
    private record CharacterReport(Character c) implements Serializable{
        public String shortString(){
            return c.shortString();
        }

        @Override
        public String toString(){
            return c.toString();
        }
    }

    /**
     * This method is the constructor of the class
     * @param myId is the nickname that caused this report
     * @param error is a String containing a message of error
     * @param turnOf is a String containing the nickname of the player who has the turn
     * @param finished is a boolean that report if the game is finished
     */
    public GameReport(String myId, String error, String turnOf, boolean finished){
        this.namePlayer = myId;
        this.error = error;
        this.finished =finished;
        this.winner=null;
        this.turnOf = turnOf;

        this.activeRule = null;
        this.characters = null;
        this.activeCard = -1;
        this.requestedAction = null;
        this.remainingMoves = -1;
        this.remainingExchanges = -1;
        this.currentPhase = null;
        this.player = null;
        this.islands = null;
        this.opponents = null;
        this.professors = null;
        this.clouds = null;
    }

    /**
     * This method is a constructor of the class
     * @param game represent the game containing all the information to fill the report
     * @param owner is the player that will receive the game report
     */
    public GameReport(Game game, Player owner){

        this.error = null;
        this.finished =game.getFinishedGame();
        this.winner=game.getWinner();

        islands = new ArrayList<>();
        professors = new HashMap<>();
        clouds = new ArrayList<>();
        opponents = new ArrayList<>();
        characters = new ArrayList<>();

        this.currentPhase = game.getCurrentPhase();
        this.turnOf = game.getCurrentPlayer();
        this.namePlayer = owner.getNickname();

        //ISLANDS
        int b = 8;
        int w = 8;
        int g = 6;
        if(game.getNumPlayers()==3){
            b = 6;
            w = 6;
        }
        int numI = 0;
        allIslands = game.getAllIslands();
        for(Island i : game.getAllIslands()){
            if(i.getReport().getOwner()==ColorTower.BLACK) b -= i.getReport().getTowerNumbers();
            else if(i.getReport().getOwner()==ColorTower.WHITE) w -= i.getReport().getTowerNumbers();
            else if(i.getReport().getOwner()==ColorTower.GREY) g -= i.getReport().getTowerNumbers();

            IslandReport ir;
            Map<Color, Integer> mapNumbers = new HashMap<>();
            for(Color c : Color.values()){
                mapNumbers.put(c, i.getReport().getColorStudents(c));
            }

            if(game.getMotherNaturePosition().equals(i)){
                MT = numI;
                ir = new IslandReport(i.getId(), mapNumbers, i.getProhibition(), i.getReport().getTowerNumbers(), i.getOwner(), true);
            } else{
                ir = new IslandReport(i.getId(), mapNumbers, i.getProhibition(), i.getReport().getTowerNumbers(), i.getOwner(), false);
            }
            numI++;
            islands.add(ir);
        }

        //PROFESSORS
        for(Color c : Color.values()){
            if(game.getProfessors().get(c) != null)
                professors.put(c, game.getProfessors().get(c).getNickname());
            else
                professors.put(c, "nobody");
        }

        //CLOUDS
        Cloud[] clGame = game.getAllClouds();
        for(int i = 0; i<clGame.length; i++){
            Map<Color, Integer> mapNumbers = new HashMap<>();
            for(Color c : Color.values()){
                mapNumbers.put(c, clGame[i].getNumberOfStudentPerColor(c));
            }
            clouds.add(new CloudReport(i, mapNumbers, clGame[i].isFull()));
            studentsOnClouds.add(clGame[i].getStudents());
        }

        //OTHER PLAYERS
        numPlayers = game.getNumPlayers();
        for(Player p : game.getAllPlayers()){
            allPlayersNick.add(p.getNickname());
            allPlayersColor.add(p.getColor());
            switch (p.getColor()){
                case BLACK -> towersInDash.add(b);
                case WHITE -> towersInDash.add(w);
                case GREY -> towersInDash.add(g);
            }
            opponentsEntrance.add(p.getDashboard().getEntrance().getAllStudents());
            ArrayList<Integer> prov = new ArrayList<>();
            prov.add(p.getDashboard().getCanteen().getNumberStudentColor(Color.GREEN));
            prov.add(p.getDashboard().getCanteen().getNumberStudentColor(Color.RED));
            prov.add(p.getDashboard().getCanteen().getNumberStudentColor(Color.YELLOW));
            prov.add(p.getDashboard().getCanteen().getNumberStudentColor(Color.PINK));
            prov.add(p.getDashboard().getCanteen().getNumberStudentColor(Color.BLUE));
            opponentsCanteen.add(prov);
            opponentsLastCard.add(p.getDashboard().getGraveyard());
            opponentsCoins.add(p.getCoins());

            if(!p.equals(owner)){

                List<Student> entranceStudents = p.getDashboard().getEntrance().getAllStudents();

                Map<Color, Integer> mapEntrance = new HashMap<>();
                for(Color c : Color.values()){
                    mapEntrance.put(c, 0);
                }
                for(Student s : entranceStudents){
                    mapEntrance.put(s.getColor(), mapEntrance.get(s.getColor()) + 1);
                }

                Map<Color, Integer> mapCanteen = new HashMap<>();
                for(Color c : Color.values()){
                    mapCanteen.put(c, p.getDashboard().getCanteen().getNumberStudentColor(c));
                }

                if(game.getCompleteRules())
                    opponents.add(new OpponentReport(p.getNickname(), p.getColor(), p.getDashboard().getGraveyard(), mapEntrance, mapCanteen, p.getDashboard().getTowers(), p.getCoins()));
                else
                    opponents.add(new OpponentReport(p.getNickname(), p.getColor(), p.getDashboard().getGraveyard(), mapEntrance, mapCanteen, p.getDashboard().getTowers(), -1));
            }
        }

        //PLAYER DASHBOARD
        myCoins = owner.getCoins();
        myCards = owner.getHand().getAllCards();
        myEntrance = owner.getDashboard().getEntrance().getAllStudents();

        myCanteen.add(owner.getDashboard().getCanteen().getNumberStudentColor(Color.GREEN));
        if(owner.getDashboard().getCanteen().getNumberStudentColor(Color.GREEN)==0) lastMyCanteen.add(-1);
        else lastMyCanteen.add(owner.getDashboard().getCanteen().getStudents(Color.GREEN).get(0).getId());

        myCanteen.add(owner.getDashboard().getCanteen().getNumberStudentColor(Color.RED));
        if(owner.getDashboard().getCanteen().getNumberStudentColor(Color.RED)==0) lastMyCanteen.add(-1);
        else lastMyCanteen.add(owner.getDashboard().getCanteen().getStudents(Color.RED).get(0).getId());

        myCanteen.add(owner.getDashboard().getCanteen().getNumberStudentColor(Color.YELLOW));
        if(owner.getDashboard().getCanteen().getNumberStudentColor(Color.YELLOW)==0) lastMyCanteen.add(-1);
        else lastMyCanteen.add(owner.getDashboard().getCanteen().getStudents(Color.YELLOW).get(0).getId());

        myCanteen.add(owner.getDashboard().getCanteen().getNumberStudentColor(Color.PINK));
        if(owner.getDashboard().getCanteen().getNumberStudentColor(Color.PINK)==0) lastMyCanteen.add(-1);
        else lastMyCanteen.add(owner.getDashboard().getCanteen().getStudents(Color.PINK).get(0).getId());

        myCanteen.add(owner.getDashboard().getCanteen().getNumberStudentColor(Color.BLUE));
        if(owner.getDashboard().getCanteen().getNumberStudentColor(Color.BLUE)==0) lastMyCanteen.add(-1);
        else lastMyCanteen.add(owner.getDashboard().getCanteen().getStudents(Color.BLUE).get(0).getId());

        myLastCard = owner.getDashboard().getGraveyard();
        List<Card> hand = owner.getHand().getAllCards();
        List<Student> entrance = owner.getDashboard().getEntrance().getAllStudents();
        Map<Color, List<Student>> mapCanteen = new HashMap<>();
        switch (owner.getColor()){
            case BLACK:
                myTowers = b;
                myColor = ColorTower.BLACK;
                break;
            case WHITE:
                myTowers = w;
                myColor = ColorTower.WHITE;
                break;
            case GREY:
                myTowers = g;
                myColor = ColorTower.GREY;
        }

        for(Color c : Color.values()){
            mapCanteen.put(c, owner.getDashboard().getCanteen().getStudents(c));
        }

        if(game.getCompleteRules())
            player = new PlayerReport(owner.getColor(), owner.getDashboard().getGraveyard(), owner.getDashboard().getTowers(), hand, entrance, mapCanteen, owner.getCoins());
        else
            player = new PlayerReport(owner.getColor(), owner.getDashboard().getGraveyard(), owner.getDashboard().getTowers(), hand, entrance, mapCanteen, -1);

        remainingMoves = game.getRemainingMoves();
        currentPhase = game.getCurrentPhase();

        if(game.getCompleteRules()){
            //CHARACTERS
            for(Character c : game.getCharactersCards()){
                characters.add(new CharacterReport(c));
                charId.add(c);
            }

            //ACTIVE CARD
            activeCard = game.getActiveCard();
            if(activeCard != -1){
                try {
                    requestedAction = game.getRequestedAction();
                } catch (NoActiveCardException e) {
                    throw new RuntimeException(e);
                }
            } else{
                requestedAction = null;
            }

            if(requestedAction == Action.EXCHANGESTUDENT){
                this.remainingExchanges = game.getRemainingExchanges();
            } else{
                this.remainingExchanges = -1;
            }

            activeRule = game.getCurrentRule().getClass().getSimpleName();
        } else{
            activeCard = -1;
            requestedAction = null;
            remainingExchanges = -1;
            activeRule = null;
        }
    }

    /**
     * This method return the islands in a String format
     * @return a String containing the island of the game
     */
    public String getIslandsString(){
        StringBuilder s = new StringBuilder();
        int i = 0;
        for(IslandReport ir : islands){
            int color = 31+i;
            s.append("\u001B[").append(color).append("m||\u001B[0m ").append(ir.toString()).append("\n");
            i = (i+1) % 7;
        }
        return s.toString();
    }

    /**
     * This method return the professors in a String format
     * @return a String containing the professors
     */
    public String getProfessorsString(){
        StringBuilder s = new StringBuilder();
        for(Color c : Color.values()){
            s.append(ansi().fgBright(c.ansiColor).a(c + ": " + professors.get(c)).reset().a("    ").toString());
        }
        s.append("\n");

        return s.toString();
    }

    /**
     * This method return the clouds in a String format
     * @return a String containing the clouds
     */
    public String getCloudsString(){
        StringBuilder s = new StringBuilder();
        for(CloudReport cr : clouds){
            s.append(cr.toString()).append("\n");
        }
        return s.toString();
    }

    /**
     * This method return the opponents in a String format
     * @return a String containing the opponents
     */
    public String getOpponentsString(){
        StringBuilder s = new StringBuilder();
        for(OpponentReport or : opponents){
            s.append(or.toString()).append("\n");
        }
        return s.toString();
    }

    /**
     * This method return the short version of the characters in a String format
     * @return a String containing the short version of the characters
     */
    public String getShortCharacters(){
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < characters.size(); i++){
            if(activeCard == i){
                s.append(ansi().fgBrightBlue().a("Card " + i).fgBrightCyan().a(" ACTIVE").reset().toString()).append("\n");
            }else {
                s.append(ansi().fgBrightBlue().a("Card " + i).reset().toString()).append("\n");
            }
            s.append(characters.get(i).shortString()).append("\n\n");
        }
        return s.toString();
    }

    /**
     * This method return the characters in a String format
     * @return a String containing the characters
     */
    public String getCharacters(){
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < characters.size(); i++){
            s.append(ansi().fgBrightBlue().a("Card " + i).reset().toString()).append("\n");
            s.append(characters.get(i).toString()).append("\n\n");
        }
        return s.toString();
    }

    /**
     * This method return the dashboard in a String format
     * @return a String containing the dashboard of the player
     */
    public String getYourDashboardString(){
        return player.toString();
    }

    /**
     * This method return a String representing the turn of the current player
     * @return a String representing the player of which is the current turn
     */
    public String getTurnOf(){
        return turnOf;
    }

    /**
     * This method return the name of the player
     * @return a String containing tha name of the player
     */
    public String getNamePlayer(){
        return namePlayer;
    }

    /**
     * This method return a String containing the current phase
     * @return a String containing the current phase
     */
    public String getCurrentPhaseString(){
        String result = "";
        if(currentPhase == Phase.MIDDLETURN){
            result += currentPhase +" - "+remainingMoves + " Remaining Moves";
        } else{
            result += currentPhase.name();
        }

        if(remainingExchanges != -1 && (currentPhase == Phase.MIDDLETURN || currentPhase == Phase.MOVEMNTURN)){
            result += " - "+remainingExchanges + " Remaining Exchanges";
        }

        return result;
    }

    /**
     * This method return  the current active card
     * @return an int representing the current active card
     */
    public int getActiveCard(){
        return activeCard;
    }

    /**
     * This method return the action requested by the server
     * @return Action requested
     */
    public Action getRequestedAction(){
        return requestedAction;
    }

    /**
     * This method return the active rule in a String format
     * @return a String containing the current active rule
     */
    public String getActiveRule(){
        return activeRule;
    }

    /**
     * This method return the current phase
     * @return a Phase object containing the current phase
     */
    public Phase getCurrentPhase(){
        return currentPhase;
    }

    /**
     * This method return a String containing the error
     * @return a String containing the error
     */
    public String getError(){
        return error;
    }

    /**
     * This method return if the game is finished
     * @return a Boolean reporting if hte game is finished
     */
    public Boolean getFinishedGame(){ return this.finished;}

    /**
     * This method return the winner of the game
     * @return a String containing the winner of the game
     */
    public String getWinner(){ return this.winner;}



    //Getter for GUI:

    /**
     * This method return the phase of the game
     * @return a Phase object representing the current phase of the game
     */
    public Phase getPhase(){
        return currentPhase;
    }

    /**
     * This method return num of players in the game
     * @return an int containing the number of players in the game
     */
    public int getNumPlayers() {
        return numPlayers;
    }

    /**
     * This method return the cards of the player
     * @return an ArrayList of cards containing the cards of the player
     */
    public ArrayList<Card> getMyCards(){
        return myCards;
    }

    /**
     * This method return the nicknames of hte opponents
     * @return an ArrayList containing the nicknames of the opponents
     */
    public ArrayList<String> getOpponentsNick(){
        return allPlayersNick;
    }

    /**
     * This method return the color of the player
     * @return a ColorTower enum representing the color of the player
     */
    public ColorTower getMyColor(){
        return myColor;
    }

    /**
     * This method return the color of the others players
     * @return an ArrayList of the enum ColorTower containing the colors of the others players
     */
    public ArrayList<ColorTower> getAllPlayersColor(){
        return allPlayersColor;
    }

    /**
     * This method return the islands
     * @return a LinkedList containing the islands
     */
    public LinkedList<Island> getAllIslands(){
        return allIslands;
    }

    /**
     * This method return Mother Nature
     * @return an int representing the position of Mother Nature
     */
    public int getMT(){
        return MT;
    }

    /**
     * This method return the students on the clouds
     * @return an ArrayList containing the students on the clouds
     */
    public ArrayList<ArrayList<Student>> getStudentsOnClouds(){
        return studentsOnClouds;
    }

    /**
     * This method return the students in the entrance of the player
     * @return an ArrayList containing the students in the entrance
     */
    public ArrayList<Student> getMyEntrance(){
        return myEntrance;
    }

    /**
     * This method return the students in the entrance of the opponents
     * @return an ArrayList containing an ArrayList for each opponent containing the students in the entrance of the opponents
     */
    public ArrayList<ArrayList<Student>> getOpponentsEntrance(){
        return opponentsEntrance;
    }

    /**
     * This method return the canteen of the player
     * @return an ArrayList of Integer representing the students in the canteen
     */
    public ArrayList<Integer> getMyCanteen(){
        return myCanteen;
    }

    /**
     * This method return the students in the canteen of the opponents
     * @return an ArrayList containing an ArrayList for each opponent containing the students in the canteen of the opponents
     */
    public ArrayList<ArrayList<Integer>> getOpponentsCanteen(){
        return opponentsCanteen;
    }

    /**
     * This method return the last card played by the player
     * @return a Card object representing the last card played by the player
     */
    public Card getMyLastCard(){
        return myLastCard;
    }

    /**
     * This method return the last cards played by the opponents
     * @return an ArrayList of Card representing the cards played by the opponents
     */
    public ArrayList<Card> getOpponentsLastCard(){
        return opponentsLastCard;
    }

    /**
     * This method return the towers of the player
     * @return an int representing the number of towers of the player
     */
    public int getMyTowers(){
        return myTowers;
    }

    /**
     * This method return the towers in the dashboard
     * @return an ArrayList of Integer representing the towers in the dashboard
     */
    public ArrayList<Integer> getTowersInDash(){
        return towersInDash;
    }

    /**
     * This method return the professors and their owners
     * @return a Map containing the professors represented by the Enum Color and their owners represented by a String
     */
    public Map<Color, String> getProfessors(){
        return professors;
    }

    /**
     * This method return the characters of the game
     * @return an ArrayList containing the characters in the game
     */
    public ArrayList<Character> getChar(){
        return charId;
    }

    /**
     * This method return the coins owned by the player
     * @return an int representing the coins owned by the player
     */
    public int getMyCoins(){
        return myCoins;
    }

    /**
     * This method return the coins owned by the opponents
     * @return an ArrayList of Integer containing the coins owned by the opponents
     */
    public ArrayList<Integer> getOpponentsCoins(){
        return opponentsCoins;
    }

    /**
     * This method return the remaining moves of the player
     * @return an int representing the remaining moves of the player
     */
    public int getRemainingMoves(){
        return remainingMoves;
    }

    /**
     * This method return an ArrayList containing the id of five students (one for every Colour) on the
     * Player's Canteen. These id are needed in case the Player exchanges one of his/her Students with one
     * Student on his/her Canteen.
     * @return an ArrayList of integer containing the id of the last Students of every table
     */
    public ArrayList<Integer> getLastMyCanteen(){
        return lastMyCanteen;
    }
}