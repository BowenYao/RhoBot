import sx.blah.discord.handle.obj.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public abstract class Game {
    private ArrayList<IUser> players;
    private final IChannel channel;
    private IChannel gameChannel;
    private RhoUser gameOwner;
    private IRole playerRole;
    private final String GAME_NAME;
    private final int MAX_PLAYERS;
    private IInvite invite;
    abstract boolean vote(String input,  IUser player);
    abstract IInvite createInvite();
    abstract void runGame();
    public Game(){
        this.players = null;
        this.channel = null;
        this.MAX_PLAYERS = 0;
        this.GAME_NAME = "";
    }
    public Game(RhoUser gameOwner,IChannel channel,String gameName,int maxPlayers){
        this.gameOwner = gameOwner;
        this.players = new ArrayList<>();
        this.channel = channel;
        this.GAME_NAME = gameName;
        this.MAX_PLAYERS = maxPlayers;
    }
 //   public Game(ArrayList<RhoUser> players, IChannel channel){
 //       this.players = players;
 //       this.channel = channel;
 //   }
    public String getGameName(){
        return GAME_NAME;
    }
    public void startGame(RhoServer gameServer){
        IGuild gameGuild = gameServer.getServer();
        gameChannel = gameGuild.createChannel(GAME_NAME + "-channel");
        RhoEventHandler.getGameScheduler().queueGame(this);

    }
    public void setPlayerRole(IRole role){
        playerRole = role;
    }
    public int getMAX_PLAYERS(){
        return MAX_PLAYERS;
    }
    public IRole getPlayerRole(){
        return playerRole;
    }
    public ArrayList<IUser> getPlayers(){
        return players;
    }
    public void addPlayer(IUser player){
        players.add(player);
    }
    public IChannel getChannel(){
        return channel;
    }
    public IChannel getGameChannel(){
        return gameChannel;
    }
    public IInvite getInvite(){return invite;}
    public RhoUser getGameOwner(){return gameOwner;}
    public void deleteGame(){
        for(IUser player:players){
            gameChannel.getGuild().kickUser(player);
        }
        playerRole.delete();
        gameChannel.delete();
    }
}
