package it.polimi.ingsw.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultRuleTest {

    private DefaultRule rule;
    private Report rep;
    private Map<Color, ColorTower> professors;

    @BeforeEach
    void init() {
        this.rule = new DefaultRule();
        this.professors = new HashMap<>();
    }

    @Test
    void calculateInfluenceBlackWinner() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 1);
        students.put(Color.YELLOW, 0);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 3);

        this.rep = new Report(ColorTower.BLACK, 3, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, null);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertEquals(ColorTower.BLACK, winner);
    }

    @Test
    void calculateInfluenceWhiteWinner() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 4);
        students.put(Color.YELLOW, 0);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 3);

        this.rep = new Report(ColorTower.BLACK, 3, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, null);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertEquals(ColorTower.WHITE, winner);
    }

    @Test
    void calculateInfluenceDrawWinner() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 3);
        students.put(Color.YELLOW, 0);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 3);

        this.rep = new Report(ColorTower.BLACK, 3, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, null);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertEquals(ColorTower.BLACK, winner);
    }

    @Test
    void calculateInfluenceNoTowers() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 1);
        students.put(Color.YELLOW, 6);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 3);

        this.rep = new Report(null, 0, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, ColorTower.GREY);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertEquals(ColorTower.GREY, winner);
    }

    @Test
    void calculateInfluenceDrawNoInfluence() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 1);
        students.put(Color.YELLOW, 2);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 4);

        this.rep = new Report(null, 0, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, ColorTower.GREY);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertNull(winner);
    }

    @Test
    void calculateInfluenceDrawNoChange() {
        HashMap<Color, Integer> students = new HashMap<>();
        students.put(Color.BLUE, 4);
        students.put(Color.RED, 1);
        students.put(Color.YELLOW, 2);
        students.put(Color.PINK, 1);
        students.put(Color.GREEN, 4);

        this.rep = new Report(ColorTower.GREY, 1, students);

        this.professors.put(Color.BLUE, ColorTower.WHITE);
        this.professors.put(Color.PINK, ColorTower.BLACK);
        this.professors.put(Color.RED, ColorTower.WHITE);
        this.professors.put(Color.YELLOW, ColorTower.GREY);
        this.professors.put(Color.GREEN, ColorTower.BLACK);

        ColorTower winner = this.rule.calculateInfluence(this.rep, this.professors);

        assertEquals(ColorTower.GREY, winner);
    }

    @Test
    void updateProfessor2Players(){
        String owner = "player1";
        HashMap<String, Integer> counterPerColor = new HashMap<>();
        counterPerColor.put("player1", 3);
        counterPerColor.put("player2", 6);

        String winner = this.rule.updateProfessor(owner, counterPerColor);

        assertEquals("player2", winner);
    }

    @Test
    void updateProfessor2PlayersDraw(){
        String owner = "player1";
        HashMap<String, Integer> counterPerColor = new HashMap<>();
        counterPerColor.put("player1", 6);
        counterPerColor.put("player2", 6);

        String winner = this.rule.updateProfessor(owner, counterPerColor);

        assertEquals("player1", winner);
    }

    @Test
    void updateProfessor3PlayersDraw(){
        String owner = "player1";
        HashMap<String, Integer> counterPerColor = new HashMap<>();
        counterPerColor.put("player1", 6);
        counterPerColor.put("player2", 6);
        counterPerColor.put("player3", 6);

        String winner = this.rule.updateProfessor(owner, counterPerColor);

        assertEquals("player1", winner);
    }

    @Test
    void updateProfessor3PlayersNoOwner(){
        HashMap<String, Integer> counterPerColor = new HashMap<>();
        counterPerColor.put("player1", 5);
        counterPerColor.put("player2", 2);
        counterPerColor.put("player3", 3);

        String winner = this.rule.updateProfessor(null, counterPerColor);

        assertEquals("player1", winner);
    }

    @Test
    void updateProfessor3PlayersNoOwnerDraw(){
        HashMap<String, Integer> counterPerColor = new HashMap<>();
        counterPerColor.put("player1", 0);
        counterPerColor.put("player2", 0);
        counterPerColor.put("player3", 0);

        String winner = this.rule.updateProfessor(null, counterPerColor);

        assertNull(winner);
    }
}