package yeonatano.steganography_system.datamodels;

// הייבוא המתוקן ששייך למסד הנתונים!
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "Files") 
public class Files 
{

    @Id
    private String id;
    
    // הוספנו את השדות לפי הצעת הפרויקט כדי לדעת מי העלה
    private String userId;      
    private String actionType;  
    private Date timestamp;     
    private String mediaType;   
    private byte[] imageData;

    public Files() {}

    public Files(String userId, String actionType, String mediaType, byte[] imageData) {
        this.userId = userId;
        this.actionType = actionType;
        this.mediaType = mediaType;
        this.imageData = imageData;
        this.timestamp = new Date(); // שומר אוטומטית את התאריך והשעה של עכשיו
    }


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
}