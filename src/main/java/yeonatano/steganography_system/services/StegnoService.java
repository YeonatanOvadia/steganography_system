package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.FilesRepository;

/**
 * מחלקת השירות המרכזית (Facade Pattern) לניהול פעולות הסטגנוגרפיה במערכת.
 * מחלקה זו משמשת כ"שער גישה" יחיד עבור שכבת התצוגה (UI), ומסתירה ממנה את המורכבות
 * של בחירת אלגוריתמי ההצפנה השונים, המרות הפורמטים ותהליכי הרקע.
 * 
 * בנוסף, המחלקה מנהלת את הארכיטקטורה האסינכרונית של המערכת על ידי עטיפת פעולות 
 * הקידוד והפענוח הכבדות בתהליכונים (Threads), כדי למנוע חסימה (Blocking) של ממשק המשתמש.
 */
@Service
public class StegnoService 
{

    /**
     * ממשק (Interface) להגדרת Callback לאחר סיום תהליך הטמעה (Embed).
     * תבנית זו מאפשרת תקשורת אסינכרונית: ה-Thread שעובד ברקע משתמש בפונקציה זו
     * כדי "להודיע" בחזרה לשכבת ה-UI שהעבודה הסתיימה ולהעביר לה את התוצאה.
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

    // רפוזיטורי לשמירת תיעוד של הפעולות (Audit Trail) במסד הנתונים
    private FilesRepository filesRepository;
    
    // שירותי אלגוריתמים ספציפיים (Strategy-like structure)
    private F5StegnoService f5StegoService;       // לטיפול בקבצי JPEG/JPG
    private DSSSStegnoService dsssStegnoService; // לטיפול בקבצי שמע WAV
    private PVDStegnoService pvdStegoService;     // לטיפול בקבצי PNG
    
    // שירות המרה - מגביר את גמישות המערכת על ידי טיפול בקבצים שאינם נתמכים במקור
    private ConvertService convertService; 
    
    // אובייקט Thread מנוהל להרצת אלגוריתמים מבלי "להקפיא" את השרת והלקוח
    private Thread StgnoTask;

    /**
     * בנאי המחלקה. מנצל את מנגנון ה-IoC (Inversion of Control) של Spring 
     * לצורך הזרקת תלויות (Dependency Injection) של כלל שירותי הליבה והרפוזיטורי.
     */
    public StegnoService(FilesRepository filesRepository, F5StegnoService f5StegoService, DSSSStegnoService dsssStegnoService, PVDStegnoService pvdStegoService, ConvertService convertService) 
    {
        this.filesRepository = filesRepository;
        this.f5StegoService = f5StegoService;
        this.dsssStegnoService = dsssStegnoService;
        this.pvdStegoService = pvdStegoService;
        this.convertService = convertService;
    }

    /**
     * רושמת פעולת סטגנוגרפיה (הצלחה/ביצוע) לתוך היסטוריית המערכת במסד הנתונים.
     *
     * @param userId מזהה המשתמש שביצע את הפעולה.
     * @param action סוג הפעולה (למשל "Embed" או "Extract").
     * @param type פורמט המדיה (MIME Type) של הקובץ שנוצר.
     * @param data נתוני הקובץ (BLOB) לשמירה.
     */
    public void saveToHistory(String userId, String action, String type, byte[] data) 
    {
        Files stegoEntry = new Files(userId, action, type, data);
        filesRepository.save(stegoEntry);
    }

    // ========================================================================
    // מנגנון ההטמעה (Embedding Engine)
    // ========================================================================

    /**
     * הפונקציה הראשית (Entry Point) להטמעת מסר סודי לתוך קובץ.
     * מפעילה תהליך רב-שלבי: ולידציה -> המרת פורמט (Fallback) -> הטמעה לפי ניתוב -> שמירה -> Callback.
     *
     * @param fileBytes נתוני קובץ המקור הבינאריים.
     * @param mimeType סוג הקובץ. משמש כפרמטר החלטה (Decision parameter) בניתוב לאלגוריתם.
     * @param msg טקסט המסר הסודי שיוצפן בתוך המדיה.
     * @param username שם המשתמש לשמירה ביומן (Log).
     * @param embedTaskCallback פונקציית התגובה שתעודכן בסוף התהליך.
     */
    public void embedMsg(byte[] fileBytes, String mimeType, String msg, String username, EmbedTaskCallback embedTaskCallback) 
    {
        // יצירת Thread חדש כדי למנוע Blocked UI. אלגוריתמים כמו F5 דורשים זמן חישוב (CPU bound).
        StgnoTask = new Thread(() -> 
        {
            byte[] currentFileBytes = fileBytes;
            String currentMimeType = mimeType;

            try 
            {
                // שלב 1: ולידציה דינמית והמרת קבצים (Graceful Degradation).
                // אם המשתמש העלה תמונה שאינה נתמכת ישירות על ידי האלגוריתמים (למשל WebP או GIF),
                // המערכת לא קורסת, אלא ממירה אותה אוטומטית ל-PNG הנתמך על ידי PVD.
                if (currentMimeType.startsWith("image/")) {
                    if (!checkValid(currentMimeType, "image")) 
                    {
                        System.out.println("פורמט תמונה לא נתמך. מתחיל המרה ל-PNG...");
                        currentFileBytes = convertService.convertFormat(currentFileBytes, currentMimeType, "png");
                        currentMimeType = "image/png";
                    }
                } 
                // כנ"ל לגבי שמע: המרה אוטומטית של פורמטים לא נתמכים ל-WAV עבור אלגוריתם DSSS.
                else if (currentMimeType.startsWith("audio/")) 
                {
                    if (!checkValid(currentMimeType, "audio")) 
                    {
                        System.out.println("פורמט שמע לא נתמך. מתחיל המרה ל-WAV...");
                        currentFileBytes = convertService.convertFormat(currentFileBytes, currentMimeType, "wav");
                        currentMimeType = "audio/wav";
                    }
                } 
                // חסימת קבצים שאינם קשורים לעיבוד אותות או פיקסלים (כמו PDF או DOCX).
                else 
                {
                    System.err.println("שגיאה: הקובץ אינו תמונה ואינו שמע. לא ניתן להטמיע.");
                    embedTaskCallback.onComplete(false, null);
                    return; // יציאה מוקדמת למניעת בזבוז משאבים
                }

                // שלב 2: ניתוב לאלגוריתם ההטמעה וביצוע ההצפנה.
                byte[] resultBytes = embed(currentFileBytes, currentMimeType, msg);
                
                // שלב 3: טיפול בתוצאות והפעלת פונקציות החזרה.
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
                embedTaskCallback.onComplete(false, null); // הודעת כשלון במקרה של זריקת חריגה
            }
        });
        
        StgnoTask.start();
    }

    /**
     * נתב פנימי (Internal Router).
     * מנתב את הבקשה לשירות הסטגנוגרפיה המדויק על בסיס חתימת הפורמט של הקובץ.
     */
    private byte[] embed(byte[] fileBytes, String mimeType, String msg) throws Exception 
    {
        switch (mimeType) 
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg");
                return embedF5(fileBytes, msg); // הטמעה במרחב התדר באמצעות F5
            case "image/png":
                System.out.println("PVD PNG");
                return embedPVD(fileBytes, msg); // הטמעה במרחב המרחבי (הפרשי פיקסלים) באמצעות PVD
            case "audio/wav":
                System.out.println("embedDSSS");
                return embedDSSS(fileBytes, msg); // הטמעת שמע באמצעות Direct-Sequence Spread Spectrum
            default:
                return null;
        }
    }

    private byte[] embedDSSS(byte[] fileBytes, String msg) 
    {
        System.out.println("embedDSSS");
        return dsssStegnoService.embed(fileBytes, msg);
    }

    private byte[] embedPVD(byte[] fileBytes, String msg) throws Exception 
    {
        System.out.println("embedPVD");
        return pvdStegoService.embed(fileBytes, msg);
    }

    private byte[] embedF5(byte[] fileBytes, String msg) 
    {
        System.out.println("Sending to F5StegoService with message: " + msg);
        return f5StegoService.embed(fileBytes, msg);     
    }

    // ========================================================================
    // מנגנון החילוץ והפענוח (Extraction Engine)
    // ========================================================================

    /**
     * הפונקציה הראשית לחילוץ מסר סודי מתוך מדיה נגועה (Stego-object).
     * עוטפת את תהליך הקריאה והפענוח ב-Thread נפרד.
     *
     * @param fileBytes המדיה הבינארית החשודה שמכילה מסר.
     * @param mimeType סוג המדיה לניתוב לאלגוריתם הפיענוח.
     * @param extractTaskCallback פונקציית התגובה שתקבל את המחרוזת המפוענחת.
     */
    public void extractMsg(byte[] fileBytes, String mimeType, ExtractTaskCallback extractTaskCallback) 
    {
        System.out.println("Enter ExtractMsg");

        StgnoTask = new Thread(() -> {
            System.out.println("Enter Thread ExtractMsg");
           
            // ולידציה קשיחה למניעת קריסות (Null Pointer / Unsupported Format) באלגוריתמים
            if(checkValid(mimeType, "image") || checkValid(mimeType, "audio")) 
            {
                String msg = null;
                try 
                {
                    msg = extract(fileBytes, mimeType);
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
                
                System.out.println("Extracted message: " + msg);
                extractTaskCallback.onComplete(true, msg);
            } 
            else 
            {
                // קריאה ל-Callback עם כשל במקרה של קובץ לא חוקי
                extractTaskCallback.onComplete(false, null);
            }
            
        });

        StgnoTask.start();
        System.out.println("the task extractMsg & Thread is end");
    }

    /**
     * נתב פנימי לחילוץ (Extraction Router).
     */
    private String extract(byte[] fileBytes, String mimeType) throws Exception 
    {
        switch (mimeType) 
        {
            case "image/jpg":
            case "image/jpeg":
                System.out.println("f5 jpeg extraction");
                return extractF5(fileBytes);
            case "image/png":
                System.out.println("PVD PNG extraction");
                return extractPVD(fileBytes);
            case "audio/wav":
                System.out.println("DSSS WAV extraction");
                return extractDSSS(fileBytes);
            default:
                return null;
        }
    } 
   
    private String extractDSSS(byte[] fileBytes) 
    {
        return dsssStegnoService.extract(fileBytes);
    }

    private String extractPVD(byte[] fileBytes) throws Exception 
    {
        System.out.println("Sending to PVDStegoService for extraction");
        return pvdStegoService.extract(fileBytes);    
    }

    private String extractF5(byte[] fileBytes) 
    {
        System.out.println("Sending to F5StegoService for extraction");
        return f5StegoService.extract(fileBytes);
    }

    // ========================================================================
    // פונקציות עזר (Utilities)
    // ========================================================================
    
    /**
     * פונקציית ולידציה.
     * בודקת האם סוג הקובץ והפורמט נתמכים באופן טבעי (Natively) על ידי המערכת.
     * משמשת את שכבת הלוגיקה כדי להחליט אם יש צורך להפעיל את ה-ConvertService.
     *
     * @param mimeType ה-MIME Type לבדיקה (למשל "image/jpeg").
     * @param fileType קטגוריית על ("image" או "audio").
     * @return true אם נתמך, false אחרת.
     */
    public boolean checkValid(String mimeType, String fileType) 
    {
        if (mimeType == null) 
            return false;

        if (fileType.equals("image") && mimeType.startsWith("image/")) 
        {
            // המערכת תומכת באופן טבעי בפורמטים מסוג PNG ו-JPEG
            return mimeType.equals("image/png") 
                || mimeType.equals("image/jpg") 
                || mimeType.equals("image/jpeg");
        }
        
        if (fileType.equals("audio") && mimeType.startsWith("audio/")) {
            // המערכת תומכת באופן טבעי בקבצי שמע לא דחוסים (WAV)
            return mimeType.equals("audio/wav");
        }

        return false;
    }
}