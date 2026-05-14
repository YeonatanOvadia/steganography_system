package yeonatano.steganography_system.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * מחלקה המייצגת הודעה במערכת הסטגנוגרפיה.
 * האנוטציה @Document מציינת שזוהי ישות שתישמר באוסף (Collection) בשם "messages" ב-MongoDB.
 */
@Document(collection = "messages")
public class Message 
{

    // מזהה ייחודי של ההודעה במסד הנתונים
    @Id
    private String id;
    
    // שם המשתמש השולח
    private String sender;
    
    // שם המשתמש הנמען
    private String receiver;
    
    // תוכן ההודעה הגלוי (הטקסט הפשוט)
    private String body;
    
    // חותמת זמן המציינת מתי ההודעה נוצרה. ברירת המחדל היא זמן היצירה הנוכחי.
    private LocalDateTime sentTime = LocalDateTime.now();
    
    // מזהה הקובץ המצורף (ID של אובייקט Files). יכול להיות null אם אין קובץ מצורף.
    private String fileId;

    /**
     * בנאי ברירת מחדל (ריק).
     * נדרש על ידי Spring Data MongoDB לצורך המרת הנתונים מהמסד לאובייקט Java.
     */
    public Message() 
    {
    }

    /**
     * בנאי ליצירת הודעה חדשה עם פרטי השולח, הנמען והתוכן.
     * 
     * @param sender שם השולח
     * @param receiver שם הנמען
     * @param body תוכן הטקסט
     * @param fileId מזהה הקובץ המצורף (אם קיים)
     */
    public Message(String sender, String receiver, String body, String fileId) 
    {
        this.sender = sender;
        this.receiver = receiver;
        this.body = body;
        this.fileId = fileId;
    }

    // --- Getters ---

    public String getId() 
    {
        return id;
    }

    public String getSender() 
    {
        return sender;
    }

    public String getReceiver() 
    {
        return receiver;
    }

    public String getBody() 
    {
        return body;
    }

    public LocalDateTime getSentTime() 
    {
        return sentTime;
    }

    public String getFileId() 
    {
        return fileId;
    }

    /**
     * פונקציית עזר הבודקת האם להודעה מצורף קובץ.
     * 
     * @return true אם קיים מזהה קובץ שאינו ריק, אחרת false
     */
    public boolean hasFile() 
    {
        return fileId != null && !fileId.trim().isEmpty();
    }

    public void addClickListener(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addClickListener'");
    }
}