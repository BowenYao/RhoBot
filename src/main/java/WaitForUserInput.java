import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

import static java.lang.Thread.sleep;

public class WaitForUserInput implements Runnable {
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
        System.out.println("Starting wait for user input");
        while(!matches()) {
            System.out.println("And I'm waiting...");
            try {
                sleep(1000);
            }catch(InterruptedException e){
                e.printStackTrace();
                System.out.println("Wait for input interuppted");
            }
        }
            System.out.println(user.getName() + " input " + userInput);
    }
    private boolean matches(){
        System.out.println(input.length);
        if(userInput==null)
            return false;
        if(userInput.isEmpty())
            return false;
        for(String inp:input){
            System.out.println(userInput + " | " + inp);
            if( inp.trim().toUpperCase().equals(userInput.trim().toUpperCase())){
                System.out.println("True? " + inp + " | " + userInput);
                return true;
            }
        }
        return false;
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
