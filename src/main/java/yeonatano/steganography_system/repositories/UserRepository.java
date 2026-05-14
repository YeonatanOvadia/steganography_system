package yeonatano.steganography_system.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.User;

/**
 * ממשק רפוזיטורי (Repository) לניהול ישויות המשתמשים (User) במסד הנתונים MongoDB.
 * הממשק יורש מ-MongoRepository, מה שמעניק לו אוטומטית יכולות CRUD בסיסיות 
 * (שמירה, מחיקה, עדכון ושליפה לפי מזהה) ללא צורך בכתיבת קוד נוסף.
 * 
 * הפרמטרים של MongoRepository הם:
 * 1. User - סוג האובייקט שמנוהל.
 * 2. String - סוג הנתון של ה-ID (במקרה שלנו שם המשתמש).
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>
{
    /**
     * מחפש ומחזיר רשימה של כל המשתמשים ששם המשתמש שלהם תואם בדיוק לשם שהתקבל.
     * למרות שבדרך כלל שם משתמש הוא ייחודי, הפונקציה מחזירה רשימה (List).
     *
     * @param name שם המשתמש לחיפוש
     * @return רשימת משתמשים בעלי שם זהה
     */
    public List<User> findAllByUsername(String name);

    /**
     * מחפש משתמשים ששם המשתמש שלהם מכיל או דומה למחרוזת מסוימת.
     * משמש בדרך כלל למימוש תכונות חיפוש חלקי (למשל חיפוש כל המשתמשים שמתחילים ב-"A").
     *
     * @param name תבנית החיפוש
     * @return רשימת משתמשים התואמים לתבנית
     */
    public List<User> findByUsernameLike(String name);
    
    /**
     * מחפש משתמש בודד לפי שם המשתמש המדויק שלו.
     * מחזירה אובייקט Optional כדי לטפל בצורה בטוחה במקרים שבהם המשתמש לא נמצא,
     * ובכך למנוע שגיאות NullPointerException.
     * שימוש נפוץ: בתהליכי התחברות (Authentication).
     *
     * @param username שם המשתמש לחיפוש
     * @return אובייקט Optional המכיל את המשתמש אם נמצא
     */
    public Optional<User> findByUsername(String username);
}