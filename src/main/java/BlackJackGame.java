import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IInvite;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.*;

public class BlackJackGame extends Game{
    private Hand[] hands;
    private RhoUser[] finalPlayers;
    private ArrayList<Hand> inHands;
    private ArrayList<Boolean> votePool;
    private ArrayList<WaitForUserInput> waitThreads = new ArrayList<>();
    private Deck deck;
    private Hand dealHand;
    private static final String GAME_NAME = "blackjack"; //NOTE: ALL CHANNEL NAMES MUST BE LOWERCASE
    private static final int MAX_PLAYERS = 7;
    public BlackJackGame(){
        super();
    }
    BlackJackGame(RhoUser gameOwner,IChannel channel){
        super(gameOwner,channel,GAME_NAME,MAX_PLAYERS);
    }
    IInvite createInvite(){
        IInvite invite = this.getGameChannel().createInvite(30,MAX_PLAYERS,true,true);
        return invite;
    }
    void startGame(){
        finalPlayers = this.getPlayers().toArray(new RhoUser[0]);
        hands = new Hand[this.getPlayers().size()];
        inHands = new  ArrayList<Hand>();
        votePool = new ArrayList<Boolean>(this.getPlayers().size());
        deck = new Deck(false);
        System.out.println(deck.getCards().size());
        for(int x = 0; x < this.getPlayers().size(); x++){
            System.out.println(this.getPlayers().get(x).getUser().getName());
            Hand hand = deck.dealHand(2);
            hands[x] = hand;
            inHands.add(hand);
            votePool.add(null);
        }
        dealHand = deck.dealHand(2);
        BlockingQueue<Runnable> runQueue = new ArrayBlockingQueue<Runnable>(getPlayers().size());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(getPlayers().size()/2, getPlayers().size(),3,TimeUnit.SECONDS,runQueue);
        for(int x = 0; x < getPlayers().size(); x++){
            getPlayers().get(x).addGame(this);
            RhoMain.sendMessage(getGameChannel(),getPlayers().get(x).getUser().mention() + "This is your hand: \n" + hands[x]
            +"\n Would you like to stand or hit?");
            WaitForUserInput waitForUserInput = new WaitForUserInput(new String[]{"HIT","PASS"},getGameChannel(),getPlayers().get(x).getUser());
            executor.submit(waitForUserInput);
            RhoEventHandler.getInputScheduler().scheduleWaitThread(waitForUserInput);
        }
        RhoMain.sendMessage(this.getGameChannel(),"This is the dealer's hand: \n" + dealHand + "\n Waiting on players to make a decision...");
        try{
            executor.awaitTermination(60,TimeUnit.SECONDS);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    void runGame(){
        for(int x = 0; x < getPlayers().size(); x++){
            RhoMain.sendMessage(getGameChannel(),getPlayers().get(x).getUser().mention() + " This is your hand: \n" + inHands.get(x) + "\n Would you like to stand or  hit?");
        }
        RhoMain.sendMessage(getGameChannel(),"This is the dealer's hand: \n" + dealHand + "\n Waiting on players to make a decision...");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        ScheduledFuture<?> waitForVotes = executor.scheduleAtFixedRate(() ->{System.out.println("And I'm ");
            System.out.println("waiting");
            if(voteFinished()){
                System.out.println("waiting on the world to change");
                executor.shutdown();
                continueGame();
            }
        },0,1,TimeUnit.SECONDS);
    }
    private void continueGame(){
        ArrayList<Integer> playerPos = new ArrayList<>(getPlayers().size());
        for(int x = 0; x < getPlayers().size(); x++){
            System.out.println(votePool.get(x));
            if(votePool.get(x)){
                System.out.println(deck.drawCard());
                hands[x].addCard(deck.drawCard());
                RhoMain.sendMessage(getPlayers().get(x).getUser().getOrCreatePMChannel(),"This is your new hand: " + hands[x].toString());
            }else{
                RhoMain.sendMessage(getPlayers().get(x).getUser().getOrCreatePMChannel(), "You finished the game with this hand: " + hands[x] +
                        " It has a value of " + calculateHand(hands[x]));
                playerPos.add(x);
            }
        }
        for(int pos: playerPos){
            getPlayers().remove(pos);
            votePool.remove(pos);
            inHands.remove(pos);
        }
        if(calculateHand(dealHand)< 17){
            System.out.println("HEY THIS IS THE HAND VALUE" + calculateHand(dealHand));
            dealHand.addCard(deck.drawCard());
        }else if(countAces(dealHand)>0 && calculateHand(dealHand)!= 21){
            dealHand.addCard(deck.drawCard());
        }
        if(getPlayers().size()>0) {
            for(int x = 0; x  < votePool.size(); x++){
                votePool.set(x, null);
            }
            runGame();
        }
    }
    private int countAces(Hand hand){
        int numAces = 0;
        for(Card card:hand.getCards()){
            if(card.getValue()== 1)
                numAces++;
        }
        return numAces;
    }
    private int calculateHand(Hand hand){
        ArrayList<Card> cards = hand.getCards();
        int sum = 0;
        for(Card card:cards) {
            if (card.getValue() != 1) {
                if(card.getValue()>=10)
                    sum+= 10;
                else
                    sum += card.getValue();
            }
        }
        int numAces = countAces(hand);
        sum+=numAces*11;
        while(numAces>0 && sum>21 ){
            sum-=10;
            numAces--;
        }
        return sum;
    }
    private boolean voteFinished(){
        for(Boolean voted: votePool){
            if(voted == null){
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean vote(String input,RhoUser player){
        if(input.toUpperCase().equals("HIT")){
            for(int x = 0; x < getPlayers().size(); x++){
                if(getPlayers().get(x).equals(player)){
                    votePool.set(x,true);
                    return true;
                }
            }
        }else if(input.toUpperCase().equals("PASS")){
            for(int x = 0; x < getPlayers().size(); x++){
                if(getPlayers().get(x).equals(player)){
                    votePool.set(x,false);
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public String getGameName(){
        return GAME_NAME;
    }
}
