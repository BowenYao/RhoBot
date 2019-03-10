import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;


public class GameServerEventHandler {
    private static MessageReceivedEvent messageEventHolder;
    private static IRole currentGameRole;
    private static Game currentGame;
    public static void GameServerEvent(GuildEvent e) {
        Class<?> eventClass = e.getClass();
        if (eventClass == MessageReceivedEvent.class) {
            messageReceivedEvent((MessageReceivedEvent) e);
        }else if(eventClass == UserJoinEvent.class){
            userJoinEvent((UserJoinEvent) e);
        }else if(eventClass == ChannelCreateEvent.class){
            channelCreateEvent((ChannelCreateEvent) e);
        }

    }
    private static void messageReceivedEvent(MessageReceivedEvent e){

    }
    private static void userJoinEvent(UserJoinEvent e){
        e.getUser().addRole(currentGame.getPlayerRole());
        currentGame.addPlayer(e.getUser());
        System.out.println(e.getUser() + " joined game server");
    }
    private static void channelCreateEvent(ChannelCreateEvent e){
         e.getChannel().overrideRolePermissions(e.getGuild().getEveryoneRole(),null,e.getGuild().getEveryoneRole().getPermissions());
    }
    public static void setCurrentGame(Game game){
        currentGame = game;
    }
}
