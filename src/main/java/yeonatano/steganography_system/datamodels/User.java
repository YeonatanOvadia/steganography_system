package yeonatano.steganography_system.datamodels;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

/**
 * מחלקה המייצגת משתמש במערכת הסטגנוגרפיה.
 * האנוטציה @Document מציינת שזוהי ישות שתישמר באוסף (Collection) בשם "Users" ב-MongoDB.
 */
@Document(collection = "Users")
public class User
{

    /**
     * שם המשתמש משמש כמזהה הייחודי (Primary Key) של המסמך במסד הנתונים.
     * האנוטציה @Id מבטיחה שלא יהיו שני משתמשים עם אותו שם במערכת.
     */
    @Id
    private String username = null;
    
    // סיסמת המשתמש (בדרך כלל תישמר כערך מוצפן/מגובב ב-DB)
    private String password = null;
    
    /**
     * בנאי ברירת מחדל (Default Constructor).
     * נדרש על ידי Spring Data MongoDB לצורך בניית האובייקט בעת שליפה מהמסד.
     */
    public User(){}

    /**
     * בנאי ליצירת משתמש חדש עם שם וסיסמה.
     * 
     * @param username שם המשתמש הייחודי
     * @param password סיסמת המשתמש
     */
    public User (String username, String password)
    {
        this.password = password;
        this.username = username;
    }

    // --- Getters & Setters ---

    public String getUsername() 
    {
        return username;
    }

    public void setUsername(String username) 
    {
        this.username = username;
    }

    public String getPassword() 
    {
        return password;
    }

    public void setPassword(String password) 
    {
        this.password = password;
    }

   
    @Override
    public String toString() 
    {
        return "User [username=" + username + ", password=" + password + "]";
    }

    /**
     * מתודה זמנית להגדרת סטטוס משתמש.
     * כרגע המתודה אינה ממומשת וזריקת החריגה (Exception) נשארה כפי שהוגדרה במקור.
     * 
     * @param string הסטטוס להגדרה
     * @throws UnsupportedOperationException בכל קריאה למתודה זו
     */

}