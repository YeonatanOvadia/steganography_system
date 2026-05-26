package yeonatano.steganography_system.services;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.repositories.UserRepository;

/**
 * מחלקת שירות (Service Layer) לניהול הלוגיקה העסקית של זהויות משתמשים במערכת.
 * מחלקה זו מפרידה בין שכבת התצוגה (UI) לבין שכבת הנתונים (Repository),
 * ואחראית על תהליכי הרשמה (Registration), אימות (Authentication) והצפנת סיסמאות מובנית.
 * 
 * האנוטציה @Service מגדירה את המחלקה כ-Bean המנוהל על ידי קונטיינר ה-IoC של Spring Boot.
 */
@Service
public class UserService 
{

    // שימוש בממשק PasswordEncoder (בדרך כלל ממומש כ-Bcrypt) כדי להבטיח שסיסמאות 
    // נשמרות כגיבוב (Hash) ולא כטקסט גלוי (Plain text), למניעת חשיפה במקרה של פריצה למסד הנתונים.
    private PasswordEncoder passwordEncoder;
    
    // אובייקט הגישה לנתונים (DAO/Repository) המשמש לביצוע פעולות CRUD מול מסד הנתונים.
    private UserRepository userRepository;

    /**
     * בנאי המחלקה - מבצע הזרקת תלויות (Constructor Dependency Injection).
     * זוהי הדרך המומלצת ביותר ב-Spring (במקום שימוש ב-@Autowired על המשתנים), 
     * מכיוון שהיא מבטיחה שהמשתנים יהיו final, מונעת מצב של חוסר אתחול, ומקלה על כתיבת בדיקות יחידה (Unit Tests).
     *
     * @param userRepository ממשק הגישה למסד הנתונים.
     * @param passwordEncoder רכיב ההצפנה/גיבוב המוגדר ברמת אפליקציית ה-Spring.
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) 
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * מוסיף משתמש חדש למערכת.
     * הפונקציה מנהלת את תהליך הרישום הכולל וידוא ייחודיות ושמירת סיסמה בצורה מאובטחת.
     *
     * @param user אובייקט המשתמש (מכיל את הסיסמה כטקסט גלוי כפי שהוקלדה ב-UI).
     * @return true אם ההרשמה עברה בהצלחה, false אם קיים כבר משתמש עם אותו שם (מזהה ייחודי).
     */
    public boolean addUserToDB(User user) 
    {
        // 1. ולידציה עסקית: בדיקה מול מסד הנתונים האם מזהה המשתמש (Primary Key) כבר תפוס
        if (userRepository.existsById(user.getUsername())) 
        {
            return false; // מונע דריסת נתונים (Data Overwrite) ומחזיר שגיאה ל-UI
        }

        // 2. אבטחת מידע (Hashing): העברת הסיסמה דרך אלגוריתם הגיבוב
        // הפונקציה encode מחוללת Hash חד-כיווני (One-way hash) בשילוב Salt אקראי
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // 3. שמירת הרשומה المאובטחת במסד הנתונים
        userRepository.insert(user);
        return true;
    }

    /**
     * מאמת פרטי התחברות של משתמש קיים מול מסד הנתונים.
     * מנגנון זה משתמש בפונקציית matches שנועדה להתמודד עם סיסמאות שהוצפנו עם Salt.
     *
     * @param username שם המשתמש שהוזן בטופס ההתחברות.
     * @param rawPassword הסיסמה הגלויה שהוזנה בטופס.
     * @return אובייקט המשתמש השלם מה-DB אם האימות הצליח, או null אם אחד מהפרטים שגוי.
     */
    public User authenticate(String username, String rawPassword) 
    {
        // 1. שליפה בטוחה: שימוש ב-Optional כדי להימנע משגיאות NullPointerException (NPE)
        // במקרה שהמשתמש לא קיים במסד הנתונים.
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        // בדיקה האם האובייקט מכיל תוכן (המשתמש נמצא)
        if (userOpt.isPresent()) 
        {
            User user = userOpt.get();
            
            // 2. אימות קריפטוגרפי: 
            // אסור להשוות String רגיל. פונקציית matches מבצעת גיבוב לסיסמה הגלויה (rawPassword)
            // ומשווה אותה מול הגיבוב השמור (user.getPassword()) בצורה בטוחה נגד התקפות תזמון (Timing Attacks).
            if (passwordEncoder.matches(rawPassword, user.getPassword())) 
            {
                return user; // האימות עבר בהצלחה
            }
        }
        
        // מטעמי אבטחה (Security through Obscurity), אנו מחזירים null גנרי
        // ולא מפרטים אם שם המשתמש לא קיים או שהסיסמה הייתה שגויה.
        return null; 
    }

    /**
     * שולף את רשימת כל המשתמשים הרשומים במערכת.
     * (הערה ארכיטקטונית: במערכות ייצור גדולות נהוג להוסיף כאן מנגנון עימוד - Pagination).
     *
     * @return רשימה של אובייקטי User.
     */
    public ArrayList<User> getAllUsers() 
    {
        // שימוש בפונקציית findAll המובנית של Spring Data והמרתה למבנה הנתונים המבוקש
        return (ArrayList<User>)userRepository.findAll();
    }
}