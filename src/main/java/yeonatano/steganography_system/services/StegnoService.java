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
        public void onComplete(boolean isSuccess, byte[] resultBytes, String errorMessage);    
    }

    /**
     * ממשק להגדרת Callback לאחר סיום תהליך חילוץ (Extract).
     */
    public interface ExtractTaskCallback 
    {
        public void onComplete(boolean isSuccess, String msg, String errorMessage);
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
                    embedTaskCallback.onComplete(false, null, "הקובץ שהועלה אינו נתמך להטמעה");                    return; // יציאה מוקדמת למניעת בזבוז משאבים
                }

                // שלב 2: ניתוב לאלגוריתם ההטמעה וביצוע ההצפנה.
                byte[] resultBytes = embed(currentFileBytes, currentMimeType, msg);
                
                // שלב 3: טיפול בתוצאות והפעלת פונקציות החזרה.
                if (resultBytes != null) 
                {
                    saveToHistory(username, "Embed", currentMimeType, resultBytes); 
                    embedTaskCallback.onComplete(true, resultBytes, null);
                } 
                else 
                {
                    embedTaskCallback.onComplete(false, null, "שגיאה פנימית: האלגוריתם לא החזיר תוצאה");                
                }

            }
            catch (IllegalArgumentException e) 
            {
                // תופס שגיאות חוסר מקום וזורק ל-UI
                embedTaskCallback.onComplete(false, null, e.getMessage());
            }
             catch (Exception e) 
            {
                System.err.println("שגיאה במהלך תהליך ההטמעה: " + e.getMessage());
                e.printStackTrace();
                embedTaskCallback.onComplete(false, null, ""); // הודעת כשלון במקרה של זריקת חריגה
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
                return f5StegoService.embed(fileBytes, msg);     
            case "image/png":
                System.out.println("PVD PNG");
                return pvdStegoService.embed(fileBytes, msg);
            case "audio/wav":
                System.out.println("embedDSSS");
                return dsssStegnoService.embed(fileBytes, msg); // הטמעת שמע באמצעות Direct-Sequence Spread Spectrum
            default:
                return null;
        }
    }
    // ========================================================================
    // מנגנון החילוץ והפענוח (Extraction Engine)
    // ========================================================================

    /**
     * הפונקציה הראשית לחילוץ מסר סודי מתוך קובץ מדיה.
     * הפונקציה מנהלת את תהליך החילוץ בצורה אסינכרונית (ברקע) כדי לא לתקוע את ממשק המשתמש.
     * * @param fileBytes מערך הבייטים של הקובץ (תמונה או שמע) שהמשתמש העלה.
     * @param mimeType סוג הקובץ (למשל "image/png" או "audio/wav") לצורך ניתוב לאלגוריתם הנכון.
     * @param extractTaskCallback אובייקט ה-Callback שדרכו אנו מדווחים ל-UI על הצלחה (והמסר) או כישלון (והשגיאה).
     */
    public void extractMsg(byte[] fileBytes, String mimeType, ExtractTaskCallback extractTaskCallback) 
    {
        System.out.println("Enter ExtractMsg");

        // עטיפת פעולת החילוץ ב-Thread נפרד כדי למנוע חסימה (Blocking) של השרת
        StgnoTask = new Thread(() -> {
            System.out.println("Enter Thread ExtractMsg");
           
            // ולידציה קשיחה: מוודאים שסוג הקובץ נתמך במערכת (תמונה או שמע חוקיים).
            // זה מונע קריסות של Null Pointer או שגיאות של פורמט לא נתמך באלגוריתמים עצמם.
            if(checkValid(mimeType, "image") || checkValid(mimeType, "audio")) 
            {
                String msg = null;
                try 
                {
                    // קריאה לנתב הפנימי שמפעיל את האלגוריתם הספציפי (PVD, F5 או DSSS)
                    msg = extract(fileBytes, mimeType);
                    System.out.println("Extracted message: " + msg);
                    
                    // נקודת ההצלחה:
                    // אם הגענו לשורה הזו, האלגוריתם סיים בהצלחה ולא זרק שום שגיאה.
                    // לכן אנו מדווחים ל-UI שהתהליך עבר בהצלחה (true) ומעבירים לו את המסר שחולץ.
                    extractTaskCallback.onComplete(true, msg, "");
                } 
                catch (Exception e) 
                {
                    // נקודת הכישלון:
                    // אם האלגוריתם גילה שהתמונה פגומה, או שאין מסר, הוא זורק שגיאה.
                    // אנחנו תופסים אותה כאן, ומעבירים ל-UI דיווח על כישלון (false) יחד עם טקסט השגיאה (e.getMessage()).
                    e.printStackTrace();
                    extractTaskCallback.onComplete(false, null, e.getMessage());
                }
            } 
            else 
            {
                // קריאה ל-Callback עם כשלון מידי במידה וסוג הקובץ כלל אינו נתמך במערכת
                extractTaskCallback.onComplete(false, null, "קובץ לא נתמך");
            }
            
        });

        // הפעלת התהליכון (Thread) ברקע
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
                return f5StegoService.extract(fileBytes);

            case "image/png":
                System.out.println("PVD PNG extraction");
                return pvdStegoService.extract(fileBytes);  

            case "audio/wav":
                System.out.println("DSSS WAV extraction");
                return dsssStegnoService.extract(fileBytes);

            default: throw new IllegalArgumentException("פורמט הקובץ אינו נתמך");        
        }
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