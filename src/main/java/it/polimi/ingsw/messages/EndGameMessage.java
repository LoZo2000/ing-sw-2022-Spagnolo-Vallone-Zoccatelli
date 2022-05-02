package it.polimi.ingsw.messages;

import it.polimi.ingsw.controller.Action;
import it.polimi.ingsw.controller.Update;
import it.polimi.ingsw.model.Game;

public class EndGameMessage extends Message{

    public EndGameMessage(String sender, Action action) {
        super(sender, action);
    }

    @Override
    public Update execute(Game game) {
        return null;
    }

}
