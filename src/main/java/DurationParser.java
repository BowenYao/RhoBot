import java.util.InputMismatchException;
import java.util.Scanner;

public class DurationParser {
    //contains static functions to parse from int hours int minutes int seconds to milliseconds and back
    public static long parse(String duration) throws IllegalArgumentException{
        Scanner s = new Scanner(duration.replaceAll("[^0-9]"," "));
        long out = 0;
        try {
            if (duration.toUpperCase().contains("HOUR")) {
                out += s.nextInt() * 3600000;
            }
            if (duration.toUpperCase().contains("MINUTE")) {
                int num = s.nextInt();
                out += num * 60000;
            }
            if (duration.toUpperCase().contains("SECOND")) {
                int num = s.nextInt();
                out += num * 1000;
            }
        }catch(InputMismatchException e){
            e.printStackTrace();
            throw new  IllegalArgumentException("Oops! Looks like your reminder might have been formatted incorrectly.\n Try formatting like so: \n" + Reminder.REMINDER_FORMAT);
        }
        return out;
    }
    public static String parseMillis(long millis){
        String out = "";
        millis = Math.abs(millis);
        if(millis == 0)
            return "0 seconds";
        if(millis>= 3600000) {
            out += millis / 3600000 + " hours ";
            millis -= 3600000 * (millis / 3600000);
        }
        if(millis>=60000){
            out += millis/60000 + " minutes ";
            millis -= 60000*(millis/60000);
        }
        if(millis>=1000){
            out += millis/1000 + " seconds";
            millis -= 1000*(millis/1000);
        }
        if(millis>0)
            out += millis + "  milliseconds";
        return out;
    }
}
