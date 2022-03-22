package it.polimi.ingsw.model;

public class Player {
    Hand hand;
    Dashboard dashboard;
    final String nickname;


    public Player(String nickname) {
        this.nickname = nickname;
        this.hand = new Hand();
        this.dashboard = new Dashboard(7);
    }

    public Player(Player player) {
        this.nickname = player.nickname;
        this.dashboard = player.dashboard;
        this.hand = new Hand(); // da creare costruttore che duplica
    }
}
