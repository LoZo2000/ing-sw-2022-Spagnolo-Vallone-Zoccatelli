package it.polimi.ingsw.model;

import it.polimi.ingsw.model.Island;
import it.polimi.ingsw.model.MotherNature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MotherNatureTest {
    private MotherNature motherNature;

    @BeforeEach
    public void init(){
        motherNature = new MotherNature();
    }

    @Test
    public void movementTest(){
        assertNull(motherNature.getPosition());
        Island i0 = new Island(0);
        motherNature.movement(i0);
        assertEquals(i0, motherNature.getPosition());
    }
}