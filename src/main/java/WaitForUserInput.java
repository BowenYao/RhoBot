import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class WaitForUserInput extends Thread {
    private String[] input;
    private String userInput;
    private IChannel listeningChannel;
    private IUser user;
    public WaitForUserInput(String input,IChannel listeningChannel, IUser user){
        super();
        this.input = new String[]{input};
        this.listeningChannel = listeningChannel;
        this.user = user;
    }
    public WaitForUserInput(String[] input,IChannel listeningChannel, IUser user){
        super();
        this.input = input;
        this.listeningChannel = listeningChannel;
        this.user = user;
    }
    public void run(){
        while(!matches()) {
            try {
                sleep(1000);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
            }
    }
    private boolean matches(){
        boolean contains = false;
        for(String inp:input){
            if(inp.equals(userInput)){
                contains = true;
            }
        }
        if(!contains)
            return false;
        return true;
    }
    public IUser getUser(){return user;}
    public IChannel getListeningChannel(){return listeningChannel;}
    public void setUserInput(String userInput){
        this.userInput = userInput;
    }
    public String getUserInput(){
        return userInput;
    }
}