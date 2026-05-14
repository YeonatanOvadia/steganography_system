package yeonatano.steganography_system.services;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.repositories.UserRepository;

/**
 * מחלקת שירות (Service) המטפלת בלוגיקה הקשורה למשתמשים במערכת.
 * כוללת פעולות כגון הוספת משתמש חדש (כולל הצפנת סיסמה), אימות פרטי התחברות ושליפת משתמשים.
 */
@Service
public class UserService 
{

    // רכיב המשמש להצפנה ובדיקה של סיסמאות (Hash)
    private final PasswordEncoder passwordEncoder;
    
    // הרפוזיטורי המשמש לגישה לנתוני המשתמשים במסד הנתונים
    private final UserRepository userRepository;

    /**
     * בנאי המחלקה להזרקת תלויות (Dependency Injection) על ידי Spring.
     *
     * @param userRepository גישה למסד הנתונים של המשתמשים
     * @param passwordEncoder רכיב להצפנת סיסמאות
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) 
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * פונקציה להוספת משתמש חדש למסד הנתונים.
     * מוודאת שהמשתמש לא קיים כבר, ומצפינה את הסיסמה שלו לפני השמירה.
     *
     * @param user אובייקט המשתמש שיש להוסיף (כולל שם משתמש וסיסמה גולמית)
     * @return true אם המשתמש נוסף בהצלחה, false אם המשתמש כבר קיים במערכת
     */
    public boolean addUserToDB(User user) 
    {
        // בדיקה האם כבר קיים במערכת משתמש עם אותו שם (מזהה ייחודי)
        if (userRepository.existsById(user.getUsername())) 
        {
            return false;
        }

        // הצפנת הסיסמה הגולמית של המשתמש לפני השמירה במסד הנתונים לטובת אבטחה
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // הכנסת המשתמש החדש (עם הסיסמה המוצפנת) למסד הנתונים
        userRepository.insert(user);
        return true;
    }

    /**
     * פונקציה לאימות פרטי התחברות (Login) של משתמש.
     * בודקת האם המשתמש קיים והאם הסיסמה שהוזנה תואמת לזו ששמורה במערכת.
     *
     * @param username שם המשתמש שניגש למערכת
     * @param rawPassword הסיסמה הגולמית (כפי שהוקלדה על ידי המשתמש)
     * @return אובייקט המשתמש (User) אם האימות הצליח, או null אם נכשל (משתמש לא קיים או סיסמה שגויה)
     */
    public User authenticate(String username, String rawPassword) 
    {
        // 1. מחפשים את המשתמש ב-DB לפי שם המשתמש
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        // אם המשתמש נמצא במסד הנתונים
        if (userOpt.isPresent()) 
        {
            User user = userOpt.get();
            
            // 2. משתמשים ב-passwordEncoder כדי לבדוק אם הסיסמה הגולמית תואמת לגיבוב (Hash) ששמור ב-DB
            if (passwordEncoder.matches(rawPassword, user.getPassword())) 
                return user; // אימות הצליח - מחזירים את המשתמש
        }
        
        return null; // אימות נכשל (או שהמשתמש לא קיים, או שהסיסמה לא תואמת)
    }

    /**
     * פונקציה לשליפת כלל המשתמשים הרשומים במערכת.
     *
     * @return רשימה (ArrayList) של כל המשתמשים
     */
    public ArrayList<User> getAllUsers() 
    {
        // שליפת כל המשתמשים מהרפוזיטורי והמרת התוצאה ל-ArrayList
        return (ArrayList<User>)userRepository.findAll();
    }
}