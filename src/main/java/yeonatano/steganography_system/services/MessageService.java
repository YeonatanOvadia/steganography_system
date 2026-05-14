package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import com.vaadin.flow.data.provider.DataProvider;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.datamodels.Message;
import yeonatano.steganography_system.repositories.FilesRepository;
import yeonatano.steganography_system.repositories.MessageRepository;
import java.util.List;
import java.util.Optional;

/**
 * מחלקת שירות (Service) המרכזת את הלוגיקה העסקית הקשורה להודעות וקבצים במערכת.
 * תגית ה-@Service מסמנת ל-Spring לנהל את המחלקה הזו כ-Bean, 
 * כך שניתן יהיה להזריק אותה למחלקות אחרות (כמו קונטרולרים).
 */
@Service
public class MessageService 
{
    
    // הרפוזיטוריז שמשמשים לשמירה ושליפה של נתונים ממסד הנתונים
    private final MessageRepository messageRepository;
    private final FilesRepository filesRepository;

    /**
     * בנאי המחלקה. משמש להזרקת תלויות (Dependency Injection).
     * Spring יספק אוטומטית את מופעי הרפוזיטוריז הנדרשים.
     *
     * @param messageRepository גישה לנתוני ההודעות ב-DB
     * @param filesRepository גישה לנתוני הקבצים ב-DB
     */
    public MessageService(MessageRepository messageRepository, FilesRepository filesRepository) 
    {
        this.messageRepository = messageRepository;
        this.filesRepository = filesRepository;
    }

    /**
     * פונקציה לשליחת הודעה חדשה, עם אפשרות לצרף אליה קובץ.
     * 
     * @param sender שם המשתמש ששולח את ההודעה
     * @param receiver שם המשתמש שאליו נשלחת ההודעה
     * @param body תוכן הטקסט של ההודעה
     * @param fileData הנתונים הבינאריים של הקובץ המצורף (מערך בייטים). יכול להיות null אם אין קובץ.
     * @param mimeType סוג הקובץ המצורף (למשל: image/jpeg)
     * @param actionType סוג הפעולה שקשורה לקובץ (רלוונטי לתהליכי הסטגנוגרפיה במערכת)
     */
    public void sendMessage(String sender, String receiver, String body, byte[] fileData, String mimeType, String actionType) 
    {
        String fileId = null;


        // שלב 1: בדיקה האם צורף קובץ להודעה (האם המערך אינו null ואינו ריק)
        if (fileData != null && fileData.length > 0) 
        {
            // יצירת אובייקט קובץ חדש עם פרטי השולח והקובץ
            Files newFile = new Files(sender, actionType, mimeType, fileData);
            
            // שמירת הקובץ במסד הנתונים וקבלת האובייקט השמור (כולל ה-ID שנוצר לו)
            newFile = filesRepository.save(newFile);
            
            // שמירת מזהה הקובץ שנוצר, כדי לקשר אותו להודעה עצמה
            fileId = newFile.getId(); 
        }

        // שלב 2: יצירת אובייקט ההודעה החדש (אם יש קובץ, יצורף המזהה שלו, אחרת fileId יהיה null)
        Message msg = new Message(sender, receiver, body, fileId);
        
        // שמירת ההודעה במסד הנתונים
        messageRepository.save(msg);
    }

    /**
     * פונקציה לשליפת כל ההודעות הנכנסות של משתמש מסוים.
     * ההודעות יוחזרו ממוינות מהחדשה ביותר לישנה ביותר (Descending).
     *
     * @param username שם המשתמש (הנמען)
     * @return רשימה של הודעות השייכות לאותו משתמש
     */
    public List<Message> getMyInbox(String username) 
    {
        // קריאה לפונקציה ברפוזיטורי שמחפשת לפי נמען וממיינת לפי זמן שליחה
        return messageRepository.findByReceiverOrderBySentTimeDesc(username);
    }

    /**
     * פונקציה לשליפת קובץ ממסד הנתונים לפי המזהה (ID) שלו.
     * הפונקציה מגינה מפני קריסות (בודקת קלט ריק) ומוודאת שהקובץ לא נמחק לוגית.
     *
     * @param fileId מזהה הקובץ שברצוננו לשלוף
     * @return אובייקט הקובץ (Files) אם הוא קיים ופעיל, אחרת מחזירה null
     */
    public Files getFileById(String fileId) 
    {
        // בדיקת תקינות בסיסית - חזרה מהירה של null אם ה-ID ריק או null
        if (fileId == null || fileId.trim().isEmpty()) return null;
        
        // חיפוש הקובץ במסד הנתונים, מוחזר כאובייקט Optional כדי למנוע NullPointerException
        Optional<Files> fileOpt = filesRepository.findById(fileId);
        
        // בדיקה כפולה: גם שהקובץ נמצא ב-DB, וגם שהוא לא סומן כמחוק (isDeleted() שקר)
        if (fileOpt.isPresent()) 
        {
            return fileOpt.get(); // הקובץ תקין - נחזיר אותו
        }
        
        // במקרה שהקובץ לא קיים או שסומן כמחוק
        return null;
    }

    public void deleteMessage(String id) 
    {
        messageRepository.deleteById(id);
    }



    public List<Message> getMySentMessages(String username) 
    {
        // קריאה לפונקציה ברפוזיטורי שמחפשת לפי נמען וממיינת לפי זמן שליחה
        return messageRepository.findBySenderOrderBySentTimeDesc(username);
    }
}