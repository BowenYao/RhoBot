import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.util.concurrent.RateLimiter;
import sx.blah.discord.handle.obj.IGuild;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class RhoServer {
    private static String[] SERVER_CHARACTERISTICS = new String[]{"ServerID","ServerName","AllowsIntroSong"};
    private static int serverSheetID = HiddenInfo.getServerSheetID();
    private IGuild server;
    private boolean allowsIntroSongs;
    private final RateLimiter introSongLimiter = RateLimiter.create(0.1);
    public RhoServer(IGuild server){
        this.server = server;
        allowsIntroSongs = false;
    }
    public RhoServer(IGuild server, boolean allowsIntroSongs){
        this.server = server;
        this.allowsIntroSongs = allowsIntroSongs;
    }
    public IGuild getServer(){return server;}
    public boolean getAllowsIntroSongs(){return allowsIntroSongs;}
    public static int getServerSheetID(){return serverSheetID;}

    public static String[] getServerCharacteristics() {
        return SERVER_CHARACTERISTICS;
    }
    public List<CellData> getCellData(){
        List<CellData> cellData = new ArrayList<>();
        ExtendedValue userID = new ExtendedValue();
        userID.setStringValue(server.getStringID());
        cellData.add(new CellData().setUserEnteredValue(userID));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(server.getName())));
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setBoolValue(allowsIntroSongs)));
        return cellData;
    }
    public List<List<Object>> toDatabaseData(){
        List<List<Object>> outerList = new ArrayList<>();
        List<Object> innerList = new ArrayList<>();
        innerList.add(server.getStringID());
        innerList.add(server.getName());
        innerList.add(allowsIntroSongs);
        outerList.add(innerList);
        return outerList;
    }
    public void setAllowsIntroSongs(boolean allowsIntroSongs){
        this.allowsIntroSongs = allowsIntroSongs;
    }
    public static RhoServer findRhoServer(List<RhoServer> servers,String serverID){
        for(RhoServer server:servers){
            if(server.getServer().getStringID().equals(serverID)){
                return server;
            }
        }
        return null;
    }
    public static List<RhoServer> toRhoServer(List<IGuild> servers){
        List<RhoServer> rhoServers = new ArrayList<>();
        for(IGuild guild: servers){
            rhoServers.add(new RhoServer(guild));
        }
        return rhoServers;
    }
    public RateLimiter getIntroSongLimiter(){
        return introSongLimiter;
    }
    public int searchServer(){
        return RhoMain.searchDatabaseByKeysForSingleItem(new String[]{"ServerID"},new String[]{server.getStringID()},new String[]{"ServerID"},new int[]{serverSheetID});
    }
    public void updateDatabase()throws IOException, GeneralSecurityException {
        int databaseRow = searchServer();
        if(databaseRow> -1) {
            Sheets service = RhoMain.createSheetsService();
            ValueRange requestBody = new ValueRange();
            requestBody.setMajorDimension("ROWS");
            List<List<Object>> requestContent = toDatabaseData();
            requestBody.setValues(requestContent);
            Sheets.Spreadsheets.Values.Update request = service.spreadsheets().values().update(RhoMain.getSpreadsheetId(), "Server Info!A" + (databaseRow + 1) + ":C" + (databaseRow+1), requestBody);
            request.setValueInputOption("RAW");
            request.execute();
        }
    }
}
