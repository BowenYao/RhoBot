import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteRangeRequest;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import sx.blah.discord.handle.obj.IUser;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Reminder extends Thread {
    //a thread class which sleeps for a given duration and wakes up to remind the requesting user
    private final IUser user;
    private final long duration, time;
    private String message;
    public static final String REMINDER_FORMAT = "remindme [message] in  (_integer_ hours) (_integer_ minutes) (_integer_ seconds):";
    public static final String[] REMINDER_CHARACTERISTICS = {"UserID","RemindDate","RemindMessage"};
    private static final int reminderSheetID = HiddenInfo.getReminderSheetID();
    public Reminder(IUser user, long duration) throws IllegalArgumentException {
        super();
        this.user = user;
        this.duration = duration;
        time = new Date().getTime() + duration;
        if (duration < 0) {
            throw new IllegalArgumentException("nSorry but I can't remind you in -" + DurationParser.parseMillis(duration) + " because that's already happened");
        }
        this.message = "null";
    }

    public Reminder(IUser user, long duration, String message) throws IllegalArgumentException {
        super();
        this.user = user;
        if (duration < 0) {
            throw new IllegalArgumentException("Sorry but I can't remind you " + message.trim() + " in -" + DurationParser.parseMillis(duration) + " because that's already happened");
        }
        this.duration = duration;
        time = new Date().getTime() + duration;
        if(message.equals(""))
            message = "null";
        this.message = " " + message.trim() + " ";
    }
    public Reminder(IUser user, long duration, long time, String message){
        super();
        this.user = user;
        this.duration = duration;
        this.time = time;
        if(message.equals(""))
            message = "null";
        this.message = " " + message.trim() + " ";
    }
    public void run() {
        try {
            this.sleep(duration);
            if(message.trim().equals("null"))
                message = " ";
            RhoMain.sendMessage(user.getOrCreatePMChannel(), user.mention() + " you asked me to remind you " + message.trim());
            deleteFromDatabase();
            this.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
            RhoMain.sendMessage(user.getOrCreatePMChannel(), user.mention() + " Oops looks like something went wrong. I won't be able to remind you " + message.trim());
        }
    }
    public static int getReminderSheetID(){return reminderSheetID;}

    public boolean addToDatabase(){
        List<List<Object>> outerList = new ArrayList<>();
        List<Object> innerList = new ArrayList<>();
        innerList.add(user.getStringID());
        innerList.add(time);
        innerList.add(message);
        outerList.add(innerList);
        try{
            RhoMain.addToDatabase(outerList,"ROWS","Reminder Info");
            return true;
        }catch(IOException | GeneralSecurityException e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean deleteFromDatabase(){
        int rowLocation = RhoMain.searchDatabaseByKeysForSingleItem(new String[]{"UserID","RemindDate"},new String[]{user.getStringID(),(time + "")},new String[]{"UserID", "Remind Date"},new int[]{1161608740,1161608740});
        if(rowLocation > -1) {
            Request deleteRequest = new Request();
            DeleteRangeRequest deleteRangeRequest = new DeleteRangeRequest();
            deleteRangeRequest.setShiftDimension("ROWS");
            GridRange gridRange = new GridRange();
            gridRange.setSheetId(reminderSheetID);
            gridRange.setStartColumnIndex(0);
            gridRange.setStartRowIndex(rowLocation + 1);
            gridRange.setEndColumnIndex(REMINDER_CHARACTERISTICS.length);
            gridRange.setEndRowIndex(rowLocation + 2);
            deleteRangeRequest.setRange(gridRange);
            deleteRequest.setDeleteRange(deleteRangeRequest);
            List<Request> updateRequests = new ArrayList<>();
            updateRequests.add(deleteRequest);
            BatchUpdateSpreadsheetRequest batchUpdate = new BatchUpdateSpreadsheetRequest();
            batchUpdate.setRequests(updateRequests);
            try {
                Sheets service = RhoMain.createSheetsService();
                Sheets.Spreadsheets.BatchUpdate update = service.spreadsheets().batchUpdate(RhoMain.getSpreadsheetId(), batchUpdate);
                update.execute();
                return true;
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public List<List<Object>> toDatabaseData(){
        List<List<Object>> outerList = new ArrayList<>();
        List<Object> innerList = new ArrayList<>();
        innerList.add(user.getStringID());
        innerList.add(new Date().getTime() + duration);
        innerList.add(message);
        outerList.add(innerList);
        return outerList;
    }
}
