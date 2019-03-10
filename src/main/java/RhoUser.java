import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.ValueRange;
import sx.blah.discord.handle.obj.IUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class RhoUser {
    private static final String[] RHOUSER_CHARACTERISTICS = new String[]{"UserID","IntroSong"};
    private static final int userSheetID = HiddenInfo.getUserSheetID();
    private final IUser user;
    private final String name;
    private String introSong;
    private ArrayList<Game> games;
    public RhoUser(){
        user = null;
        name = "";
        introSong = "null";
    }
    public RhoUser(IUser user){
        this.user = user;
        this.name = user.getName();
        games = new ArrayList<Game>();
        introSong = "null";
    }
    IUser getUser(){
        return user;
    }
    public ArrayList<Game> getGames(){
        return games;
    }
    public static int getUserSheetID(){return userSheetID;}

    public void addGame(Game game){
        games.add(game);
    }
    public Game removeGame(Game game){
            if(games.remove(game))
                return game;
            return null;
    }
    public Game removeGame(int index){ return games.remove(index);
    }
    public static RhoUser[] toRhoUser(List<IUser> users){
        RhoUser[] out = new RhoUser[users.size()];
        for(int x = 0; x < users.size(); x++){
            out[x] = new RhoUser(users.get(x));
        }
        return out;
    }
    public static RhoUser findRhoUser(RhoUser[] rhoUsers, IUser user){
        for(RhoUser rhoUser: rhoUsers){
            if(user.equals(rhoUser.getUser()))
                return rhoUser;
        }
        return null;
    }
    public static RhoUser findRhoUser(RhoUser[] rhoUsers, String userID){
        for(RhoUser rhoUser: rhoUsers){
            if(rhoUser.getUser().getStringID().equals(userID))
                return rhoUser;
        }
        return null;
    }
    public boolean addToDatabase(){
        List<List<Object>> outerList = new ArrayList<>();
        List<Object> innerList = new ArrayList<>();
        innerList.add(user.getStringID());
        innerList.add(introSong);
        outerList.add(innerList);
        try{
            RhoMain.addToDatabase(outerList,"ROWS","USER INFO");
            return true;
        }catch(IOException| GeneralSecurityException e){
            e.printStackTrace();
        }
        return false;
    }
    public void updateFromDatabase(int databaseRow){
        try {
            Sheets service = RhoMain.createSheetsService();
            Sheets.Spreadsheets.Values.Get getRequest = service.spreadsheets().values().get(RhoMain.getSpreadsheetId(), "User Info!A" + (databaseRow + 1) + ":I" + (databaseRow + 1));
            getRequest.setMajorDimension("ROWS");
            ValueRange response = getRequest.execute();
            List<Object> values = response.getValues().get(0);
            String introID = values.get(1).toString();
            introSong = introID;
        }catch(IOException | GeneralSecurityException  e){
            e.printStackTrace();
        }
    }
    public void updateDatabase()throws IOException, GeneralSecurityException{
        int databaseRow = searchUser();
        System.out.println(databaseRow);
        if(databaseRow> -1) {
            Sheets service = RhoMain.createSheetsService();
            ValueRange requestBody = new ValueRange();
            requestBody.setMajorDimension("ROWS");
            List<List<Object>>  requestContent = toDatabaseData();
            requestBody.setValues(requestContent);
            Sheets.Spreadsheets.Values.Update request = service.spreadsheets().values().update(RhoMain.getSpreadsheetId(), "User Info!A" + (databaseRow + 1) + ":B" + (databaseRow+1), requestBody);
            request.setValueInputOption("RAW");
            request.execute();
        }
    }
    public int searchUser(){
        return RhoMain.searchDatabaseByKeysForSingleItem(new String[]{"UserID"},new String[]{user.getStringID()},new String[]{"UserID"},new int[]{0});
    }
    public static String[] getRhoUserCharacteristics(){
        return RHOUSER_CHARACTERISTICS;
    }
    public String getIntroSong(){
        return introSong;
    }
    public void setIntroSong(String introSong){
        this.introSong = introSong;
    }
    public List<CellData> getCellData(){
        List<CellData> cellData = new ArrayList<>();
        ExtendedValue userID = new ExtendedValue();
        userID.setStringValue(user.getStringID());
        cellData.add(new CellData().setUserEnteredValue(userID));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(introSong)));
        return cellData;
    }
    public List<List<Object>> toDatabaseData(){
        List<List<Object>> outerList = new ArrayList<>();
        List<Object> innerList = new ArrayList<>();
        innerList.add(user.getStringID());
        innerList.add(introSong);
        outerList.add(innerList);
        return outerList;
    }

}
