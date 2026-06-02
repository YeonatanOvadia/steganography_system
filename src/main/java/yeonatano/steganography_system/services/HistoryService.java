package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.FilesRepository;
import java.util.List;

/**
 * שירות (Service) לניהול היסטוריית הפעולות והקבצים של המשתמשים.
 * מחלקה זו מנוהלת על ידי Spring (@Service) ומרכזת את הלוגיקה העסקית מול מסד הנתונים
 * בכל הנוגע לשליפה, תצוגה ומחיקה של היסטוריית סטגנוגרפיה.
 * * דגש ארכיטקטוני: המחלקה מתוכננת לביצועים גבוהים (Performance Optimization) 
 * על ידי הפרדה בין שליפת המטא-דאטה (המידע היבש) לבין שליפת הקבצים הכבדים (BLOBs).
 */
@Service
public class HistoryService 
{
    // רפוזיטורי לגישה לנתוני הקבצים במסד הנתונים (Data Access Layer)
    private FilesRepository filesRepository;

    /**
     * בנאי המחלקה. 
     * משתמש בהזרקת תלויות (Dependency Injection) של Spring כדי לקבל את הרפוזיטורי.
     */
    public HistoryService(FilesRepository filesRepository) 
    {
        this.filesRepository = filesRepository;
    }

    /**
     * שולף את היסטוריית הפעולות של המשתמש, ללא הנתונים הבינאריים הכבדים.
     * שיפור ביצועים (Optimization): פונקציה זו מחזירה רק מטא-דאטה (כמו תאריך, סוג פעולה) 
     * כדי שרינדור הטבלה ב-UI יהיה מהיר ולא יסתום את זיכרון ה-RAM של השרת או רשת התקשורת.
     *
     * @param username שם המשתמש שאת היסטוריית הפעולות שלו אנו רוצים לשלוף
     * @return רשימה של אובייקטי Files המכילים רק נתוני טקסט (ללא ה-byte[])
     */
    public List<Files> getActiveUserHistory(String username) 
    {
        return filesRepository.findHistoryWithoutData(username);
    }

    /**
     * מבצע "מחיקה רכה" (Soft Delete) לקובץ מההיסטוריה.
     * במקום למחוק את הרשומה לחלוטין ממסד הנתונים (Hard Delete), 
     * אנו מסמנים אותה כ"נמחקה" (Flag). הדבר מאפשר שחזור נתונים בעתיד,
     * שומר על שלמות המידע (Data Integrity) ומונע שגיאות בטבלאות מקושרות.
     *
     * @param fileId המזהה הייחודי (ID) של הקובץ למחיקה
     */
    public void softDeleteFile(String fileId) 
    {
        // חיפוש הקובץ במסד הנתונים, ואם הוא קיים (ifPresent) - נסמן אותו כמחוק
        filesRepository.findById(fileId).ifPresent(file -> 
        {
            file.setDeleted(true);      // הדלקת דגל המחיקה הרכה
            filesRepository.save(file); // שמירת העדכון במסד הנתונים
        });
    }

    /**
     * מושכת את המידע הבינארי המלא (BLOB) של קובץ בודד לפי דרישה (On-Demand / Lazy Fetching).
     * פונקציה זו נקראת על ידי ה-UI רק כאשר המשתמש לוחץ בפועל על כפתור "תצוגה" או "הורדה".
     * גישה זו מונעת טעינה מיותרת של מגה-בייטים רבים של נתונים לזיכרון.
     *
     * @param fileId המזהה הייחודי של הקובץ שממנו נרצה לחלץ את המדיה
     * @return מערך הבייטים (byte[]) המייצג את הקובץ עצמו, או null אם לא נמצא/שגיאה
     */
    public byte[] getFileData(String fileId) 
    {
        try 
        {
            // שליפה נקודתית ומהירה לפי מפתח ראשי (Primary Key)
            // שימוש ב-Optional וב-Streams של Java 8 לחילוץ אלגנטי של הנתונים
            return filesRepository.findById(fileId)
                    .map(Files::getImageData)
                    .orElse(null); // אם הקובץ לא קיים, מחזירים null בבטחה
        } 
        catch (Exception e) 
        {
            // תפיסת שגיאות I/O או בעיות שליפה מהמסד למניעת קריסת השרת
            System.err.println("Error fetching file data: " + e.getMessage());
            return null;
        }
    }
}