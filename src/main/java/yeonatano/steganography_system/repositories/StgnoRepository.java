package yeonatano.steganography_system.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.Files;

/**
 * ממשק רפוזיטורי (Repository) לניהול ישויות הקבצים (Files) במסד הנתונים MongoDB.
 * הממשק יורש מ-MongoRepository, מה שמאפשר ביצוע פעולות מסד נתונים בסיסיות (CRUD)
 * על אובייקטים מסוג Files, כאשר המזהה הייחודי שלהם (ID) הוא מסוג String.
 */
@Repository
public interface StgnoRepository extends MongoRepository<Files, String>
{
    /**
     * שאילתה מותאמת אישית השולפת את כל הקבצים השייכים למשתמש ספציפי.
     * Spring Data מפענח את שם המתודה (findAllByUserId) ומייצר אוטומטית 
     * את השאילתה ל-MongoDB שמבצעת סינון לפי שדה ה-userId.
     *
     * @param userId המזהה של המשתמש שאת הקבצים שלו אנו רוצים לשלוף
     * @return רשימה (List) של כל הקבצים שנמצאו עבור אותו משתמש
     */
}