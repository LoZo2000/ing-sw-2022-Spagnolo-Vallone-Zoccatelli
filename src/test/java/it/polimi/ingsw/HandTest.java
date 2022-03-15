package it.polimi.ingsw;

import it.polimi.ingsw.model.Card;
import it.polimi.ingsw.model.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



import java.util.ArrayList;

public class HandTest {
    Hand hand;



    @BeforeEach
    public void init(){
        hand = new Hand();
    }



    //Checks 'getNumCard' and a correct extraction and if exception is triggered
    @Test
    public void getNumCardTest(){
        for(int i=0; i <10; i++){
            assertEquals(10-i, hand.getNumCards());
            try{
                hand.playCard(0);
            }
            catch(Exception e){
                System.out.println(e);
                assertTrue(false, "False");
            }
        }
        assertEquals(0, hand.getNumCards());
        try{
            hand.playCard(0);
            assertTrue(false, "False");
        }
        catch(Exception e){
            System.out.println(e);
        }
        assertEquals(0, hand.getNumCards());
    }



    //Checks if it returns the right card
    @Test
    @RepeatedTest(50)
    public void testExtraction(){
        int rand = (int)(Math.random()*10);
        assertEquals(10, hand.getNumCards());
        try {
            Card c = hand.getCard(rand);
            Card c2 = hand.playCard(rand);
            assertEquals(c.getInitiative(), c2.getInitiative());
            assertEquals(c.getMovement(), c2.getMovement());
            assertEquals(9, hand.getNumCards());
        }
        catch(Exception e){
            System.out.println(e);
            assertTrue(false, "False");
        }
    }



    //Checks if all cards are created correctly
    @Test
    public void checksCardsCreated(){
        ArrayList<Card> cards = hand.getAllCards();
        assertEquals(1, cards.get(0).getInitiative());
        assertEquals(1, cards.get(0).getMovement());
        assertEquals(2, cards.get(1).getInitiative());
        assertEquals(1, cards.get(1).getMovement());
        assertEquals(3, cards.get(2).getInitiative());
        assertEquals(2, cards.get(2).getMovement());
        assertEquals(4, cards.get(3).getInitiative());
        assertEquals(2, cards.get(3).getMovement());
        assertEquals(5, cards.get(4).getInitiative());
        assertEquals(3, cards.get(4).getMovement());
        assertEquals(6, cards.get(5).getInitiative());
        assertEquals(3, cards.get(5).getMovement());
        assertEquals(7, cards.get(6).getInitiative());
        assertEquals(4, cards.get(6).getMovement());
        assertEquals(8, cards.get(7).getInitiative());
        assertEquals(4, cards.get(7).getMovement());
        assertEquals(9, cards.get(8).getInitiative());
        assertEquals(5, cards.get(8).getMovement());
        assertEquals(10, cards.get(9).getInitiative());
        assertEquals(5, cards.get(9).getMovement());
    }
}
