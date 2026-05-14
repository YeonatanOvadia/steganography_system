package yeonatano.steganography_system.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * מחלקה המייצגת קובץ מדיה (תמונה או אודיו) במסד הנתונים.
 * האנוטציה @Document מציינת שזוהי ישות שתישמר באוסף (Collection) בשם "Files" ב-MongoDB.
 * המחלקה משמשת לשמירת קבצים שהועלו, עובדו או נשלחו בין משתמשים.
 */
@Document(collection = "Files")
public class Files 
{

    /**
     * המזהה הייחודי של הקובץ במסד הנתונים (Primary Key).
     * נוצר אוטומטית על ידי MongoDB בעת השמירה.
     */
    @Id
    private String id;

    // מזהה המשתמש (username) שבבעלותו הקובץ או שביצע את הפעולה
    private String userId;

    // סוג הפעולה שבוצעה (למשל: "Embed", "Extract", "Upload")
    private String actionType;

    // חותמת זמן המציינת מתי הקובץ נוצר או נשמר במערכת
    private Date timestamp;

    // סוג המדיה (MIME Type), למשל: "image/png", "image/jpeg" או "audio/wav"
    private String mediaType;

    // הנתונים הבינאריים של הקובץ עצמו (מערך בייטים)
    private byte[] imageData;

    // דגל לסימון מחיקה לוגית (מאפשר להסתיר קבצים מבלי למחוק אותם פיזית מהמסד)
    private boolean isDeleted;

    /**
     * בנאי ברירת מחדל (Default Constructor).
     * נדרש על ידי Spring Data MongoDB לצורך המרת הנתונים מהמסד לאובייקט Java בזמן שליפה.
     */
    public Files() 
    {
    }

    /**
     * בנאי ליצירת אובייקט קובץ חדש לשמירה.
     * מאתחל אוטומטית את זמן היצירה ואת סטטוס המחיקה (לשקר).
     * 
     * @param userId המשתמש היוצר
     * @param actionType סוג הפעולה שבוצעה על הקובץ
     * @param mediaType סוג הקובץ (MimeType)
     * @param imageData תוכן הקובץ כמערך בייטים
     */
    public Files(String userId, String actionType, String mediaType, byte[] imageData) 
    {
        this.userId = userId;
        this.actionType = actionType;
        this.mediaType = mediaType;
        this.imageData = imageData;
        this.timestamp = new Date(); // קביעת הזמן הנוכחי בעת היצירה
        this.isDeleted = false;      // כברירת מחדל, קובץ חדש אינו מחוק
    }

    // --- Getters & Setters ---

    public String getId() 
    {
        return id;
    }

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getUserId() 
    {
        return userId;
    }

    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getActionType() 
    {
        return actionType;
    }

    public void setActionType(String actionType) 
    {
        this.actionType = actionType;
    }

    public Date getTimestamp() 
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) 
    {
        this.timestamp = timestamp;
    }

    public String getMediaType() 
    {
        return mediaType;
    }

    public void setMediaType(String mediaType) 
    {
        this.mediaType = mediaType;
    }

    public byte[] getImageData() 
    {
        return imageData;
    }

    public void setImageData(byte[] imageData) 
    {
        this.imageData = imageData;
    }

    /**
     * בודק האם הקובץ מסומן כמחוק במערכת.
     * @return true אם הקובץ מחוק לוגית, false אחרת.
     */
    public boolean isDeleted() 
    {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) 
    {
        this.isDeleted = deleted;
    }
}