import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.gif.FrameReader;
import com.icafe4j.image.gif.GIFFrame;
import com.icafe4j.image.gif.GIFTweaker;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.FileUtils;
import sx.blah.discord.handle.obj.IUser;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Ram {
    private static final String RAM_ANIMATION_PATH = "C:\\Users\\boboa\\IdeaProjects\\RhoBot\\RamAnimations",DEFAULT_RAM_IDLE_PATH = RAM_ANIMATION_PATH + "\\defaultIdle.gif";
    private static final int DEFAULT_RAM_SKIN_COLOR = -9612799, DEFAULT_RAM_EYE_COLOR = -143027,DEFAULT_HORN_COLOR = 0xFFA54717;
    private static final String[] RAM_CHARACTERISTICS = new String[]{"SkinColor","HornColor","EyeColor","RamName","HornStrength","Speed","Mass","Species","OwnerID"},
    SPECIES_TYPES = new String[]{"default","domestic","longwool"};
    private static final int RamSheetID = HiddenInfo.getRamSheetID();
    private int hornColor,eyeColor,skinColor;
    private double mass,hornStrength,speed;
    private String name,species;
    public Ram(){
        hornColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        eyeColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        skinColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        mass = Math.random()*50 + 90;
        speed = Math.random()*2 + 6;
        hornStrength = Math.random()*900 + 2700;
        species = SPECIES_TYPES[(int)(Math.random()*SPECIES_TYPES.length)];
        //collision duration should range from 0.1 to about 0.4
    }
    public Ram(String name){
        this.name = name;
        hornColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        eyeColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        skinColor = (int) (Math.random()*0xFF000000 + 0xFFFFFFFF);
        mass = Math.random()*50 + 90;
        speed = Math.random()*2 + 6;
        hornStrength = Math.random()*900 + 2700;
        species = SPECIES_TYPES[(int)(Math.random()*SPECIES_TYPES.length)];
    }
    public Ram(int skinColor,int hornColor,int eyeColor, String name, double hornStrength, double speed, double mass,String species){
        this.skinColor = skinColor;
        this.hornColor = hornColor;
        this.eyeColor = eyeColor;
        this.name = name;
        this.hornStrength = hornStrength;
        this.speed = speed;
        this.mass = mass;
        this.species = species;
    }
    public static int getRamSheetID(){
        return RamSheetID;
    }
    private BufferedImage[] splitGIFFrames(File gif){
        return splitGIFFrames(gif,gif.getName());
        }
    private BufferedImage[] splitGIFFrames(File gif, String newFileName){
        ImageWriter writer = com.icafe4j.image.ImageIO.getWriter(ImageType.PNG);
        try {
            FileInputStream fis = new FileInputStream(gif.getPath());
            File tempCache = new File(gif.getParent() + "\\tempCache");
            tempCache.mkdir();
            // GIFTweaker.splitAnimatedGIF(fis,writer, tempCache.getPath() + "\\" + newFileName); Had to rewrite GIFTWeaker lol because they didn't properly close their output streams
            FrameReader reader = new FrameReader();
            ImageType imageType = writer.getImageType();
            GIFFrame frame = reader.getGIFFrameEx(fis);
            int frameCount = 0;
            String newName = tempCache.getPath() + "\\" + newFileName;
            String baseFileName = StringUtils.isNullOrEmpty(newName)?"frame_":newName + "_frame_";
            while(frame != null){
                String outFileName = baseFileName + frameCount++;
                FileOutputStream os = new FileOutputStream(outFileName + "." + imageType.getExtension());
                writer.write(frame.getFrame(),os);
                frame = reader.getGIFFrameEx(fis);
                os.close();
            }
            //END OF GIFTWEAKER REWRITE
            fis.close();
            File[] files = FileUtils.listFilesMatching(tempCache, newFileName + "_frame_[0-9]*.png");
            BufferedImage[] bufferedImages = new BufferedImage[files.length];
            for(int x = 0; x < files.length; x++){
                FileInputStream fis2 = new FileInputStream(files[x]);
                BufferedImage image = ImageIO.read(fis2);
                bufferedImages[Integer.parseInt(files[x].getName().replace(newFileName + "_frame_","").replace(".png",""))] = image;
                fis2.close();
            }
            org.apache.commons.io.FileUtils.deleteDirectory(tempCache);
            return bufferedImages;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    private void combineGIFFrames (File dir, String fileName, BufferedImage[] bufferedImages, int[] delays) {
        try {
            FileOutputStream fous = new FileOutputStream(dir.getPath() + "\\" + fileName + ".gif");
            GIFTweaker.writeAnimatedGIF(bufferedImages,delays,fous);
            fous.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public File getIdleAni() throws IOException{
        File defaultAni = new File(DEFAULT_RAM_IDLE_PATH);
        File idleFrameCache = new File(RAM_ANIMATION_PATH + "\\" + species + "\\idleFrames");
        int numPatterns = (int)(Math.random()*12 + 3);
        ArrayList<BufferedImage> defaultAniFrames = new ArrayList<>();
        int restCount = 0;
        ArrayList<Integer> overallDelays = new ArrayList<>();
        int[][][] idleDelays = getDelays(new File(idleFrameCache+ "\\idleDelays.txt"));
        String[] idlePatterns = getPatterns(new File(idleFrameCache + "\\idlePatterns.txt"));
        boolean[] idleHasBlink = getHasBlink(new File(idleFrameCache + "\\idleHasBlink.txt"));
        boolean[] idleRepeatable = getRepeatable(new File(idleFrameCache + "\\idleRepeatable.txt"));
        for(int x = 0; x < numPatterns; x++) {
            int patternIndex;
            if (restCount < 1) {
                patternIndex = 0;
                restCount++;
            } else {
                patternIndex = (int) (Math.random() * idleDelays.length);
                restCount = 0;
            }
            int repeats = 1;
            if(idleRepeatable[patternIndex])
                repeats = (int)(Math.random()*3 + 1);
            for(int r = 0; r < repeats; r++) {
                BufferedImage[] patternFrames = buildPatternFrames(idleFrameCache, "idle", idlePatterns[patternIndex], idleHasBlink[patternIndex]);
                int[] delays = new int[idleDelays[patternIndex].length];
                for (int i = 0; i < delays.length; i++) {
                    int delayMin = idleDelays[patternIndex][i][0];
                    int delayMax = idleDelays[patternIndex][i][1];
                    delays[i] = (int) (Math.random() * (delayMax - delayMin) + delayMin);
                }
                for (int i = 0; i < patternFrames.length; i++) {
                    defaultAniFrames.add(patternFrames[i]);
                    overallDelays.add(delays[i]);
                }
            }
        }
            File tempCache = new File(RAM_ANIMATION_PATH + "\\tempCache");
            tempCache.mkdir();
            int[] delays = new int[overallDelays.size()];
            for(int x = 0; x< delays.length; x++){
                delays[x] = overallDelays.get(x);
            }
            combineGIFFrames(tempCache,"tempIdleAni",defaultAniFrames.toArray(new BufferedImage[0]),delays);
            return new File(RAM_ANIMATION_PATH + "\\tempCache\\tempIdleAni.gif");
        }
        private int[][][] getDelays(File delayFile){
        try {
            Scanner s = new Scanner(delayFile);
            String delays = s.nextLine();
            List<int[][]> delayList = new ArrayList<>();
            List<int[]> patternDelays = null;
            List<Integer> frameDelays = null;
            int bracketCount = 0;
            String num = "";
            for(int x = 0; x < delays.length(); x++) {
                if (delays.charAt(x) == '{') {
                    bracketCount++;
                    if (bracketCount == 2) {
                        patternDelays = new ArrayList<>();
                    } else if (bracketCount == 3) {
                        frameDelays = new ArrayList<>();
                    }
                } else if (delays.charAt(x) == '}') {
                    bracketCount--;
                    if (bracketCount == 2) {
                        frameDelays.add(Integer.parseInt(num));
                        num = "";
                        int[] frameDelay = new int[frameDelays.size()];
                        for (int n = 0; n < frameDelay.length; n++) {
                            frameDelay[n] = frameDelays.get(n);
                        }
                        patternDelays.add(frameDelay);
                    }else if(bracketCount == 1){
                        delayList.add(patternDelays.toArray(new int[0][]));
                    }

                }else if (delays.charAt(x) == ','){
                    frameDelays.add(Integer.parseInt(num));
                    num = "";
            }else if(bracketCount == 3){
                    num+=delays.charAt(x);
                }
            }
            return delayList.toArray(new int[0][][]);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        return null;
        }
        private String[] getPatterns(File patternFile){
            try{
                Scanner s = new Scanner(patternFile);
                String patterns = s.nextLine();
                List<String> patternList = new ArrayList<>();
                String pattern = "";
                for(int x = 0; x < patterns.length(); x++){
                    if(patterns.charAt(x) == ','){
                        patternList.add(pattern);
                        pattern = "";
                    }else
                        pattern+=patterns.charAt(x);
                }
                patternList.add(pattern);
                return patternList.toArray(new String[0]);
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }
            return null;
        }
        public boolean[] getHasBlink(File blinkFile){
            try{
                Scanner s = new Scanner(blinkFile);
                List<Boolean> hasBlink = new ArrayList<>();
                while(s.hasNextInt()){
                    hasBlink.add(s.nextInt() == 1);
                }
                boolean[] blinks = new boolean[hasBlink.size()];
                for(int x = 0; x < blinks.length; x++){
                    blinks[x] = hasBlink.get(x);
                }
                return blinks;
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }
            return null;
        }
        private boolean[] getRepeatable(File repeatFile){
            try{
                Scanner s = new Scanner(repeatFile);
                List<Boolean> repeatable = new ArrayList<>();
                while(s.hasNextInt()){
                    repeatable.add(s.nextInt() == 1);
                }
                boolean[] repeats = new boolean[repeatable.size()];
                for(int x = 0; x < repeats.length; x++){
                    repeats[x] = repeatable.get(x);
                }
                return repeats;
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }
            return null;
        }
  /*      BufferedImage[] newAniFrames = new BufferedImage[defaultAniFrames.size()];
        for(int i = 0; i < defaultAniFrames.size(); i++){
            BufferedImage frame = defaultAniFrames.get(i);
            BufferedImage newFrame = new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_ARGB);
            newFrame.getGraphics().drawImage(frame,0,0,null);
            newFrame.getGraphics().dispose();
            System.out.println(newFrame.getColorModel().hasAlpha());

            }
            newAniFrames[i] = newFrame;
        }
        File testRam = new File(RAM_ANIMATION_PATH + "\\testRam");
        testRam.mkdir();
        combineGIFFrames(testRam,"testRamIdleAni", newAniFrames,IDLE_DELAYS);
        idleAniFile = new File(testRam + "\\testRamIdleAni.gif");
        return idleAniFile;
   }*/
   public static String[] getRamCharacteristics(){
       return RAM_CHARACTERISTICS;
   }
   public String getName(){return name;}
   public double getSpeed(){return speed;}
   public double getMass(){return mass;}
   public double getHornStrength(){return hornStrength;}
   public int getHornColor(){return hornColor;}
   public int getEyeColor(){return eyeColor;}
   public int getSkinColor(){return skinColor;}
   private BufferedImage[] buildPatternFrames(File patternCache, String patternGroupName, String patternName, boolean hasBlink) throws IOException{
       File[] patternFiles = FileUtils.listFilesMatching(patternCache,patternGroupName + patternName + "\\d*.png");
        BufferedImage[] patternImages = new BufferedImage[patternFiles.length];
        File[] blinkFiles = null;
        if(hasBlink){
            blinkFiles = FileUtils.listFilesMatching(patternCache,patternGroupName + patternName + "Blink" + "\\d*.png");
        }
        for(int i = 0; i < patternFiles.length; i++){
            BufferedImage defaultFrame;
            if(hasBlink && Math.random()*3 + 1 > 3){
                defaultFrame = ImageIO.read(blinkFiles[i]);
                BufferedImage newFrame = recolorFrame(defaultFrame);
                patternImages[Integer.parseInt(blinkFiles[i].getName().replaceAll(patternGroupName + patternName + "Blink","").replaceAll(".png",""))] = newFrame;
            }else{
                defaultFrame = ImageIO.read(patternFiles[i]);
                BufferedImage newFrame = recolorFrame(defaultFrame);
                patternImages[Integer.parseInt(patternFiles[i].getName().replaceAll(patternGroupName + patternName,"").replaceAll(".png",""))] = newFrame;
            }
        }
        return patternImages;
   }
   private BufferedImage recolorFrame(BufferedImage frame){
       BufferedImage newFrame = new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_ARGB);
       newFrame.getGraphics().drawImage(frame,0,0,null);
       newFrame.getGraphics().dispose();
       int w = frame.getWidth();
       int h = frame.getHeight();
       for(int x = 0; x < w; x++){
           for(int y = 0; y < h; y++){
               int defaultRGB = frame.getRGB(x,y);
               if(defaultRGB == DEFAULT_RAM_SKIN_COLOR){
                   newFrame.setRGB(x,y,skinColor);
               }else if (defaultRGB == DEFAULT_RAM_EYE_COLOR){
                   newFrame.setRGB(x,y, eyeColor);
               }else if (defaultRGB == DEFAULT_HORN_COLOR){
                   newFrame.setRGB(x,y,hornColor);
               }
               else if(defaultRGB == Color.red.getRGB()){
                   newFrame.setRGB(x,y,0x00FFFFFF);
               }
           }
    }
    return newFrame;
    }
    public static int searchRam(String ramName, String ownerID){
        String[] stringsToMatch = new String[2];
        stringsToMatch[1] = ownerID;
        stringsToMatch[0] = ramName;
        return RhoMain.searchDatabaseByKeysForSingleItem(new String[]{"RamName","OwnerID"},stringsToMatch,new String[]{"Ram Name", "OwnerID"},new int[]{RamSheetID,RamSheetID});
    }
    public void addToDatabase(IUser owner){
        List<Object> innerList = new ArrayList<>();
        innerList.add(skinColor);
        innerList.add(hornColor);
        innerList.add(eyeColor);
        innerList.add(name);
        innerList.add(hornStrength);
        innerList.add(speed);
        innerList.add(mass);
        innerList.add(species);
        innerList.add(owner.getStringID());
        List<List<Object>> outerList = new ArrayList<>();
        outerList.add(innerList);
        try {
            RhoMain.addToDatabase(outerList, "ROWS", "Ram Info");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.out.println("THERE WAS AN ERROR IN ADDING RAM " + name + " TO THE DATABASE");
        }
    }
    public static Ram getRamFromDatabase(int rowNum) throws IOException, GeneralSecurityException{
       Sheets service = RhoMain.createSheetsService();
       Sheets.Spreadsheets.Values.Get getRequest = service.spreadsheets().values().get(RhoMain.getSpreadsheetId(),"Ram Info!A" + (rowNum + 1) + ":I" + (rowNum+1));
       getRequest.setMajorDimension("ROWS");
       ValueRange response = getRequest.execute();
       List<Object> ramInfo =  response.getValues().get(0);
       int skinColor = Integer.parseInt(ramInfo.get(0).toString());
       int hornColor = Integer.parseInt(ramInfo.get(1).toString());
       int eyeColor = Integer.parseInt(ramInfo.get(2).toString());
       String name = ramInfo.get(3).toString();
       double hornStrength = Double.parseDouble(ramInfo.get(4).toString());
       double speed = Double.parseDouble(ramInfo.get(5).toString());
       double mass = Double.parseDouble(ramInfo.get(6).toString());
       String species = ramInfo.get(7).toString();
       return new Ram(skinColor,hornColor,eyeColor,name,hornStrength,speed,mass,species);
    }
    public static Ram[] getOwnedRams(String ownerID){
       DeveloperMetadataLookup ownerLookup = new DeveloperMetadataLookup();
       ownerLookup.setMetadataKey("OwnerID");
       ownerLookup.setLocationType("COLUMN");
       List<DataFilter> filters = new ArrayList<>();
       DataFilter ownerFilter = new DataFilter();
       ownerFilter.setDeveloperMetadataLookup(ownerLookup);
       filters.add(ownerFilter);
       BatchGetValuesByDataFilterRequest requestBody = new BatchGetValuesByDataFilterRequest();
       requestBody.setMajorDimension("ROWS");
       requestBody.setDataFilters(filters);
       try{
           Sheets service = RhoMain.createSheetsService();
           Sheets.Spreadsheets.Values.BatchGetByDataFilter request = service.spreadsheets().values().batchGetByDataFilter(RhoMain.getSpreadsheetId(),requestBody);
           BatchGetValuesByDataFilterResponse response = request.execute();
           List<List<Object>> owners =  response.getValueRanges().get(0).getValueRange().getValues();
           ArrayList<Ram> rams = new ArrayList<>();
           for(int x = 0; x< owners.size(); x++){
               if(owners.get(x).get(0).toString().equals(ownerID)){
                   rams.add(getRamFromDatabase(x));
               }
           }
           return rams.toArray(new Ram[0]);
       }catch(GeneralSecurityException | IOException e){
           e.printStackTrace();
       }
       return null;
    }
    public static boolean checkOwned(String ramName, String ownerID){
       int ramLocation = searchRam(ramName, ownerID);
       return ramLocation > -1;
    }
}
