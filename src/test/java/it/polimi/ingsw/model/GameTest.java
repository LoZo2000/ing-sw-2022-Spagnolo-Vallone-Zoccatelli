package it.polimi.ingsw.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Game game;

    @BeforeEach
    void init(){
        ArrayList<Student> students1 = new ArrayList<>();
        students1.add(new Student(1, Color.RED));
        students1.add(new Student(2, Color.BLUE));
        students1.add(new Student(3, Color.GREEN));
        students1.add(new Student(4, Color.PINK));
        students1.add(new Student(5, Color.BLUE));
        students1.add(new Student(6, Color.BLUE));
        students1.add(new Student(7, Color.BLUE));

        Player player1 = new Player("player1",2, ColorTower.BLACK, students1);

        ArrayList<Student> students2 = new ArrayList<>();
        students2.add(new Student(8, Color.RED));
        students2.add(new Student(9, Color.BLUE));
        students2.add(new Student(10, Color.GREEN));
        students2.add(new Student(11, Color.PINK));
        students2.add(new Student(12, Color.YELLOW));
        students2.add(new Student(13, Color.RED));
        students2.add(new Student(14, Color.BLUE));

        Player player2 = new Player("player2", 2, ColorTower.WHITE, students2);

        this.game = new Game(false, 2);

        this.game.addPlayer(player1);
        this.game.addPlayer(player2);
    }

    @Test
    void moveMotherNature() {
        Entrance e = null;
        Canteen c = null;
        try {
            e = this.game.getPlayer("player1").getDashboard().getEntrance();
            c = this.game.getPlayer("player1").getDashboard().getCanteen();
        }catch(Exception ex){
            fail();
        }
        Island i = null;
        try {
            i = this.game.getIsland(3);
        }catch(Exception ex){
            fail();
        }

        try {
            this.game.moveStudent(6, c, e);
            this.game.moveStudent(2, i, e);
            this.game.moveStudent(7, i, e);
        }catch (Exception ex){
            fail();
        }

        this.game.moveMotherNature(i);

        assertEquals(ColorTower.BLACK, i.getReport().getOwner());
        assertEquals(1, i.getReport().getTowerNumbers());

        try {
            i = this.game.getIsland(4);
        }catch (Exception ex){
            fail();
        }
        try {
            this.game.moveStudent(5, i, e);
        }catch (Exception ex){
            fail();
        }

        this.game.moveMotherNature(i);

        assertEquals(11, this.game.getAllIslands().size());
    }

    @Test
    void updateProfessorsMoveToCanteen(){
        Entrance e = null;
        Canteen c = null;
        try {
            e = this.game.getPlayer("player1").getDashboard().getEntrance();
            c = this.game.getPlayer("player1").getDashboard().getCanteen();
        }catch(Exception ex){
            fail();
        }
        try {
            this.game.moveStudent(2, c, e);
        }catch (Exception ex){
            fail();
        }
        Map<Color, Player> professors = this.game.getProfessors();

        try {
            assertEquals(this.game.getPlayer("player1"), professors.get(Color.BLUE));
        }catch (Exception ex){
            fail();
        }
    }

    @Test
    void updateProfessorsNotMoveToCanteen(){
        Entrance e = null;
        try{
            e = this.game.getPlayer("player1").getDashboard().getEntrance();
        }catch (Exception ex){
            fail();
        }
        Island i = null;
        try{
            i = this.game.getIsland(0);
        }catch(Exception ex){
            fail();
        }
        try {
            this.game.moveStudent(2, i, e);
        }catch (Exception ex){
            fail();
        }
        Map<Color, Player> professors = this.game.getProfessors();

        assertNull(professors.get(Color.BLUE));
    }

    @Test
    void updateProfessorsChangeOwnership(){
        Entrance e = null;
        Canteen c = null;
        try{
            e = this.game.getPlayer("player1").getDashboard().getEntrance();
            c = this.game.getPlayer("player1").getDashboard().getCanteen();
        }catch (Exception ex){
            fail();
        }

        try {
            this.game.moveStudent(2, c, e);
        }catch (Exception ex){
            fail();
        }
        Map<Color, Player> professors = this.game.getProfessors();

        try {
            assertEquals(this.game.getPlayer("player1"), professors.get(Color.BLUE));
        }catch (Exception ex){
            fail();
        }

        try {
            e = this.game.getPlayer("player2").getDashboard().getEntrance();
            c = this.game.getPlayer("player2").getDashboard().getCanteen();
        }catch (Exception ex){
            fail();
        }

        try {
            this.game.moveStudent(9, c, e);
            this.game.moveStudent(14, c, e);
        }catch (Exception ex){
            fail();
        }
        professors = this.game.getProfessors();

        try {
            assertEquals(this.game.getPlayer("player2"), professors.get(Color.BLUE));
        }catch (Exception ex){
            fail();
        }
    }

    @Test
    void getterTest(){
        for(int i=0; i<game.getNumberOfClouds(); i++) assertEquals(3, game.getNumberOfStudentPerColorOnCloud(i, Color.BLUE)+game.getNumberOfStudentPerColorOnCloud(i, Color.YELLOW)+game.getNumberOfStudentPerColorOnCloud(i, Color.RED)+game.getNumberOfStudentPerColorOnCloud(i, Color.GREEN)+game.getNumberOfStudentPerColorOnCloud(i, Color.PINK));
        assertEquals(2, game.getNumPlayers());
        assertEquals(2, game.getRegisteredNumPlayers());
        assertEquals(0, game.getMotherNaturePosition().getId());
        assertEquals(2, game.getAllPlayers().size());
        assertEquals(2, game.getAllClouds().length);
        //Try to get islands or players that don't exist:
        try{
            game.getIsland(13);
            fail();
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            game.getPlayer("brooo");
            fail();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void otherInitTest(){
        try {
            game.selectCloud("player1", game.getAllClouds()[0]);
            game.selectCloud("player2", game.getAllClouds()[1]);
            game.refillClouds();
        }catch (Exception e){
            fail();
        }

        game = new Game(false, 5);
        game = new Game(false, 3);
        for(int i=0; i<20; i++) game.addPlayer("bro1", ColorTower.WHITE);
        try {
            game.mergeIsland(game.getIsland(0), game.getIsland(2));
            fail();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            game.moveStudent(3045, game.getIsland(0), game.getIsland(1));
            fail();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @RepeatedTest(100)
    public void moveMotherNatureTest(){
        try {
            for(int i=1; i<8; i++)
                game.moveStudent(i, game.getPlayer("player1").getDashboard().getCanteen(), game.getPlayer("player1").getDashboard().getEntrance());
            game.moveMotherNature(game.getIsland(1));
            game.moveMotherNature(game.getIsland(2));
            game.moveMotherNature(game.getIsland(5));
            game.moveMotherNature(game.getIsland(4));
            game.moveMotherNature(game.getIsland(7));
            for(int i=8; i<15; i++)
                game.moveStudent(i, game.getPlayer("player2").getDashboard().getCanteen(), game.getPlayer("player2").getDashboard().getEntrance());
            game.moveMotherNature(game.getIsland(1));
            game.moveMotherNature(game.getIsland(7));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}