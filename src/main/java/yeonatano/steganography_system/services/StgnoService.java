package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.StgnoRepository;

/**
 * מחלקת השירות המרכזית (Facade) המנהלת את כלל פעולות הסטגנוגרפיה במערכת.
 * המחלקה מקבלת בקשות להטמעה או חילוץ, בודקת את סוג הקובץ, 
 * ומנתבת את העבודה לאלגוריתם המתאים (F5, PVD, או DSSS).
 * העבודה מתבצעת ברקע (Threads) כדי לא לתקוע את ממשק המשתמש (UI).
 */
@Service
public class StgnoService 
{

    /**
     * ממשק (Interface) להגדרת Callback לאחר סיום תהליך הטמעה (Embed).
     * מאפשר ל-UI לדעת מתי ה-Thread סיים לעבוד ולקבל את התוצאה.
     */
    public interface EmbedTaskCallback 
    {
        public void onComplete(boolean isSuccess, byte[] resultBytes);
    }

    /**
     * ממשק להגדרת Callback לאחר סיום תהליך חילוץ (Extract).
     */
    public interface ExtractTaskCallback 
    {
        public void onComplete(boolean isSuccess, String msg);
    }

    // רפוזיטורי לשמירת היסטוריית פעולות (הטמעה/חילוץ) במסד הנתונים
    private StgnoRepository stgnoRepository;
    
    // שירותים ספציפיים לכל אלגוריתם הצפנה
    private F5StegoService f5StegoService;       // עבור תמונות JPEG/JPG
    private DSSSStegnoService dsssStegnoService; // עבור קבצי שמע WAV
    private PVDStegoService pvdStegoService;     // עבור תמונות PNG
    private ConvertService convertService; // אות קטנה!
    // אובייקט Thread שישמש להרצת משימות כבדות ברקע
    private Thread StgnoTask;

    /**
     * בנאי המחלקה. Spring מזריק לכאן אוטומטית את הרפוזיטורי ואת כל שירותי האלגוריתמים.
     */
    public StgnoService(StgnoRepository stgnoRepository, F5StegoService f5StegoService, DSSSStegnoService dsssStegnoService, PVDStegoService pvdStegoService, ConvertService convertService) 
    {
        this.stgnoRepository = stgnoRepository;
        this.f5StegoService = f5StegoService;
        this.dsssStegnoService = dsssStegnoService;
        this.pvdStegoService = pvdStegoService;
        this.convertService = convertService;

    }

    /**
     * פונקציית עזר לשמירת תיעוד של הפעולה שבוצעה בהיסטוריית המשתמש במסד הנתונים.
     *
     * @param userId שם המשתמש שביצע את הפעולה
     * @param action סוג הפעולה ("Embed" או "Extract")
     * @param type סוג הקובץ (MimeType)
     * @param data נתוני הקובץ לאחר העיבוד
     */
    public void saveToHistory(String userId, String action, String type, byte[] data) 
    {
        Files stegoEntry = new Files(userId, action, type, data);
        stgnoRepository.save(stegoEntry);
    }

    //_________________________________________הטמעה_________________________________________

    /**
     * פונקציה ראשית להטמעת מסר סודי (Embed). 
     * פותחת Thread חדש כדי לבצע את ההצפנה מבלי לתקוע את המערכת.
     * 
     * @param fileBytes נתוני הקובץ המקוריים
     * @param mimeType סוג הקובץ (קובע איזה אלגוריתם יופעל)
     * @param msg המסר הסודי שיוטמע
     * @param username שם המשתמש לשמירה בהיסטוריה
     * @param embedTaskCallback פונקציית החזרה שתופעל בסיום
     */
    public void embedMsg(byte[] fileBytes, String mimeType, String msg, String username, EmbedTaskCallback embedTaskCallback) 
    {
        StgnoTask = new Thread(() -> 
        {
            byte[] currentFileBytes = fileBytes;
            String currentMimeType = mimeType;

            try 
            {
                // 1. ניתוב עבור תמונות
                if (currentMimeType.startsWith("image/")) {
                    if (!checkValid(currentMimeType, "image")) 
                    {
                        System.out.println("פורמט תמונה לא נתמך. מתחיל המרה ל-PNG...");
                        currentFileBytes = convertService.convertFormat(currentFileBytes, currentMimeType, "png");
                        currentMimeType = "image/png";
                    }
                } 
                // 2. ניתוב עבור קבצי שמע
                else if (currentMimeType.startsWith("audio/")) 
                {
                    if (!checkValid(currentMimeType, "audio")) 
                    {
                        System.out.println("פורמט שמע לא נתמך. מתחיל המרה ל-WAV...");
                        currentFileBytes = convertService.convertFormat(currentFileBytes, currentMimeType, "wav");
                        currentMimeType = "audio/wav";
                    }
                } 
                // 3. סוג קובץ שאינו נתמך כלל (למשל PDF, TXT)
                else 
                {
                    System.err.println("שגיאה: הקובץ אינו תמונה ואינו שמע. לא ניתן להטמיע.");
                    embedTaskCallback.onComplete(false, null);
                    return; // עצירת התהליך
                }

                // המשך ביצוע ההטמעה...

                // 4. ביצוע ההטמעה בפועל לאחר שווידאנו שהקובץ תקין/הומר
                byte[] resultBytes = embed(currentFileBytes, currentMimeType, msg);
                
                if (resultBytes != null) 
                {
                    saveToHistory(username, "Embed", currentMimeType, resultBytes); 
                    embedTaskCallback.onComplete(true, resultBytes);
                } 
                
                else 
                {
                    embedTaskCallback.onComplete(false, null);
                }

            }
             catch (Exception e) 
            {
                System.err.println("שגיאה במהלך תהליך ההטמעה: " + e.getMessage());
                e.printStackTrace();
                embedTaskCallback.onComplete(false, null);
            }
        });
        
        StgnoTask.start();
    }

    /**
     * פונקציית נתב (Router) להטמעה.
     * בודקת את סוג הקובץ וקוראת לשירות האלגוריתם המתאים.
     */
    private byte[] embed(byte[] fileBytes, String mimeType, String msg) throws Exception 
    {
        switch (mimeType) 
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                return embedF5(fileBytes, msg); // העברה לאלגוריתם F5
            case "image/png":
                System.out.println("PVD PNG");
                return embedPVD(fileBytes, msg); // העברה לאלגוריתם PVD
            case "audio/wav":
                System.out.println("embedDSSS");
                return embedDSSS(fileBytes, msg); // העברה לאלגוריתם DSSS
            default:
                return null;
        }
    }

    private byte[] embedDSSS(byte[] fileBytes, String msg) 
    {
        System.out.println("embedDSSS");
        byte[] result = dsssStegnoService.embed(fileBytes, msg);
        return result;
    }

    private byte[] embedPVD(byte[] fileBytes, String msg) throws Exception 
    {
        System.out.println("embedPVD");
        byte[] result = pvdStegoService.embed(fileBytes, msg);
        return result;
    }

    private byte[] embedF5(byte[] fileBytes, String msg) 
    {
        System.out.println("Sending to F5StegoService with message: " + msg);
        byte[] resultBytes = f5StegoService.embed(fileBytes, msg);     
        return resultBytes;
    }

    //______________________________________פענוח______________________________________________

    /**
     * פונקציה ראשית לחילוץ מסר סודי (Extract).
     * פועלת גם היא בצורה אסינכרונית (Thread) כדי למנוע עומס על השרת.
     *
     * @param fileBytes הקובץ שממנו יש לחלץ את המסר
     * @param mimeType סוג הקובץ (קובע איזה אלגוריתם הפוך יופעל)
     * @param extractTaskCallback פונקציית החזרה שתופעל כשיימצא המסר
     */
    public void extractMsg(byte[] fileBytes, String mimeType, ExtractTaskCallback extractTaskCallback) 
    {
        System.out.println("Enter ExtractMsg");

        StgnoTask = new Thread(() -> {
            System.out.println("Enter Thread ExtractMsg");
           
            // ולידציה של סוג הקובץ
            if(checkValid(mimeType, "image") || checkValid(mimeType, "audio")) 
            {
                String msg = null;
                try 
                {
                    // קריאה לפונקציית הניתוב של החילוץ
                    msg = extract(fileBytes, mimeType);
                } 
                
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
                
                System.out.println(msg);
                // הפעלת ה-Callback עם המסר שחולץ
                extractTaskCallback.onComplete(true, msg);
            } 
            
            else 
                extractTaskCallback.onComplete(false, null);
            
        });

        // הרצת תהליך החילוץ ברקע
        StgnoTask.start();
        System.out.println("the task extractMsg & Thread is end");
    }

    /**
     * פונקציית נתב (Router) לחילוץ.
     * בודקת את סוג הקובץ וקוראת לאלגוריתם הפיענוח המתאים.
     */
    private String extract(byte[] fileBytes, String mimeType) throws Exception 
    {
        switch (mimeType) 
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                return extractF5(fileBytes);
            case "image/png":
                System.out.println("PVD PNG");
                return extractPVD(fileBytes);
            case "audio/wav":
                return extractDSSS(fileBytes);
            default:
                return null;
        }
    } 
   
    private String extractDSSS(byte[] fileBytes) 
    {
        String result = dsssStegnoService.extract(fileBytes);
        return result;
    }

    private String extractPVD(byte[] fileBytes) throws Exception 
    {
        System.out.println("Sending to PVDStegoService for extraction");
        String result = pvdStegoService.extract(fileBytes);
        return result;    
    }

    private String extractF5(byte[] fileBytes) 
    {
        System.out.println("Sending to F5StegoService for extraction");
        String result = f5StegoService.extract(fileBytes);
        return result;
    }

    //_________________________________________פונקציות עזר_________________________________________
    
    /**
     * פונקציית ולידציה אחודה.
     * מקבלת את סוג הקובץ המקורי ואת הקטגוריה המבוקשת, 
     * ובודקת האם הוא נתמך ישירות על ידי האלגוריתמים שלנו (ללא המרה).
     *
     * @param mimeType סוג הקובץ המקורי (למשל: "image/heic", "audio/mp3", "image/png")
     * @param fileType קטגוריית הבדיקה ("image" או "audio")
     * @return true אם נתמך ישירות, false אם דורש המרה או לא חוקי
     */
    public boolean checkValid(String mimeType, String fileType) 
    {
        if (mimeType == null) 
            return false;

        // בדיקה ממוקדת עבור תמונות
        if (fileType.equals("image") && mimeType.startsWith("image/")) 
        {
            return mimeType.equals("image/png") 
                || mimeType.equals("image/jpg") 
                || mimeType.equals("image/jpeg");
        }
        
        // בדיקה ממוקדת עבור קבצי שמע
        if (fileType.equals("audio") && mimeType.startsWith("audio/")) {
            return mimeType.equals("audio/wav");
        }

        return false;
    }
}