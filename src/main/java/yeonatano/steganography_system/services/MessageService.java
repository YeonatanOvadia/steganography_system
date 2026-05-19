package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.datamodels.Message;
import yeonatano.steganography_system.repositories.FilesRepository;
import yeonatano.steganography_system.repositories.MessageRepository;
import java.util.List;
import java.util.Optional;

/**
 * מחלקת שירות (Service Layer) המרכזת את הלוגיקה העסקית של תעבורת ההודעות והקבצים במערכת.
 * מחלקה זו מתפקדת כ"מנצח על התזמורת" (Orchestrator) בין נתוני הטקסט לבין נתוני המדיה (BLOBs).
 * 
 * תגית ה-@Service מסמנת ל-Spring Boot לנהל את המחלקה הזו כ-Singleton Bean 
 * בתוך קונטיינר ה-IoC, מה שמאפשר הזרקתה למחלקות התצוגה (Views) והקונטרולרים בקלות.
 */
@Service
public class MessageService 
{
    
    // אובייקטי הגישה לנתונים (Data Access Objects - DAO).
    // מוגדרים כ-final כדי להבטיח שהם מאותחלים פעם אחת בלבד בעת יצירת המחלקה (Immutability).
    private final MessageRepository messageRepository;
    private final FilesRepository filesRepository;

    /**
     * בנאי המחלקה - מיישם הזרקת תלויות (Constructor Dependency Injection).
     * זוהי הפרקטיקה המומלצת ביותר ב-Spring (לעומת הזרקה עם @Autowired על השדות),
     * כיוון שהיא הופכת את הקוד לקל יותר לבדיקה (Unit Testing) ומונעת מצב של תלויות חסרות.
     *
     * @param messageRepository ממשק לשליפה ושמירה של אובייקטי Message
     * @param filesRepository ממשק לשליפה ושמירה של אובייקטי Files
     */
    public MessageService(MessageRepository messageRepository, FilesRepository filesRepository) 
    {
        this.messageRepository = messageRepository;
        this.filesRepository = filesRepository;
    }

    /**
     * שולח הודעה חדשה, תוך ניהול חכם של צירוף קבצים.
     * 
     * 💡 החלטה ארכיטקטונית (Normalization/Referencing):
     * במקום לשמור את המידע הבינארי הכבד (מערך הבייטים של הקובץ) ישירות בתוך אובייקט ההודעה, 
     * אנו שומרים קודם את הקובץ בטבלה/אוסף נפרד, שולפים את ה-ID הייחודי שלו, 
     * ורק אותו שומרים בהודעה (כמפתח זר - Foreign Key). 
     * זה מונע עומס זיכרון (Out Of Memory) כאשר שולפים רשימה ארוכה של הודעות ל-Inbox.
     * 
     * @param sender שם השולח
     * @param receiver שם הנמען
     * @param body תוכן גלוי
     * @param fileData הנתונים הבינאריים של הקובץ (יכול להיות null)
     * @param mimeType סוג הקובץ (למשל image/png)
     * @param actionType סוג הפעולה שבוצעה על הקובץ (למשל "Embed" או "Upload")
     */
    public void sendMessage(String sender, String receiver, String body, byte[] fileData, String mimeType, String actionType) 
    {
        String fileId = null;

        // שלב 1: טיפול בקובץ המצורף (אם קיים)
        // מוודאים שהמערך מאותחל ויש בו נתונים ממש (גדול מ-0 בייטים)
        if (fileData != null && fileData.length > 0) 
        {
            Files newFile = new Files(sender, actionType, mimeType, fileData);
            
            // השמירה מחזירה את האובייקט המעודכן מהמסד, הכולל כעת את ה-ID שנוצר אוטומטית (Auto-Generated ID)
            newFile = filesRepository.save(newFile);
            
            // חילוץ המזהה החדש לצורך קישורו להודעה
            fileId = newFile.getId(); 
        }

        // שלב 2: הרכבת ושמירת ההודעה
        // אם לא היה קובץ, fileId יישאר null, וזהו מצב תקין לחלוטין (הודעת טקסט בלבד)
        Message msg = new Message(sender, receiver, body, fileId);
        messageRepository.save(msg);
    }

    /**
     * שולף את תיבת הדואר הנכנס ("Inbox") עבור משתמש ספציפי.
     *
     * @param username שם המשתמש (הנמען)
     * @return רשימת הודעות מסודרת כרונולוגית בסדר יורד (Descending - מהחדש לישן)
     */
    public List<Message> getMyInbox(String username) 
    {
        // 💡 שימוש בכוח של Spring Data JPA/Mongo:
        // אין צורך לכתוב שאילתת SQL/MQL מורכבת. Spring מפרש את שם הפונקציה בזמן הריצה
        // (findBy + Receiver + OrderBy + SentTime + Desc) ובונה את השאילתה אוטומטית ביעילות.
        return messageRepository.findByReceiverOrderBySentTimeDesc(username);
    }

    /**
     * שולף את תיבת הדואר היוצא ("Sent") עבור משתמש ספציפי.
     *
     * @param username שם המשתמש (השולח)
     * @return רשימת הודעות מסודרת כרונולוגית בסדר יורד
     */
    public List<Message> getMySentMessages(String username) 
    {
        return messageRepository.findBySenderOrderBySentTimeDesc(username);
    }

    /**
     * שליפת קובץ בינארי ממסד הנתונים באמצעות המזהה שלו.
     * משמשת את שכבת התצוגה (UI) כאשר משתמש מבקש לראות תצוגה מקדימה או להוריד קובץ.
     *
     * @param fileId המזהה הייחודי (UUID/ObjectId) של הקובץ
     * @return אובייקט ה-Files השלם (כולל מערך הבייטים), או null במקרה של שגיאה או קובץ חסר
     */
    public Files getFileById(String fileId) 
    {
        // ולידציה מוקדמת (Fail-Fast): הגנה על שאילתת המסד מפני קלטים ריקים שיגרמו לשגיאות
        if (fileId == null || fileId.trim().isEmpty()) return null;
        
        // שימוש ב-Optional הוא Best Practice ב-Java המודרנית למניעת NullPointerException.
        // הוא מציין באופן מפורש שהתוצאה מהמסד עלולה לא להיות קיימת.
        Optional<Files> fileOpt = filesRepository.findById(fileId);
        
        if (fileOpt.isPresent()) 
        {
            // פריקת האובייקט מתוך מעטפת ה-Optional בבטחה
            return fileOpt.get(); 
        }
        
        // החזרת null שקטה אם הקובץ לא נמצא (מאפשר ל-UI להציג הודעת "הקובץ הוסר" בצורה אלגנטית)
        return null;
    }

    /**
     * מחיקת הודעה ממסד הנתונים.
     * שים לב: פונקציה זו מוחקת את רשומת ההודעה בלבד (Hard Delete של אובייקט ה-Message).
     * אם נדרש למחוק גם את הקובץ המקושר או לבצע מחיקה לוגית (Soft Delete), זה המקום להרחיב את הלוגיקה.
     *
     * @param id מזהה ההודעה למחיקה
     */
    public void deleteMessage(String id) 
    {
        messageRepository.deleteById(id);
    }
}