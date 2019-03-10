import sx.blah.discord.handle.obj.*;

import java.util.ArrayList;

public abstract class Game {
    private ArrayList<RhoUser> players;
    private final IChannel channel;
    private IChannel gameChannel;
    private RhoUser gameOwner;
    private IRole playerRole;
    private final String GAME_NAME;
    private final int MAX_PLAYERS;
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
        RhoMain.sendMessage(channel,gameOwner.getUser().getName() + " has started a " + GAME_NAME + " game\n Click on the invite link below in the next 30 seconds to join!");
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
    public ArrayList<RhoUser> getPlayers(){
        return players;
    }
    public IChannel getChannel(){
        return channel;
    }
    public IChannel getGameChannel(){
        return gameChannel;
    }
    public void deleteGame(){
        for(RhoUser player:players){
            gameChannel.getGuild().kickUser(player.getUser());
        }
        playerRole.delete();
        gameChannel.delete();
    }
}
