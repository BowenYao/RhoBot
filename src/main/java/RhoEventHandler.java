import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.apache.commons.io.FileUtils;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.activation.MimetypesFileTypeMap;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;

import static java.lang.Thread.sleep;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.mid;

public class RhoEventHandler{
    private static final String COMMAND_TRIGGER = "]]";
    private static final long GAME_SERVER_ID = HiddenInfo.getGameServerID();
    private RhoServer gameServer;
    private static GameScheduler gameScheduler = new GameScheduler();
    private List<RhoServer> rhoServers;
    private IDiscordClient client;
    private RhoUser[] rhoUsers;
    private List<Request> batchRequests = new ArrayList<>();
    private Thread updateThread;
    private static WaitForInputScheduler inputScheduler = new WaitForInputScheduler();


    public int[] batchSearchDatabase(){
        String[][] stringsToMatch = new String[rhoUsers.length + rhoServers.size()][];
        int[][] metadataKeyIndices = new int[rhoUsers.length + rhoServers.size()][];
        for(int x =0; x < rhoUsers.length; x++){
            String[] rhoUserStringsToMatch = new String[1];
            rhoUserStringsToMatch[0] = rhoUsers[x].getUser().getStringID();
            stringsToMatch[x] = rhoUserStringsToMatch;
            metadataKeyIndices[x] = new int[]{0};
        }for(int x = 0; x < rhoServers.size(); x++){
            String[] serverStringsToMatch = new String[1];
            serverStringsToMatch[0] = rhoServers.get(x).getServer().getStringID();
            stringsToMatch[rhoUsers.length + x] = serverStringsToMatch;
            metadataKeyIndices[rhoUsers.length + x] = new int[]{1};
        }
        return RhoMain.searchDatabaseForMultipleColumns(stringsToMatch,new String[]{"UserID","ServerID"},new String[]{"UserID","ServerID"},new int[]{0,1006704219},metadataKeyIndices);
    }
    public void updateDatabase(){
        List<ValueRange> batchUpdateRequests = new ArrayList<>();
        List<Request> batchAddRequests = new ArrayList<>();
        int[] indices = batchSearchDatabase();
        for(int x = 0; x < rhoUsers.length; x++){
            if(indices[x] > -1){
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(rhoUsers[x].toDatabaseData());
                valueRange.setRange("User Info!A" + (indices[x] + 1) + ":B" + (indices[x]+ 1));
                valueRange.setMajorDimension("ROWS");
                batchUpdateRequests.add(valueRange);
            }
            else{
                List<RowData> rows = new ArrayList<>();
                RowData rowData = new RowData();
                rowData.setValues(rhoUsers[x].getCellData());
                rows.add(rowData);
                Request request = new Request().setAppendCells(new AppendCellsRequest().setSheetId(0).setFields("*").setRows(rows));
                batchAddRequests.add(request);
            }
        }
        for(int x = 0; x < rhoServers.size();x++){
            if(indices[x+rhoUsers.length] > -1){
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(rhoServers.get(x).toDatabaseData());
                valueRange.setRange("Server Info!A" + (indices[x + rhoUsers.length]+ 1) + ":C" + (indices[x+rhoUsers.length]+1));
                valueRange.setMajorDimension("ROWS");
                batchUpdateRequests.add(valueRange);
            }else{
                List<RowData> rows = new ArrayList<>();
                RowData rowData = new RowData();
                rowData.setValues(rhoServers.get(x).getCellData());
                rows.add(rowData);
                Request request = new Request().setAppendCells(new AppendCellsRequest().setSheetId(1006704219).setFields("*").setRows(rows));
                batchAddRequests.add(request);
            }
        }
        String[][] reminderStringsToMatch = new String[rhoUsers.length][];
        int [][] metadataKeyIndices = new int[rhoUsers.length][];
        for(int x = 0; x < rhoUsers.length; x++){
            String[] rhoUserID = new String[] {rhoUsers[x].getUser().getStringID()};
            reminderStringsToMatch[x] = rhoUserID;
            metadataKeyIndices[x] = new int[]{0};
        }
        List<String> batchGetRemindRanges = new ArrayList<>();
        int[][] remindIndices = RhoMain.searchDatabaseForMultipleColumnsAndItems(reminderStringsToMatch,new String[]{"UserID"},new String[]{"UserID"},new int[]{1161608740},metadataKeyIndices);
        //IMPORTANT!!! AS THE CODE IS CURRENTLY WRITTEN SEARCHDATABASEFORMULTIPLEITEMS ONLY RETURNS THE FIRST ITEM THAT MATCHES ALL THE CRITERIA AND THIS CODE CALLS FOR ALL
        for(int[] num: remindIndices){
            if(num!=null){
                for(int n: num) {
                    batchGetRemindRanges.add("Reminder Info!A" + (n + 1) + ":C" + (n + 1));
                }
            }
        }
        if(batchGetRemindRanges.size()>0) {
            BatchGetValuesResponse response;
            try {
                Sheets service = RhoMain.createSheetsService();
                Sheets.Spreadsheets.Values.BatchGet batchGet = service.spreadsheets().values().batchGet(RhoMain.getSpreadsheetId());
                batchGet.setMajorDimension("ROWS");
                batchGet.setRanges(batchGetRemindRanges);
                batchGet.setFields("*");
                response = batchGet.execute();
            }catch(IOException|GeneralSecurityException ex){
                ex.printStackTrace();
                return;
            }
            for(int x = 0; x < response.getValueRanges().size(); x++){
                ValueRange valueRange = response.getValueRanges().get(x);
                List<List<Object>> values = valueRange.getValues();
                RhoUser rhoUser = RhoUser.findRhoUser(rhoUsers, values.get(0).get(0).toString());
                for(int y = 0; y < values.size(); y++) {
                    long remindDate = Long.parseLong(values.get(y).get(1).toString());
                    long date = remindDate - new Date().getTime();
                    String message = values.get(y).get(2).toString();
                    if (date < 0) {
                        if (message.trim().equals("null"))
                            message = "";
                        RhoMain.sendMessage(rhoUser.getUser().getOrCreatePMChannel(), rhoUser.getUser().mention() + " Sorry but it looks like I was offline when i was supposed to remind you" + message.trim() + " on " + new Date(remindDate).toString());
                        Request deleteRequest = new Request();
                        DeleteRangeRequest deleteRangeRequest = new DeleteRangeRequest();
                        deleteRangeRequest.setShiftDimension("ROWS");
                        GridRange gridRange = new GridRange();
                        gridRange.setSheetId(1161608740);
                        gridRange.setStartColumnIndex(0);
                        gridRange.setStartRowIndex(y + 1);
                        gridRange.setEndColumnIndex(Reminder.REMINDER_CHARACTERISTICS.length);
                        gridRange.setEndRowIndex(y + 2);
                        deleteRangeRequest.setRange(gridRange);
                        deleteRequest.setDeleteRange(deleteRangeRequest);
                        batchRequests.add(deleteRequest);
                    } else if (date < 60000) {
                        Reminder reminder = new Reminder(rhoUser.getUser(), date, message);
                        reminder.start();
                    }
                }
            }
            if(batchRequests.size()>0) {
                try {
                    Sheets service = RhoMain.createSheetsService();
                    BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
                    batchUpdateRequest.setRequests(batchRequests);
                    Sheets.Spreadsheets.BatchUpdate batchUpdate = service.spreadsheets().batchUpdate(RhoMain.getSpreadsheetId(), batchUpdateRequest);
                    batchUpdate.execute();
                }catch(IOException | GeneralSecurityException e){
                    e.printStackTrace();
                }
            }
        }
        try{
            Sheets service = RhoMain.createSheetsService();
            if(!batchAddRequests.isEmpty()){
                BatchUpdateSpreadsheetRequest batchAddRequest = new BatchUpdateSpreadsheetRequest();
                batchAddRequest.setRequests(batchAddRequests);
                Sheets.Spreadsheets.BatchUpdate update = service.spreadsheets().batchUpdate(RhoMain.getSpreadsheetId(),batchAddRequest);
                update.execute();
            }
            if(!batchUpdateRequests.isEmpty()){
                BatchUpdateValuesRequest batchUpdateRequest = new BatchUpdateValuesRequest();
                batchUpdateRequest.setData(batchUpdateRequests);
                Sheets.Spreadsheets.Values.BatchUpdate batchUpdate = service.spreadsheets().values().batchUpdate(RhoMain.getSpreadsheetId(),batchUpdateRequest);
                batchUpdateRequest.setValueInputOption("RAW");
                batchUpdate.execute();
            }
        }catch(IOException | GeneralSecurityException ex){
            ex.printStackTrace();
        }

    }
    public void initializeRhoBot(){
        int[] actionIndices = batchSearchDatabase();
        String spreadsheetID = RhoMain.getSpreadsheetId();
        List<String> batchGetRanges = new ArrayList<>();
        List<Request> batchAddRequests = new ArrayList<>();
        int userSheetID = 0,numStoredUsers = 0, serverSheetID = 1006704219;
        for(int x = 0; x < rhoUsers.length; x++){
            if(actionIndices[x] > -1){
                batchGetRanges.add("User Info!A" + actionIndices[x] + ":B" + actionIndices[x]);
                numStoredUsers++;
            }else{
                List<RowData> rows = new ArrayList<>();
                RowData rowData = new RowData();
                rowData.setValues(rhoUsers[x].getCellData());
                rows.add(rowData);
                Request request = new Request().setAppendCells(new AppendCellsRequest().setSheetId(userSheetID).setFields("*").setRows(rows));
                batchAddRequests.add(request);
            }
        }
        for(int x = 0; x < rhoServers.size(); x++){
            if(actionIndices[rhoUsers.length + x]> -1){
                batchGetRanges.add("Server Info!A" + actionIndices[rhoUsers.length + x] + ":C" + actionIndices[rhoUsers.length + x]);
            }else{
                List<RowData> rows = new ArrayList<>();
                RowData rowData = new RowData();
                rowData.setValues(rhoServers.get(x).getCellData());
                rows.add(rowData);
                Request request = new Request().setAppendCells(new AppendCellsRequest().setSheetId(serverSheetID).setFields("*").setRows(rows));
                batchAddRequests.add(request);
            }
        }
        BatchGetValuesResponse response = null;
        try {

            Sheets service = RhoMain.createSheetsService();
            if(!batchGetRanges.isEmpty()) {
                Sheets.Spreadsheets.Values.BatchGet batchGet = service.spreadsheets().values().batchGet(spreadsheetID);
                batchGet.setRanges(batchGetRanges);
                batchGet.setMajorDimension("ROWS");
                response = batchGet.execute();
            }
            if(!batchAddRequests.isEmpty()) {
                BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
                requestBody.setRequests(batchAddRequests);
                Sheets.Spreadsheets.BatchUpdate batchUpdate = service.spreadsheets().batchUpdate(spreadsheetID, requestBody);
                batchUpdate.execute();
            }
        }catch(IOException | GeneralSecurityException e ){
            e.printStackTrace();
        }
        if(response != null){
            List<ValueRange> responseValues =  response.getValueRanges();
            for(int x = 0; x < numStoredUsers; x++){
                RhoUser rhoUser = RhoUser.findRhoUser(rhoUsers,responseValues.get(x).getValues().get(0).get(0).toString());
                if(rhoUser!= null) {
                    rhoUser.setIntroSong(responseValues.get(x).getValues().get(0).get(1).toString());
                }
            }
            for(int x = numStoredUsers; x < responseValues.size(); x++){
                RhoServer rhoServer = RhoServer.findRhoServer(rhoServers,responseValues.get(x).getValues().get(0).get(0).toString());
                if(rhoServer!= null) {
                    rhoServer.setAllowsIntroSongs(Boolean.parseBoolean(responseValues.get(x).getValues().get(0).get(2).toString()));
                }
            }
        }
        //RhoMain.setDevMetadata();
        //RhoMain.updateDevMetadataKeys();
    }
    public void logout(){
        updateThread.interrupt();
    }

    public static WaitForInputScheduler getInputScheduler(){
        return inputScheduler;
    }
    //initializes useful variables and the like when bot goes live
    @EventSubscriber
    public void onReady(ReadyEvent e){
        client = RhoMain.getClient();
        IGuild gameGuild = client.getGuildByID(GAME_SERVER_ID);
        gameServer = new RhoServer(gameGuild,false);
        List<IGuild> servers = client.getGuilds();
        servers.remove(gameGuild);
        rhoServers = RhoServer.toRhoServer(servers);
        rhoServers.add(gameServer);
        rhoUsers = RhoUser.toRhoUser(client.getUsers());
        client.changePresence(StatusType.ONLINE, ActivityType.PLAYING,"Type " + COMMAND_TRIGGER + "help for a list of commands.");
        initializeRhoBot();
        updateThread = new Thread(() ->{
            while(true) {
                try {
                    sleep(60000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                updateDatabase();
                System.out.println("Updated database at: " + new Date().toString());
            }
        });
        gameScheduler.start();
        updateThread.start();
        //RhoMain.setDevMetadata();
        //RhoMain.updateDevMetadataKeys();
    }
    public static GameScheduler getGameScheduler(){
        return gameScheduler;
    }
    @EventSubscriber
    public void onDisconnect(DisconnectedEvent e){
        if(e.getReason().equals(DisconnectedEvent.Reason.LOGGED_OUT)){
            client.changePresence(StatusType.OFFLINE);
            updateThread.interrupt();
            try {
                updateThread.join();
            }catch(InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }
    @EventSubscriber
    public void onVoiceChannelJoinEvent(UserVoiceChannelJoinEvent e) {
        RhoUser rhoUser = RhoUser.findRhoUser(rhoUsers, e.getUser());
        RhoServer rhoServer = RhoServer.findRhoServer(rhoServers, e.getGuild().getStringID());
        if (rhoServer.getIntroSongLimiter().tryAcquire()) {
            if (!rhoUser.getIntroSong().equals("null") && rhoServer.getAllowsIntroSongs()) {
                String fileID = rhoUser.getIntroSong();
                OutputStream os = new ByteArrayOutputStream();
                try {
                    Drive service = RhoMain.createDriveService();
                    service.files().get(fileID).executeMediaAndDownloadTo(os);
                } catch (IOException | GeneralSecurityException ex) {
                    ex.printStackTrace();
                }
                try {
                    File tempCache = new File("tempCache");
                    tempCache.mkdir();
                    File introSong = new File(tempCache.getPath() + "\\" + "introSong.mp3");
                    introSong.createNewFile();
                    OutputStream fileOutputStream = new FileOutputStream(introSong.getPath());
                    ((ByteArrayOutputStream) os).writeTo(fileOutputStream);
                    os.close();
                    fileOutputStream.close();
                    e.getVoiceChannel().join();
                    AudioPlayer audioPlayer = AudioPlayer.getAudioPlayerForGuild(e.getGuild());
                    audioPlayer.clear();
                    Thread killThread = new Thread(() -> {
                        Date date = new Date();
                        while (audioPlayer.getCurrentTrack() != null) {
                            try {
                                sleep(1000);
                                System.out.println(new Date().getTime() - date.getTime());
                            } catch (InterruptedException interrupted) {
                                interrupted.printStackTrace();
                            }
                        }
                        audioPlayer.clear();
                        e.getVoiceChannel().leave();
                        try {
                            FileUtils.deleteDirectory(tempCache);
                        } catch (IOException deleteFail) {
                            deleteFail.printStackTrace();
                        }
                        join();
                    });
                    try {
                        audioPlayer.queue(introSong);
                        killThread.start();
                    } catch (UnsupportedAudioFileException audioException) {
                        audioException.printStackTrace();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    //Handles message events
    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent e) {
        if (!e.getAuthor().isBot()) {//Bot check prevents looping and other shenanigans
            if (e.getChannel().isPrivate()) {
                System.out.println("Private Event Triggered");
                //  RhoUser rhoUser = RhoUser.findRhoUser(rhoUsers, e.getAuthor());
                //  ArrayList<Game> gameList = rhoUser.getGames();
                //   boolean gameCommand = false;
                //   for (Game game : gameList) {
                //       if (game.vote(e.getMessage().getContent(), rhoUser)) {
                //           gameCommand = true;
                //          break;
                //       }
                //I decided to change my method of input for now but I'll leave this up in case I want to go back
                inputScheduler.checkInput(e.getMessage());

            } else {
                if (!inputScheduler.checkInput(e.getMessage())) {
                    if (e.getMessage().getContent().startsWith(COMMAND_TRIGGER)) {
                        System.out.println("Command Triggered:");
                        String command = e.getMessage().getContent().replaceFirst(COMMAND_TRIGGER, "").trim();
                        System.out.println(command);
                        //Removes the command trigger from the command message
                        if (command.toUpperCase().startsWith("HELP")) {
                            try {
                                Scanner s = new Scanner(new File("helpText.txt"));
                                String helpMessage = "";
                                while (s.hasNext()) {
                                    helpMessage += s.nextLine() + "\n";
                                }
                                RhoMain.sendMessage(e.getChannel(), helpMessage);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            //Help message. Should probably make it a constant somewhere else at some point
                        }/*else if(command.toUpperCase().startsWith("SAY")){
                String message = command.substring(3).trim();
                //removes say from command
                System.out.println("\tSpeech command from " + e.getAuthor().getName() + " recognized");
                RhoMain.sendMessage(e.getChannel(),message);
                System.out.println("\tSent the following message: " + message + " to the " + e.getChannel().getName() +
                                " channel on the " + e.getGuild().getName() + " server"); //A little bit of self-logging
            } Removed say function because it is redundant and encourages spam*/
                        else if (command.toUpperCase().equals("BLACKJACK")) {
                            Game game = new BlackJackGame(RhoUser.findRhoUser(rhoUsers, e.getAuthor()), e.getChannel());
                            game.startGame(gameServer);
                        } else if (command.matches("^(?i)(remind ??me).*")) {
                            System.out.println("Remind Me Command Triggered");
                            //Remind function has rhobot dm a specific user in after a specified interval of time has passed
                            //Might have to change this later but currently remind me starts a new thread that sleeps for the given duration
                            command = command.replaceFirst("(?i)(remind ??me)", " ");
                            //We using regex now boys
                            if (!command.contains(" in ")) {
                                RhoMain.sendMessage(e.getChannel(), "Oops! Looks like your reminder might have been formatted incorrectly.\n Try formatting like so: \n" + Reminder.REMINDER_FORMAT);
                                //A little bit of garbage protection
                            } else {
                                String message = command.substring(0, command.lastIndexOf(" in "));
                                //gets the message part of the reminder command

                                String duration = command.substring(command.lastIndexOf(" in ") + 3).trim();
                                //gets the duration part of the reminder command
                            /*checking for " in " instead of in prevents the word minutes from screwing things up
                            Also the last index of prevents messages that contain in from messing things up but screw
                            you I ain't protecting against extra garbage after the command*/
                                long durationInMillis;
                                try {
                                    durationInMillis = DurationParser.parse(duration);
                                } catch (IllegalArgumentException re) {
                                    RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + " " + re.getMessage());
                                    return;
                                }
                                Reminder reminder;
                                try {
                                     reminder = new Reminder(e.getAuthor(), durationInMillis, message.trim());
                                }catch(IllegalArgumentException re){
                                    RhoMain.sendMessage(e.getChannel(),e.getMessage().mentionsEveryone() + " " + re.getMessage());
                                    return;
                                }
                                RhoMain.sendMessage(e.getAuthor().getOrCreatePMChannel(), "Hey " + e.getAuthor().mention() + " I'm going to remind you " + message.trim() + " in " + DurationParser.parseMillis(durationInMillis));
                                if (durationInMillis > 60000)
                                    reminder.addToDatabase();
                                else
                                    reminder.start();
                                //starts the reminder thread only if the remind is a minute or less long
                                /* creates a new reminder thread and sends the duration to duration parser which returns a long in milliseconds.
                                Duration should be in format [integer hour(s)] [integer minute(s)] ]integer second(s)]
                                reminder  constructors throw an illegal argument exception if the duration is negative
                                parser throws an illegal argument exception if the duration isn't properly formatted */


                            }
                        } else if (command.matches("^(?i)(download ??ram).*")) {
                            String ramName = command.replaceFirst("^(?i)(download ??ram)", "").trim();
                            if (ramName.isEmpty()) {
                                RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + " Please specify a name for your ram and try again");
                                return;
                            }
                            if (Ram.checkOwned(ramName, e.getAuthor().getStringID())) {
                                RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + " Looks like you already own a ram named " + ramName + "." +
                                        "\n If you want to call " + ramName + " try using the ]]call function." +
                                        "\n Otherwise, please download a ram with a new name");
                            } else {
                                Ram ram = new Ram(ramName);
                                Date date = new Date();
                                RhoMain.sendMessage(e.getChannel(), ram);
                                System.out.println("Loading ram animation took " + (new Date().getTime() - date.getTime()) + " milliseconds");
                                ram.addToDatabase(e.getAuthor());
                                //NOTE TO SELF: I should probably make the exception handling better
                            }

                        } else if (command.matches("^(?i)(call) .*")) {
                            String ramName = command.replaceFirst("^(?i)(call) ", "");
                            int ramLocation = Ram.searchRam(ramName, e.getAuthor().getStringID());
                            if (ramLocation > -1) {
                                try {
                                    RhoMain.sendMessage(e.getChannel(), Ram.getRamFromDatabase(ramLocation));
                                } catch (IOException | GeneralSecurityException ex) {
                                    ex.printStackTrace();
                                    RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + " It looks like there was an error retrieving " + ramName + " from my database.\n Please try again later.");
                                }
                            } else {
                                RhoMain.sendMessage(e.getChannel(), "Sorry " + e.getAuthor().mention() + ". It looks like you don't own a ram by the name of " + ramName.trim());
                            }
                        } else if (command.matches("^(?i)(set ??intro ??song)") && e.getMessage().getAttachments().size() > 0) {
                            List<IMessage.Attachment> attachments = e.getMessage().getAttachments();
                            String fileName = "";
                            File attachment = null;
                            try {
                                URL attachmentUrl = new URL(attachments.get(0).getUrl());
                                File tempCache = new File("tempCache");
                                tempCache.mkdir();
                                attachment = new File(tempCache.getPath() + "\\" + attachments.get(0).getFilename());
                                FileUtils.copyURLToFile(attachmentUrl, attachment);
                                fileName = attachment.getName();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            boolean tooLong = false;
                            try {
                                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(attachment);
                                AudioFormat format = audioInputStream.getFormat();
                                System.out.println(format.toString());
                                int frameSize = format.getFrameSize();
                                if (format.getFrameSize() == -1 && attachment.getName().endsWith(".mp3")) {
                                    frameSize = (int) (144 * ((int) format.properties().get("bitrate")) / format.getSampleRate());
                                    System.out.println(frameSize + " Framesize | " + format.getSampleRate());
                                }
                                float audioLength = attachment.length() / (frameSize * format.getFrameRate());
                                System.out.println(attachment.length() + " | " + format.getFrameRate() + " | " + format.getFrameSize() + " | " + audioLength + " seconds");
                                if (audioLength > 60) {
                                    tooLong = true;
                                    int newLength = (int) (30 * frameSize * format.getFrameRate());
                                    byte[] byteArray = new byte[newLength];
                                    audioInputStream.read(byteArray);
                                    File testFile = new File(attachment.getParent() + "\\tempIntro" + attachment.getName().substring(attachment.getName().lastIndexOf('.')));
                                    FileOutputStream test = new FileOutputStream(testFile);
                                    test.write(byteArray);
                                    test.close();
                                    audioInputStream.close();
                                    attachment = testFile;
                                }
                                audioInputStream.close();
                            } catch (UnsupportedAudioFileException | IOException unsupportedAudioFile) {
                                unsupportedAudioFile.printStackTrace();
                                RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + "Looks like the file you uploaded isn't a supported audio file");
                                return;
                            }
                            javax.activation.MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
                            String mimeType = fileTypeMap.getContentType(attachment);
                            FileContent mediaContent = new FileContent(mimeType, attachment);
                            System.out.println(mediaContent.getType());
                            try {
                                Drive service = RhoMain.createDriveService();
                                List<String> parents = new ArrayList<>();
                                parents.add(HiddenInfo.getIntroSongParent());
                                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                                System.out.println(fileMetadata.getFileExtension());
                                fileMetadata.setName(e.getAuthor().getName() + "'s Intro Song" + fileName.substring(fileName.lastIndexOf('.')));
                                fileMetadata.setParents(parents);
                                com.google.api.services.drive.model.File introSongFile = service.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
                                RhoUser rhoUser = RhoUser.findRhoUser(rhoUsers, e.getAuthor());
                                if (!rhoUser.getIntroSong().equals("null")) {
                                    Drive.Files.Delete deleteExistingIntroSong = service.files().delete(rhoUser.getIntroSong());
                                    deleteExistingIntroSong.execute();
                                }
                                rhoUser.setIntroSong(introSongFile.getId());
                                System.out.println(rhoUser.getIntroSong() + " | " + rhoUser.getUser().getName());
                                rhoUser.updateDatabase();
                                String message = e.getAuthor().mention() + " your intro song was succesfully set and stored!";
                                if (tooLong)
                                    message += "But... it was longer than one minute so I trimmed it to thirty seconds for you";
                                RhoMain.sendMessage(e.getChannel(), message);
                            } catch (IOException | GeneralSecurityException ex) {
                                ex.printStackTrace();
                            }
                            try {
                                FileUtils.deleteDirectory(attachment.getParentFile());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if (command.matches("^(?i)(toggle ??intro ??song)")) {
                            if (e.getAuthor().getPermissionsForGuild(e.getGuild()).contains(Permissions.ADMINISTRATOR)) {
                                RhoServer rhoServer = RhoServer.findRhoServer(rhoServers, e.getGuild().getStringID());
                                boolean allowsIntroSongs = rhoServer.getAllowsIntroSongs();
                                rhoServer.setAllowsIntroSongs(!allowsIntroSongs);
                                try {
                                    rhoServer.updateDatabase();
                                } catch (GeneralSecurityException | IOException ex) {
                                    ex.printStackTrace();
                                    return;
                                }
                                RhoMain.sendMessage(e.getChannel(), e.getAuthor().mention() + " Intro songs successfully toggled to " + rhoServer.getAllowsIntroSongs());
                            }
                        } else if (command.startsWith("test")) {
                            //if(e.getAuthor().getPermissionsForGuild(e.getGuild()).contains(Permissions.ADMINISTRATOR)){
                                //MessageHistory history = e.getChannel().getMessageHistory();
                                //e.getChannel().bulkDelete();
                                //history.bulkDelete();
                            //}
                        }
                        //MessageReceivedEvent messageReceivedEvent = new MessageReceivedEvent(e.getMessage());

                    }
                }
            }
        }
    }

        @EventSubscriber
        public void onAction (GuildEvent e){
            System.out.println("GuildAction Occured");
            if (RhoMain.getClient().isReady()) {
                if(gameServer!=null) {
                    if (e.getGuild().equals(gameServer.getServer())) {
                        GameServerEventHandler.GameServerEvent(e);

                    }
                }
            }
        }
    }
