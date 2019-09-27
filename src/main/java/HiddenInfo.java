public class HiddenInfo {
    private static final long GameServerID = 00000000000L; //Your server ID here
    private static final String ClientToken = "Your Discord Client token here",
    introSongParent = "driveUrlToIntroSongs", userInfoParent = "driveUrlToUserInfo";
    private static final String SpreadsheetID = "yourSpreadsheetID";
    private static final int RamSheetID = 0, serverSheetID = 0,userSheetID = 0, reminderSheetID = 0; //replace with your sheet IDs
    public static String getClientToken(){
        return ClientToken;
    }
    public static String getIntroSongParent(){
        return introSongParent;
    }
    public static long getGameServerID(){
        return GameServerID;
    }
    public static String getSpreadsheetID(){return SpreadsheetID;}
    public static int getRamSheetID(){return RamSheetID;}
    public static int getServerSheetID(){return serverSheetID;}
    public static int getUserSheetID(){return userSheetID;}
    public static int getReminderSheetID(){return reminderSheetID;}
}