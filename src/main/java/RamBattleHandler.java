public class RamBattleHandler {
    public static void attack(String attackerID,String defenderID){
        Ram[] attackingRams = Ram.getOwnedRams(attackerID);
        if(attackingRams.length == 1){
            //trigger 1v1
        }else if(attackingRams.length == 2){
            //trigger 2v2
        }else if (attackingRams.length == 3){
            //trigger 3v3
        }else{
            System.out.println("WARNING! USER " + attackerID + " OWNS MORE THAN THE MAXIMUM AMOUNT OF RAMS");
        }
    }
}