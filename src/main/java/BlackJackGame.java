import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IInvite;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.*;

public class BlackJackGame extends Game{
    private Hand[] hands;
    private ArrayList<Hand> inHands;
    private boolean[] votePool;
    private ArrayList<WaitForUserInput> waitThreads = new ArrayList<>();
    private Deck deck;
    private Hand dealHand;
    private static final String GAME_NAME = "blackjack"; //NOTE: ALL CHANNEL NAMES MUST BE LOWERCASE
   // private static final int MAX_PLAYERS = 7;
    private int numFinished = 0;
    public BlackJackGame(){
        super();
    }
    BlackJackGame(RhoUser gameOwner,IChannel channel){
        super(gameOwner,channel,GAME_NAME,7);
    }
    IInvite createInvite(){
        IInvite invite = this.getGameChannel().createInvite(30,getMAX_PLAYERS(),true,true);
        return invite;
    }
    void startGame(){

    }
    void runGame(){
        //Initial setup]
       // System.out.println("Starting blackjack game with " + getPlayers().size() + " Players");
        hands = new Hand[getPlayers().size()];
        inHands = new  ArrayList<Hand>();
        votePool = new boolean[getPlayers().size()];
        deck = new Deck(false);
        System.out.println(deck.getCards().size());
        for(int x = 0; x < getPlayers().size(); x++){
            System.out.println(getPlayers().get(x).getName());
            Hand hand = deck.dealHand(2);
            hands[x] = hand;
            inHands.add(hand);
            votePool[x]= true;
        }
        dealHand = deck.dealHand(2);
        //End inital setup
        while(numFinished < votePool.length) {
            BlockingQueue<Runnable> runQueue = new ArrayBlockingQueue<>(getPlayers().size());
            ThreadPoolExecutor executor = new ThreadPoolExecutor(getPlayers().size(), getPlayers().size()*2,3,TimeUnit.SECONDS,runQueue);
            String dealMessage = "";
            for (int x = 0; x < getPlayers().size(); x++) {
                // getPlayers().get(x).addGame(this);
                IUser player = getPlayers().get(x);
                dealMessage += player.mention() + "This is your hand: \n" + hands[x] + "\n";
                if (votePool[x] == true) {
                    dealMessage += " Would you like to stand or hit?";
                    WaitForUserInput waitForUserInput = new WaitForUserInput(new String[]{"HIT", "STAND"}, getGameChannel(), getPlayers().get(x));
                    executor.execute(waitForUserInput);
                    waitThreads.add(waitForUserInput);
                    RhoEventHandler.getInputScheduler().scheduleWaitThread(waitForUserInput);
                }
            }
            dealMessage += "This is the dealer's hand: \n" + dealHand.getCards().get(0) + new String(new char[dealHand.getCards().size()-1]).replace("\0"," | ? |") + "\n Waiting on players to make a decision...";
            RhoMain.sendMessage(getGameChannel(), dealMessage);
            //System.out.println(executor.getTaskCount() + " | " + executor.getCompletedTaskCount()+ "HUHHH") ;
            try {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Blackjack wait for input timed out");
                deleteGame();
                //Add error stuffs later
            }
            for (WaitForUserInput waitThread : waitThreads) {
                String input = waitThread.getUserInput();
                vote(waitThread.getUserInput(), waitThread.getUser());
                RhoEventHandler.getInputScheduler().descheduleWaitThread(waitThread);
            }
            String hitMessage = "";
            for(int x = 0; x < votePool.length; x++){
                IUser user = getPlayers().get(x);
                if(votePool[x]){
                    hands[x].addCard(deck.drawCard());
                   // hitMessage+= user.mention() + " This is your new hand: \n" + hands[x] + "\n";
                    int value = calculateHand(hands[x]);
                    if(value> 21){
                        votePool[x] = false;
                        System.out.println("hmm");
                        numFinished++;
                    }
                }
            }
            int dealValue = calculateHand(dealHand);
            if(dealValue<17){
                dealHand.addCard(deck.drawCard());
            }
            if(!hitMessage.isEmpty())
                RhoMain.sendMessage(getGameChannel(),hitMessage);
        }
        //Initiate endgame
        String endMessage ="This is the dealer's hand: \n" + dealHand;
        boolean dealBust = false;
        int dealValue = calculateHand(dealHand);
        if(dealValue>21){
            dealBust = true;
            endMessage +="\n Looks like the dealer busted :/\n";
        }else{
            endMessage+="\n It is worth " + dealValue + "\n";
        }
        for(int x = 0; x < getPlayers().size(); x++){
            IUser user = getPlayers().get(x);
            int value = calculateHand(hands[x]);
            int dealerValue = calculateHand(dealHand);
            endMessage+= user.mention() + " Your final hand is: \n" + hands[x];
            if(value>21){
                endMessage+= "\n Awww that's too bad looks like you busted\n";
            }else if(value>dealValue){
                endMessage += "\n Woah! Looks like you won!\n";
            }else if (value==dealerValue){
                endMessage += "\n You tied with the dealer. It's a draw.\n";
            }else if(dealerValue<=21){
                endMessage +="\n Darn looks like you lost...\n";
            }
        }
        RhoMain.sendMessage(getGameChannel(),endMessage);
        try {
            System.out.println("Waiting a bit before deleting game");
            Thread.sleep(60000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        deleteGame();
    }
 /*  private void continueGame(){
        ArrayList<Integer> playerPos = new ArrayList<>(getPlayers().size());
        for(int x = 0; x < getPlayers().size(); x++){
            System.out.println(votePool.get(x));
            if(votePool.get(x)){
                System.out.println(deck.drawCard());
                hands[x].addCard(deck.drawCard());
                RhoMain.sendMessage(getPlayers().get(x).getOrCreatePMChannel(),"This is your new hand: " + hands[x].toString());
            }else{
                RhoMain.sendMessage(getPlayers().get(x).getOrCreatePMChannel(), "You finished the game with this hand: " + hands[x] +
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
    */
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
    public boolean vote(String input,IUser player){
        if(input.toUpperCase().equals("HIT")){
            for(int x = 0; x < getPlayers().size(); x++){
                if(getPlayers().get(x).equals(player)){
                    votePool[x] =true;
                    return true;
                }
            }
        }else if(input.toUpperCase().equals("STAND")){
            for(int x = 0; x < getPlayers().size(); x++){
                if(getPlayers().get(x).equals(player)){
                    votePool[x]=false;
                    numFinished++;
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
