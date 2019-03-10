import java.util.ArrayList;

public class Hand extends Deck {
    public Hand(ArrayList<Card> cards){
        super(cards);
    }
    public void addCard(Card card){
        System.out.println(cards.add(card));
    }
}
