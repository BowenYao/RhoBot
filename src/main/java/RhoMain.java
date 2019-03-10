import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RhoMain {
    private final static IDiscordClient client = new ClientBuilder().withToken(HiddenInfo.getClientToken()).build();
    private static final String GOOGLE_SHEETS_APPLICATION_NAME = "RhobotDatabase";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FOLDER_PATH = "googleSheetsCredentials";
    private static final String CLIENT_SECRET = "client_secret.json";
    private static final List<String> SCOPES = getScopes();
    private static final String SPREADSHEET_ID = HiddenInfo.getSpreadsheetID();
    public static List<String> getScopes(){
        List<String> scopes = new ArrayList<>();
        scopes.add(SheetsScopes.SPREADSHEETS);
        scopes.add(DriveScopes.DRIVE_FILE);
        return scopes;
    }
    public static IDiscordClient getClient(){
        return client;
    }
    public static String getSpreadsheetId(){
        return SPREADSHEET_ID;
    }
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = RhoMain.class.getResourceAsStream(CLIENT_SECRET);
        File file = new File(CLIENT_SECRET);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER_PATH)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
    public static Sheets createSheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Sheets.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT)).setApplicationName(GOOGLE_SHEETS_APPLICATION_NAME).build();
    }
    public static Drive createDriveService() throws IOException,GeneralSecurityException{
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT)).setApplicationName(GOOGLE_SHEETS_APPLICATION_NAME).build();
    }
    public static IMessage sendMessage(IChannel channel, String message){
        try{
            return channel.sendMessage(message);
        }catch(DiscordException e){
            e.printStackTrace();
            //System.out.println(e.getErrorMessage());
        }
        return null;
    }
    public static IMessage sendMessage(IChannel channel, File file){
        try{
            return channel.sendFile(file);
        }catch(DiscordException |FileNotFoundException e){
            e.printStackTrace();
            System.out.println(file.getPath() + " could not be found");
        }
        return null;
    }
    public static IMessage sendMessage(IChannel channel, Ram ram){
        File idleAni;
        try {
            idleAni = ram.getIdleAni();
            IMessage message =  sendMessage(channel, idleAni);
            try {
                FileUtils.deleteDirectory(idleAni.getParentFile());
            }catch(IOException io){
                System.out.println("There was an error deleting " + idleAni.getParentFile().getPath());
            }
            return message;
        }catch(IOException e){
            sendMessage(channel, "Looks like there was a problem with loading " + ram.getName() + "'s sprite. Please try again later.");
        }
        return null;
    }
    public static IUser findUser(IChannel channel, String name){
        List<IUser> users = channel.getUsersHere();
        for(IUser user: users){
            if(!user.isBot()){
                if(user.getName().equals(name))
                    return user;
                else if(user.getNicknameForGuild(channel.getGuild())!= null && user.getNicknameForGuild(channel.getGuild()).equals(name)){
                    return user;
                }
            }
        }
        return null;
    }
    public static IGuild findGuild(String name){
        for(IGuild guild: client.getGuilds()){
            if(guild.getName().equals(name)){
                return guild;
            }
        }
        return null;
    }
    public static IChannel findChannel(IGuild guild, String name){
        for(IChannel channel: guild.getChannels()){
            if(channel.getName().equals(name)){
                return channel;
            }
        }
        return  null;
    }
    public static String listUsers(IChannel channel){
        List<IUser> users = channel.getUsersHere();
        int longestName = 0;
        for(IUser user: users){
            if(!user.isBot()){
                String name = user.getName();
                if(name.length() > longestName){
                    longestName = name.length();
                }
            }
        }
        String output = "Name\t" +StringUtils.repeat("   ",longestName - 4) + " Nickname(if applicable)";
        for(IUser user: users){
            if(!user.isBot()){
                output += "\n" +user.getName() + "\t";
                String nickname = "N/A";
                if(user.getNicknameForGuild(channel.getGuild())!=null)
                    nickname = user.getNicknameForGuild(channel.getGuild());
                output += StringUtils.repeat("   ",longestName -user.getName().length()) + nickname;
            }
        }
        return output;
    }

    public static void addToDatabase(List<List<Object>> addItems, String majorDimension,String range)throws IOException, GeneralSecurityException {
        Sheets service = createSheetsService();
        ValueRange updateBody = new ValueRange();
        updateBody.setMajorDimension(majorDimension);
        updateBody.setRange(range);
        updateBody.setValues(addItems);
        Sheets.Spreadsheets.Values.Append append = service.spreadsheets().values().append(SPREADSHEET_ID, range,updateBody);
        append.setValueInputOption("RAW");
        append.execute();
        //Should probably do something with the update response
    }
    public static void addDatabaseMetadata(DeveloperMetadataLocation location, String key) throws IOException, GeneralSecurityException {
        List<DeveloperMetadataLocation> locations = new ArrayList<>();
        locations.add(location);
        List<String> keys = new ArrayList<>();
        keys.add(key);
        addDatabaseMetadata(locations,keys);
        //Should probably do something with the update response
    }
    public static void addDatabaseMetadata(List<DeveloperMetadataLocation> locations, List<String> keys) throws IOException, GeneralSecurityException {
        Sheets service = createSheetsService();
        List<Request> requests = new ArrayList<>();
        int count = 0;
        for(DeveloperMetadataLocation location: locations){
            DeveloperMetadata newMetadata = new DeveloperMetadata();
            newMetadata.setLocation(location);
            newMetadata.setVisibility("DOCUMENT");
            newMetadata.setMetadataKey(keys.get(count));
            requests.add(new Request().setCreateDeveloperMetadata(new CreateDeveloperMetadataRequest().setDeveloperMetadata(newMetadata)));
            count++;
        }
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest();
        batchRequest.setRequests(requests);
        Sheets.Spreadsheets.BatchUpdate batch = service.spreadsheets().batchUpdate(SPREADSHEET_ID, batchRequest);
        batch.execute();
    }
    public static void setDevMetadata(){
        String[] ramCharacteristics = Ram.getRamCharacteristics();
        List<DeveloperMetadataLocation> metadataLocations = new ArrayList<>();
        List<String> metadataKeys = new ArrayList<>();
        for(int x = 0; x < ramCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(Ram.getRamSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(ramCharacteristics[x]);
        }
        String[] serverCharacteristics = RhoServer.getServerCharacteristics();
        for(int x = 0; x < serverCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(RhoServer.getServerSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(serverCharacteristics[x]);
        }
        String[] rhoUserCharacteristics = RhoUser.getRhoUserCharacteristics();
        for(int x = 0; x < rhoUserCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(RhoUser.getUserSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(rhoUserCharacteristics[x]);
        }
        for(int x = 0; x < Reminder.REMINDER_CHARACTERISTICS.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(Reminder.getReminderSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(Reminder.REMINDER_CHARACTERISTICS[x]);
        }
        try {
            addDatabaseMetadata(metadataLocations, metadataKeys);
        }catch(IOException | GeneralSecurityException e){
            e.printStackTrace();
        }

    }
    public static void updateDevMetadataKeys(){
        String[] ramCharacteristics = Ram.getRamCharacteristics();
        List<DeveloperMetadataLocation> metadataLocations = new ArrayList<>();
        List<String> metadataKeys = new ArrayList<>();
        for(int x = 0; x < ramCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(Ram.getRamSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(ramCharacteristics[x]);
        }
        String[] serverCharacteristics = RhoServer.getServerCharacteristics();
        for(int x = 0; x < serverCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(RhoServer.getServerSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(serverCharacteristics[x]);
        }
        String[] rhoUserCharacteristics = RhoUser.getRhoUserCharacteristics();
        for(int x = 0; x < rhoUserCharacteristics.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(RhoUser.getUserSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(rhoUserCharacteristics[x]);
        }
        for(int x = 0; x < Reminder.REMINDER_CHARACTERISTICS.length; x++){
            DeveloperMetadataLocation metadataLocation = new DeveloperMetadataLocation();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setDimension("COLUMNS");
            dimensionRange.setSheetId(Reminder.getReminderSheetID());
            dimensionRange.setStartIndex(x);
            dimensionRange.setEndIndex(x+1);
            metadataLocation.setDimensionRange(dimensionRange);
            metadataLocations.add(metadataLocation);
            metadataKeys.add(Reminder.REMINDER_CHARACTERISTICS[x]);
        }
        try {
            Sheets service = createSheetsService();
            List<Request> requests = new ArrayList<>();
            int count = 0;
            for (DeveloperMetadataLocation metadataLocation : metadataLocations) {
                List<DataFilter> dataFilters = new ArrayList<>();
                DataFilter dataFilter = new DataFilter();
                DeveloperMetadataLookup developerMetadataLookup = new DeveloperMetadataLookup();
                developerMetadataLookup.setMetadataLocation(metadataLocation);
                developerMetadataLookup.setLocationMatchingStrategy("EXACT_LOCATION");
                dataFilter.setDeveloperMetadataLookup(developerMetadataLookup);
                dataFilters.add(dataFilter);
                DeveloperMetadata developerMetadata = new DeveloperMetadata();
                developerMetadata.setMetadataKey(metadataKeys.get(count));
                developerMetadata.setLocation(metadataLocation);
                developerMetadata.setVisibility("DOCUMENT");
                requests.add(new Request().setUpdateDeveloperMetadata(new UpdateDeveloperMetadataRequest().setDeveloperMetadata(developerMetadata).setDataFilters(dataFilters).setFields("*")));
                count++;
            }
            BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest();
            batchRequest.setRequests(requests);
            Sheets.Spreadsheets.BatchUpdate batchUpdate = service.spreadsheets().batchUpdate(SPREADSHEET_ID, batchRequest);
            batchUpdate.execute();
        }
        catch(GeneralSecurityException | IOException e){
            e.printStackTrace();
        }
    }
    public static int [] searchDatabaseByKeys(String[] metaDataKeys,String[] stringsToMatch, String[] columnTitles,int[] SheetIDs){
        Date date = new Date();
        List<DataFilter> filters = new ArrayList<>();
        for(int x = 0; x < metaDataKeys.length; x++){
            DataFilter filter = new DataFilter();
            DeveloperMetadataLookup metadataLookup = new DeveloperMetadataLookup();
            metadataLookup.setMetadataKey(metaDataKeys[x]);
            metadataLookup.setLocationType("COLUMN");
            metadataLookup.setLocationMatchingStrategy("INTERSECTING_LOCATION");
            DeveloperMetadataLocation developerMetadataLocation = new DeveloperMetadataLocation();
            developerMetadataLocation.setSheetId(SheetIDs[x]);
            metadataLookup.setMetadataLocation(developerMetadataLocation);
            filter.setDeveloperMetadataLookup(metadataLookup);
            filters.add(filter);
        }
        BatchGetValuesByDataFilterRequest batchRequest = new BatchGetValuesByDataFilterRequest();
        batchRequest.setDataFilters(filters);
        batchRequest.setMajorDimension("ROWS");
        try{
            Sheets service = createSheetsService();
            Sheets.Spreadsheets.Values.BatchGetByDataFilter request = service.spreadsheets().values().batchGetByDataFilter(SPREADSHEET_ID,batchRequest);
            BatchGetValuesByDataFilterResponse response = request.execute();
            List<MatchedValueRange> range = response.getValueRanges();
            List<String> columnHeaders = new ArrayList<>();
            for(MatchedValueRange valueRange: range){
               columnHeaders.add(valueRange.getValueRange().getValues().get(0).get(0).toString());
            }
            List<List<Object>> firstList = range.get(columnHeaders.indexOf(columnTitles[0])).getValueRange().getValues();
            ArrayList<Integer> matches = new ArrayList<>();
            for(int x = 0; x < firstList.size(); x++){
                if(firstList.get(x).get(0).toString().toUpperCase().equals(stringsToMatch[0].toUpperCase())){
                    boolean match = true;
                    for(int y = 1; y < stringsToMatch.length; y++){
                        List<Object> list = range.get(y).getValueRange().getValues().get(columnHeaders.indexOf(columnTitles[y]));
                        System.out.println("Searching FOR RAM: " + stringsToMatch[y] + " | " + columnTitles[y] + " : " +   list.get(0).toString() + " | " + list.size());
                        if(!range.get(columnHeaders.indexOf(columnTitles[y])).getValueRange().getValues().get(x).get(0).toString().toUpperCase().equals(stringsToMatch[y].toUpperCase())){
                            match = false;
                        }
                    }
                    if(match){
                        System.out.println("Searching for database item took " + (new Date().getTime() - date.getTime()) + " milliseconds");
                        matches.add(x);
                    }
                }
            }
            if(matches.isEmpty())
                return null;
            else{
                return matches.stream().mapToInt(Integer::intValue).toArray();
            }
        }catch(GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static int searchDatabaseByKeysForSingleItem(String[] metadataKeys,String[] stringsToMatch,String[] columnTitles,int[]SheetIDs){
        int[] indices = searchDatabaseByKeys(metadataKeys,stringsToMatch,columnTitles,SheetIDs);
        if(indices!=null){
            return indices[0];
        }else
            return -1;
    }
    public static int[] searchDatabaseForMultipleColumns(String[][]stringsToMatch, String[]columnTitles, String[] metadataKeys, int[]sheetIDs, int[][] metadataKeyIndex){
        List<DataFilter> filters = new ArrayList<>();
        for(int x = 0; x < metadataKeys.length; x++){
            DataFilter filter = new DataFilter();
            DeveloperMetadataLookup developerMetadataLookup = new DeveloperMetadataLookup();
            developerMetadataLookup.setLocationType("COLUMN");
            developerMetadataLookup.setMetadataKey(metadataKeys[x]);
            DeveloperMetadataLocation developerMetadataLocation = new DeveloperMetadataLocation();
            developerMetadataLocation.setSheetId(sheetIDs[x]);
            developerMetadataLookup.setLocationMatchingStrategy("INTERSECTING_LOCATION");
            developerMetadataLookup.setMetadataLocation(developerMetadataLocation);
            filter.setDeveloperMetadataLookup(developerMetadataLookup);
            filters.add(filter);
        }
        BatchGetValuesByDataFilterRequest batchRequest = new BatchGetValuesByDataFilterRequest();
        batchRequest.setMajorDimension("ROWS");
        batchRequest.setDataFilters(filters);
        BatchGetValuesByDataFilterResponse response = null;
        try{
            Sheets service = createSheetsService();
            Sheets.Spreadsheets.Values.BatchGetByDataFilter request = service.spreadsheets().values().batchGetByDataFilter(SPREADSHEET_ID,batchRequest);
            response = request.execute();
        }catch(IOException | GeneralSecurityException e){
            e.printStackTrace();
        }
        if(response != null){
            List<MatchedValueRange> ranges = response.getValueRanges();
            int[] indices = new int[stringsToMatch.length];
            List<String> foundColumnHeaders = new ArrayList<>();
            for(MatchedValueRange range: ranges){
                foundColumnHeaders.add(range.getValueRange().getValues().get(0).get(0).toString());
            }
            for(int x = 0; x < stringsToMatch.length; x++){
                List<List<Object>> firstList = ranges.get(foundColumnHeaders.indexOf(columnTitles[metadataKeyIndex[x][0]])).getValueRange().getValues();
                boolean matches = false;
                int y = 0;
                while(!matches && y < firstList.size()){
                    if (firstList.get(y).get(0).toString().toUpperCase().equals(stringsToMatch[x][0].toUpperCase())) {
                       matches = true;
                        for (int z = 1; z < stringsToMatch[x].length; z++) {
                            String matchStringColumnHeader = columnTitles[metadataKeyIndex[x][z]];
                            if(!ranges.get(foundColumnHeaders.indexOf(matchStringColumnHeader)).getValueRange().getValues().get(0).get(z).toString().toUpperCase().equals(stringsToMatch[x][z]))
                                matches = false;
                        }
                    }
                    y++;
                }
                if(matches){
                    indices[x] = y - 1;
                }else
                    indices[x] = -1;
            }
            return indices;
        }
        return null;
    }
    public static int[][] searchDatabaseForMultipleColumnsAndItems(String[][]stringsToMatch, String[]columnTitles, String[] metadataKeys, int[]sheetIDs, int[][] metadataKeyIndex){
        List<DataFilter> filters = new ArrayList<>();
        for(int x = 0; x < metadataKeys.length; x++){
            DataFilter filter = new DataFilter();
            DeveloperMetadataLookup developerMetadataLookup = new DeveloperMetadataLookup();
            developerMetadataLookup.setLocationType("COLUMN");
            developerMetadataLookup.setMetadataKey(metadataKeys[x]);
            DeveloperMetadataLocation developerMetadataLocation = new DeveloperMetadataLocation();
            developerMetadataLocation.setSheetId(sheetIDs[x]);
            developerMetadataLookup.setLocationMatchingStrategy("INTERSECTING_LOCATION");
            developerMetadataLookup.setMetadataLocation(developerMetadataLocation);
            filter.setDeveloperMetadataLookup(developerMetadataLookup);
            filters.add(filter);
        }
        BatchGetValuesByDataFilterRequest batchRequest = new BatchGetValuesByDataFilterRequest();
        batchRequest.setMajorDimension("ROWS");
        batchRequest.setDataFilters(filters);
        BatchGetValuesByDataFilterResponse response = null;
        try{
            Sheets service = createSheetsService();
            Sheets.Spreadsheets.Values.BatchGetByDataFilter request = service.spreadsheets().values().batchGetByDataFilter(SPREADSHEET_ID,batchRequest);
            response = request.execute();
        }catch(IOException | GeneralSecurityException e){
            e.printStackTrace();
        }
        if(response != null){
            List<MatchedValueRange> ranges = response.getValueRanges();
            int[][] indices = new int[stringsToMatch.length][];
            List<String> foundColumnHeaders = new ArrayList<>();
            for(MatchedValueRange range: ranges){
                foundColumnHeaders.add(range.getValueRange().getValues().get(0).get(0).toString());
            }
            for(int x = 0; x < stringsToMatch.length; x++){
                List<List<Object>> firstList = ranges.get(foundColumnHeaders.indexOf(columnTitles[metadataKeyIndex[x][0]])).getValueRange().getValues();
                boolean match = false;
                ArrayList<Integer> matches = new ArrayList<>();
                for(int y = 0; y < firstList.size(); y++){
                    if (firstList.get(y).get(0).toString().toUpperCase().equals(stringsToMatch[x][0].toUpperCase())) {
                        match = true;
                        for (int z = 1; z < stringsToMatch[x].length; z++) {
                            String matchStringColumnHeader = columnTitles[metadataKeyIndex[x][z]];
                            if(!ranges.get(foundColumnHeaders.indexOf(matchStringColumnHeader)).getValueRange().getValues().get(0).get(z).toString().toUpperCase().equals(stringsToMatch[x][z]))
                                match = false;
                        }
                    }
                    if(match) {
                        matches.add(y);
                    }
                }
                if(matches.isEmpty()){
                    indices[x] =  null;
                }else
                    indices[x] = matches.stream().mapToInt(Integer::intValue).toArray();
            }
            return indices;
        }
        return null;
    }
}