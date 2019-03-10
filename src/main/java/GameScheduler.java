
import sx.blah.discord.handle.obj.IExtendedInvite;
import sx.blah.discord.handle.obj.IInvite;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

import java.awt.*;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class GameScheduler extends Thread{
    private Queue<Game> gameQueue;
    public void run(){
        System.out.println("Game Scheduler Started");
        gameQueue = new ConcurrentLinkedQueue<>();
        while(true) {
            while (!gameQueue.isEmpty()) {
                System.out.println("Queueing Games");
                Game game = gameQueue.poll();
                IRole role = game.getGameChannel().getGuild().createRole();
                role.changeName(game.getGameName() + " player");
                role.changeColor(new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
                GameServerEventHandler.setCurrentGame(game);
                EnumSet<Permissions> permissions = role.getPermissions();
                permissions.add(Permissions.READ_MESSAGES);
                permissions.add(Permissions.READ_MESSAGE_HISTORY);
                permissions.add(Permissions.SEND_MESSAGES);
                // permissions.add(Permissions.VOICE_SPEAK);
                game.setPlayerRole(role);
                game.getGameChannel().overrideRolePermissions(role, permissions, null);
                IInvite invite = game.createInvite();
                RhoMain.sendMessage(game.getChannel(), invite.toString());

                IExtendedInvite extendedInvite = (IExtendedInvite) invite;
                      //  game.getChannel().getExtendedInvites().get(0);
                //while (!extendedInvite.isRevoked()) {
                    try {
                        System.out.println(extendedInvite.getMaxAge() + " Waiting");
                        sleep(extendedInvite.getMaxAge()*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        invite.delete();
                        game.deleteGame();
                  //  }
                }
                if(extendedInvite.getUses() == 0 ){
                    game.deleteGame();
                    invite.delete();
                }else{
                        game.runGame();
                }
            }
            try {
                this.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

                break;
            }
        }
    }
    public void queueGame(Game game){
            gameQueue.add(game);
    }
}
