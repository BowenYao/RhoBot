import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;

public class WaitForInputScheduler {
    private ArrayList<WaitForUserInput> waitThreads = new ArrayList<>();
    public void scheduleWaitThread(WaitForUserInput waitThread){
        waitThreads.add(waitThread);
    }
    public boolean checkInput(IMessage message){
        ArrayList<WaitForUserInput> matchingWaitThreads = new ArrayList<>();
        for(WaitForUserInput waitThread: waitThreads) {
            if (waitThread.getUser().equals(message.getAuthor())) {
                if (!(waitThread.getListeningChannel() != null && waitThread.getListeningChannel().equals(message.getChannel()))) {
                    waitThread.setUserInput(message.getContent());
                    return true;
                }
            }
        }
        return false;
    }
}
