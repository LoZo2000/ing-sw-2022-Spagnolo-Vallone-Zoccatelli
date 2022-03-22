package it.polimi.ingsw.model;

public class Dashboard implements Cloneable{
    Card graveyard;
    Canteen canteen;
    Entrance entrance;
    int towers;

    //students will be inizialized when the game start not when the players are created
    public Dashboard(int towers) {
        canteen = new Canteen();
        entrance = new Entrance();
        this.towers = towers;
    }

    public Dashboard getDashboard(){
        try {
            return (Dashboard) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Canteen getCanteen(){
        return canteen.getCanteen();
    }

    public Entrance getEntrance(){
        return entrance.getEntrance();
    }

    public void addTower(int t){
        towers += t;
    }

    public void removeTower(int t){
        towers -= t;
    }

    public Card getGraveyard(){
        return graveyard.getCard();
    }

    public void setGraveyard(Card c){
        graveyard = c;
    }
}
