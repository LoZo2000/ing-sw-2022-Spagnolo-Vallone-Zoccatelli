package it.polimi.ingsw.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.polimi.ingsw.controller.Action;
import it.polimi.ingsw.controller.Location;
import it.polimi.ingsw.model.characters.*;
import it.polimi.ingsw.model.characters.Character;
import it.polimi.ingsw.model.exceptions.*;
import it.polimi.ingsw.model.rules.DefaultRule;
import it.polimi.ingsw.model.rules.InfluenceRule;
import it.polimi.ingsw.model.rules.Rule;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class Game {
    private MotherNature motherNature;
    private LinkedList <Island> islands = new LinkedList<>();
    private Cloud[] clouds;
    private ArrayList<Player> players = new ArrayList<>();
    private Card[] playedCards;
    private Bag bag;
    private Map<Color, Player> professors;
    private Rule currentRule;
    private final int numPlayers;
    private boolean completeRules;
    private int lastPlayed;

    private int activeCard;
    private Character[] charactersCards;

    private final String JSON_PATH = "characters.json";


    //Create game but no players are added;
    public Game(boolean completeRules, int numPlayers) throws IOException{
        lastPlayed = 0;
        this.numPlayers=numPlayers;
        this.completeRules=completeRules;
        FactoryBag fb = new FactoryBag();
        bag = fb.getBag();
        Bag initBag = fb.getInitBag();
        initIslands(initBag);
        motherNature = new MotherNature();
        motherNature.movement(islands.get(0));
        try{
            initClouds(numPlayers);
        } catch (Exception e ) {
            e.printStackTrace();
        }

        this.currentRule = new DefaultRule();
        this.professors = new HashMap<>();

        for(Color c: Color.values()){
            this.professors.put(c, null);
        }

        if(this.completeRules){
            this.activeCard = -1;
            this.charactersCards = initCardsFromJSON();
            Arrays.stream(charactersCards).map(Character::toString)
                    .forEach(System.out::println);
        } else{
            this.activeCard = -1;
            this.charactersCards = null;
        }
    }

    public int getNumberOfStudentPerColorOnCloud(int i, Color color){
        return clouds[i].getNumberOfStudentPerColor(color);
    }

    public int getNumberOfClouds(){
        return clouds.length;
    }

    private void initClouds(int numPlayers) throws NoMoreStudentsException, TooManyStudentsException, StillStudentException {
        switch(numPlayers){
            case 2:
            case 4:
                clouds= new Cloud[numPlayers];
                for(int i=0; i<numPlayers; i++){
                    clouds[i]= new Cloud(3);
                    clouds[i].refillCloud(bag.getRandomStudent(3));
                }
                break;
            case 3:
                clouds= new Cloud[numPlayers];
                for(int i=0; i<numPlayers; i++){
                    clouds[i]= new Cloud(4);
                    clouds[i].refillCloud(bag.getRandomStudent(4));
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value of Players: " + numPlayers);
        }
    }

    private void initIslands(Bag initBag){
        for(int i=0; i<12; i++){
            islands.add(new Island(i));
            if(i!=0 && i!=6) {
                try {
                    islands.get(i).addStudent(initBag.getRandomStudent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //TODO TEMPORARY METHOD
    public void addPlayer(Player p){
        this.players.add(p);
    }

    //TODO TEMPORARY METHOD
    public Map<Color, Player> getProfessors(){
        return this.professors;
    }

    public void addPlayer(String nickname, ColorTower color){
        int numberOfStudents = this.numPlayers != 3 ? 7 : 9;
        ArrayList<Student> entranceStudents = extractFromBag(numberOfStudents);

        Player newPlayer = new Player(nickname, numPlayers, color, entranceStudents);

        players.add(newPlayer);
    }

    public int getNumPlayers(){
        return this.numPlayers;
    }

    public int getRegisteredNumPlayers(){
        return this.players.size();
    }

    public LinkedList<Island> getAllIslands(){
        return (LinkedList<Island>)islands.clone();
    }

    public Island getMotherNaturePosition(){
        return motherNature.getPosition();
    }

    public Island getIsland(int id){
        for(Island i : islands){
            if(i.getId() == id) return i;
        }
        return null;
    }

    public Player getPlayer(String nickName){
        for(Player p : players){
            if(p.getNickname().equals(nickName)) return p;
        }
        return null;
    }

    public ArrayList<Player> getAllPlayers(){
        return (ArrayList<Player>) players.clone();
    }

    //When parameter enableMovement is true the method has the regular behaviour.
    //If enableMovement is false, it will be checked if there's an active card.
    //If not an Exception will be raised
    public void moveMotherNature(Island island, boolean enableMovement) throws NoActiveCardException{
        if(enableMovement)
            motherNature.movement(island);
        else{
            if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

            ActionCharacter ac = (ActionCharacter) this.charactersCards[this.activeCard];
            if(ac.getType() != Action.ISLAND_INFLUENCE)
                throw new NoActiveCardException("You can't calculate the influence of an island with this power");
        }
        Report report = island.getReport();

        if(!island.getProhibition()) {
            ColorTower higherInfluence = influence(report);

            if (higherInfluence != report.getOwner()) {
                island.conquest(higherInfluence);
                //TODO Necesario passare per Team
                for (Player p : this.players) {
                    if (p.getColor() == report.getOwner()) {
                        try {
                            p.getDashboard().addTowers(report.getTowerNumbers());
                        } catch (WrongNumberOfTowersException e) {
                            e.printStackTrace();
                        }
                    }
                    if (p.getColor() == higherInfluence) {
                        try {
                            p.getDashboard().removeTowers(report.getTowerNumbers());
                        } catch (WrongNumberOfTowersException e) {
                            e.printStackTrace();
                        }
                    }
                }

                int islandNumber = this.islands.indexOf(island);
                int previousPosition = (islandNumber - 1) % this.islands.size();
                if (previousPosition < 0) previousPosition += this.islands.size();

                if (this.islands.get(previousPosition).getOwner() == island.getOwner()) {
                    try {
                        //Only one Token on the merged island
                        if(this.islands.get(previousPosition).getProhibition() && island.getProhibition()){
                            addProhibitionToken(island);
                        }
                        island = mergeIsland(this.islands.get(previousPosition), island);
                    } catch (NoContiguousIslandException e) {
                        e.printStackTrace();
                    }
                }

                islandNumber = this.islands.indexOf(island);
                int successivePosition = (islandNumber + 1) % this.islands.size();

                if (this.islands.get(successivePosition).getOwner() == island.getOwner()) {
                    try {
                        //Only one Token on the merged island
                        if(this.islands.get(successivePosition).getProhibition() && island.getProhibition()){
                            addProhibitionToken(island);
                        }
                        island = mergeIsland(island, this.islands.get(successivePosition));
                    } catch (NoContiguousIslandException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else{
            addProhibitionToken(island);
        }
    }

    public Island mergeIsland(Island i1, Island i2) throws NoContiguousIslandException {
        int indx1=islands.indexOf(i1);
        int indx2=islands.indexOf(i2);
        if(indx1-indx2==1 || indx1-indx2==-1 || (indx1==0 && indx2== islands.size()-1) || (indx1== islands.size()-1 && indx2==0)){
            Island temp= new Island(i1, i2);
            islands.remove(i1);
            islands.remove(i2);
            islands.add(indx1, temp);
            return temp;
        }
        else throw new NoContiguousIslandException("Islands are not Contiguous");
    }

    // this method is temporary because i have to discuss with others about movement
    public void takeStudentsFromIsland(int CloudNumber, String nickName){
        Entrance to = getPlayer(nickName).getDashboard().getEntrance();
        ArrayList<Student> from = clouds[CloudNumber].chooseCloud();
        for (Student s : from){
            to.addStudent(s);
        }
    }

    private ColorTower influence(Report report){
        //TODO Costruzione Mappa Professori da quella con Player

        //Map with the information about the Color of the professor and the Color of the Tower of the player or team who
        //has the professor
        HashMap<Color, ColorTower> profAndPlayer = new HashMap<>();
        for(Color c: Color.values()){
            Player owner = this.professors.get(c);
            if(owner == null)
                profAndPlayer.put(c, null);
            else
                profAndPlayer.put(c, owner.getColor());
        }

        return this.currentRule.calculateInfluence(report, profAndPlayer);
    }

    private void updateProfessors(){
        for(Color c: Color.values()){
            HashMap<String, Integer> counterCanteen = new HashMap<>();
            for(Player p: this.players){
                int numberStudents = p.getDashboard().getCanteen().getNumberStudentColor(c);
                counterCanteen.put(p.getNickname(), numberStudents);
            }
            Player currentOwner = this.professors.get(c);

            String currentOwnerNickname;
            if(currentOwner == null){
                currentOwnerNickname = null;
            } else{
                currentOwnerNickname = currentOwner.getNickname();
            }

            String newOwnerNickname = this.currentRule.updateProfessor(currentOwnerNickname, counterCanteen);

            Player newOwner = this.players.stream()
                    .filter(player -> player.getNickname().equals(newOwnerNickname))
                    .findAny()
                    .orElse(null);

            this.professors.put(c, newOwner);
        }
    }

    //MOVE FUNCTIONS
    //function move written in a view where the parameters are message received by a client (temporary)
    public void moveStudent(int studentId, Movable arrival, Movable departure){
        Student s;
        try{
            s = departure.removeStudent(studentId);
            arrival.addStudent(s);

            this.updateProfessors();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void exchangeStudent(int studentId1, int studentId2, Movable arrival, Movable departure){
        Student s1, s2;
        try{
            s1 = departure.removeStudent(studentId1);
            s2 = arrival.removeStudent(studentId2);

            arrival.addStudent(s1);
            departure.addStudent(s2);

            this.updateProfessors();

            //TODO Reminder: Use this snippet at the end of each method that can be called when there is an active card
            if(this.activeCard != -1){
                this.activeCard = -1;
                this.currentRule = new DefaultRule();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void refillClouds() throws NoMoreStudentsException, TooManyStudentsException, StillStudentException {
        for (int i=0; i<clouds.length; i++){
            if (numPlayers==3) clouds[i].refillCloud(bag.getRandomStudent(4));
            else clouds[i].refillCloud(bag.getRandomStudent(3));
        }
    }

    public void selectCloud(String playerNick, Cloud cloud){
        ArrayList<Student> students = cloud.chooseCloud();
        for(Student s : students) getPlayer(playerNick).getDashboard().getEntrance().addStudent(s);
    }

    public Cloud[] getAllClouds(){
        return clouds;
    }

    //-------------------------------------------------------------------------------------
    //|                                     CHARACTERS                                    |
    //-------------------------------------------------------------------------------------

    public Character[] initCardsFromJSON() throws IOException{
        Reader reader = new FileReader(JSON_PATH);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        JSONCharacter[] jsonCharacters = gson.fromJson(reader, JSONCharacter[].class);
        List<Character> allCharacters = new ArrayList<>();
        for (JSONCharacter jc : jsonCharacters) {
            switch (jc.getTypeCharacter()) {
                case MOVEMENT:
                    ArrayList<Student> s = extractFromBag(jc.getParams().getNumThingOnIt());
                    allCharacters.add(new MovementCharacter(jc.getId(), jc.getTypeCharacter(), jc.getDesc(), jc.getCost(), s, jc.getParams()));
                    break;

                case INFLUENCE:
                    allCharacters.add(new InfluenceCharacter(jc.getId(), jc.getTypeCharacter(), jc.getDesc(), jc.getCost(), jc.getParams()));
                    break;

                case PROFESSOR:
                    allCharacters.add(new ProfessorCharacter(jc.getId(), jc.getTypeCharacter(), jc.getDesc(), jc.getCost()));
                    break;

                case MOTHERNATURE:
                    allCharacters.add(new MotherNatureCharacter(jc.getId(), jc.getTypeCharacter(), jc.getDesc(), jc.getCost(), jc.getParams()));
                    break;

                case ACTION:
                    allCharacters.add(new ActionCharacter(jc.getId(), jc.getTypeCharacter(), jc.getDesc(), jc.getCost(), jc.getParams()));
                    break;
            }
        }

        Character[] selectedCharacter = new Character[3];
        for(int i=0; i<3; i++){
            Random rand = new Random();
            int randomPos = rand.nextInt(allCharacters.size());
            selectedCharacter[i] = allCharacters.get(randomPos);
            allCharacters.remove(randomPos);
        }

        return selectedCharacter;
    }

    //TODO Refill method for cards which need it (Wait for controller) or use a flag in moveStudent (The Card will always be the departure Movable)

    public int getActiveCard(){
        return this.activeCard;
    }

    public Character[] getCharactersCards(){
        return this.charactersCards.clone();
    }

    //TODO Debug Method
    public Class<?> getCurrentRule(){
        return this.currentRule.getClass();
    }

    //TODO Debug Method
    public void setCharactersCards(Character[] characters){
        this.charactersCards = characters.clone();
    }

    public boolean usePower(Player activePlayer, int card) throws NoCharacterSelectedException{
        if(card <= -1 || card > 2){
            throw new NoCharacterSelectedException("No character selected to use its power");
        }

        //TODO Check Monete

        Character c = this.charactersCards[card];
        this.currentRule = c.usePower(activePlayer);

        if(this.currentRule.isActionNeeded()){
            this.activeCard = card;
        }

        return this.currentRule.isActionNeeded();

    }

    public Action getRequestedAction() throws NoActiveCardException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        MovementCharacter c1 = (MovementCharacter) this.charactersCards[activeCard];

        return c1.getType();
    }

    public Set<Location> getAllowedDepartures() throws NoActiveCardException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        MovementCharacter c1 = (MovementCharacter) this.charactersCards[activeCard];

        return c1.getAllowedDepartures();
    }

    public Set<Location> getAllowedArrivals() throws NoActiveCardException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        MovementCharacter c1 = (MovementCharacter) this.charactersCards[activeCard];

        return c1.getAllowedArrivals();
    }

    public void disableIsland(Island i) throws NoActiveCardException, NoMoreTokensException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        ActionCharacter ac = (ActionCharacter) this.charactersCards[this.activeCard];
        if(ac.getType() != Action.BLOCK_ISLAND)
            throw new NoActiveCardException("You can't block an island with this power");

        ac.removeToken();

        i.setProhibition(true);

        if(this.activeCard != -1){
            this.activeCard = -1;
            this.currentRule = new DefaultRule();
        }
    }

    public void disableColor(Player player, Color c) throws NoActiveCardException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        ActionCharacter ac = (ActionCharacter) this.charactersCards[this.activeCard];
        if(ac.getType() != Action.BLOCK_COLOR)
            throw new NoActiveCardException("You can't block a color with this power");

        this.currentRule = new InfluenceRule(player.getColor(), c, 0, false);

    }

    private void addProhibitionToken(Island island){
        ActionCharacter ac;
        for(Character c : this.charactersCards){
            if(c.getTypeCharacter() == CharacterType.ACTION){
                ac = (ActionCharacter) c;
                if(ac.getType() == Action.BLOCK_ISLAND){
                    //TODO This try/catch will become throws
                    try {
                        island.setProhibition(false);
                        ac.addToken();
                    } catch (NoMoreTokensException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void putBackInBag(Color color) throws NoActiveCardException{
        if(this.activeCard == -1) throw new NoActiveCardException("No Active Card");

        ActionCharacter ac = (ActionCharacter) this.charactersCards[this.activeCard];
        if(ac.getType() != Action.PUT_BACK)
            throw new NoActiveCardException("You can't put back students with this power");

        for(Player p : this.players){
            Canteen canteen = p.getDashboard().getCanteen();
            ArrayList<Student> students = canteen.getStudents(color);

            for(int i=0; i<3 && students.size()!=0; i++){
                Student s = students.remove(0);
                try {
                    canteen.removeStudent(s.getId());
                } catch (NoSuchStudentException e) {
                    e.printStackTrace();
                }
                this.bag.putBackStudent(s);
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    //|                                             UTILS                                        |
    //--------------------------------------------------------------------------------------------

    private ArrayList<Student> extractFromBag(int numStudents){
        ArrayList<Student> students = new ArrayList<>();
        for(int i= 0; i<numStudents; i++){
            try {
                Student s = this.bag.getRandomStudent();
                students.add(s);
            } catch (NoMoreStudentsException e) {
                e.printStackTrace();
            }
        }

        return students;
    }

    /*public void showMe(){
        System.out.println("");
        System.out.println("");
        System.out.println("CURRENT BOARD:");
        LinkedList<Island> islands = getAllIslands();
        for(Island i : islands){
            String owner = "nobody";
            if(i.getOwner() != null) owner = String.valueOf(i.getOwner());

            System.out.print("Island "+i+", Owner "+owner+", Students: ");
            ArrayList<Student> students = i.getAllStudents();
            for(Student s : students) System.out.print(s+" ");
            if(getMotherNaturePosition().equals(i)) System.out.print("MN");
            System.out.print("\n");
        }
        Map<Color, Player> professors = getProfessors();
        System.out.print("PROFESSORS: ");
        for(Color co : Color.values()) System.out.print(co+": "+professors.get(co)+", ");
        ArrayList<Player> players = getAllPlayers();
        System.out.print("\nCLOUDS:\n");
        for(int i=0; i<getNumberOfClouds(); i++){
            System.out.print("Cloud "+i+": ");
            for(Color co : Color.values()) System.out.print(co+"="+getNumberOfStudentPerColorOnCloud(i,co)+", ");
            System.out.print("\n");
        }
        for(Player p : players){
            System.out.println("\nDASHBOARD of "+p+":");
            System.out.print("CARDS: ");
            ArrayList<Card> cards = p.getHand().getAllCards();
            for(Card ca : cards) System.out.print(ca+" ");
            System.out.print("\nEntrance: ");
            ArrayList<Student> students = p.getDashboard().getEntrance().getAllStudents();
            for(Student s : students) System.out.print(s+" ");
            System.out.print("\nCanteen: ");
            for(Color col : Color.values()){
                System.out.print(col+": "+p.getDashboard().getCanteen().getNumberStudentColor(col)+" ");
            }
            System.out.println("");
        }

        Character[] characters = this.getCharactersCards();
        System.out.println("\nCARDS: \n");
        for(Character c : characters){
            System.out.println(c.toString());
        }

        System.out.println();
        if(this.getActiveCard() == -1){
            System.out.println("\u001B[31mNo active card\u001B[0m");
        } else{
            System.out.println("\u001B[34mThe active card is "+this.getActiveCard()+"\u001B[0m");
            try {
                System.out.println("\u001B[34mThe nextAction is "+this.getRequestedAction()+"\u001B[0m");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //TODO DEBUG
        System.out.println();
        System.out.println(getCurrentRule().toString());

        System.out.println("\n\n");
    }*/
}