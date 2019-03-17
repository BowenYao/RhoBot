import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;

public class WaitForInputScheduler {
    private ArrayList<WaitForUserInput> waitThreads = new ArrayList<>();
    public void scheduleWaitThread(WaitForUserInput waitThread){
        waitThreads.add(waitThread);
    }
    public void descheduleWaitThread(WaitForUserInput waitThread){waitThreads.remove(waitThread);}
    public boolean checkInput(IMessage message){
        for(WaitForUserInput waitThread: waitThreads) {
            if (waitThread.getUser().equals(message.getAuthor())) {
                if (waitThread.getListeningChannel() != null && waitThread.getListeningChannel().equals(message.getChannel())) {
                    waitThread.setUserInput(message.getContent());
                    System.out.println("Wait thread consumed input: " + message.getContent());
                    return true;
                }else if(waitThread.getListeningChannel() == null){
                    waitThread.setUserInput(message.getContent());
                    return true;
                }
            }
        }
        return false;
    }
}
