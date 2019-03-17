
import sx.blah.discord.handle.obj.IExtendedInvite;
import sx.blah.discord.handle.obj.IInvite;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

import java.awt.*;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.*;


public class GameScheduler extends Thread{
    private Queue<Game> gameQueue;
    private IInvite currentInvite;
    private Game currentGame;
    public void run(){
        System.out.println("Game Scheduler Started");
        gameQueue = new ConcurrentLinkedQueue<>();
        while(true) {
            while (!gameQueue.isEmpty()) {
              //  currentInvite = null;
                System.out.println("Queueing Games");
                currentGame = gameQueue.poll();
                IRole role = currentGame.getGameChannel().getGuild().createRole();
                role.changeName(currentGame.getGameName() + " player");
                role.changeColor(new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
                GameServerEventHandler.setCurrentGame(currentGame);
                EnumSet<Permissions> permissions = role.getPermissions();
                permissions.add(Permissions.READ_MESSAGES);
                permissions.add(Permissions.READ_MESSAGE_HISTORY);
                permissions.add(Permissions.SEND_MESSAGES);
                // permissions.add(Permissions.VOICE_SPEAK);
                currentGame.setPlayerRole(role);
                currentGame.getGameChannel().overrideRolePermissions(role, permissions, null);
                currentInvite = currentGame.createInvite();
                RhoMain.sendMessage(currentGame.getChannel(),currentGame.getGameOwner().getUser().getName() + " has started a " + currentGame.getGameName() + " game\n Click on the invite link below in the next 30 seconds to join!\n"  + currentInvite.toString());
                RhoMain.sendMessage(currentGame.getChannel(),currentInvite.toString());
             //   game.lobbyTime()
                IExtendedInvite extendedInvite = (IExtendedInvite) currentInvite;
                try {
                    System.out.println(extendedInvite.getMaxAge() + " Waiting");
                    sleep(extendedInvite.getMaxAge()*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    currentInvite.delete();
                    currentGame.deleteGame();
                    //  }
                }
                currentGame.runGame();
                      //  game.getChannel().getExtendedInvites().get(0);
                //while (!extendedInvite.isRevoked()) {

               // game.runGame();
            }
            try {
                this.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

                break;
            }
        }
    }
    public void queueGame(Game game)  {
            if(!gameQueue.isEmpty())
                RhoMain.sendMessage(game.getChannel(),"Waiting for game queue to open up. This may take a while");
            gameQueue.add(game);
    }
}
