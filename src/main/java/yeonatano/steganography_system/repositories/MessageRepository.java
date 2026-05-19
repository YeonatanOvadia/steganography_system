package yeonatano.steganography_system.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import yeonatano.steganography_system.datamodels.Message;
import java.util.List;

/**
 * ממשק רפוזיטורי (Repository) לניהול ישויות ההודעות (Message) במסד הנתונים MongoDB.
 * הממשק יורש מ-MongoRepository, מה שמאפשר ביצוע פעולות מסד נתונים בסיסיות (CRUD)
 * על אובייקטים מסוג Message, כאשר המזהה הייחודי שלהם (ID) הוא מסוג String.
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> 
{

    /**
     * שאילתה מותאמת אישית השולפת את כל ההודעות שנשלחו לנמען ספציפי.
     * השאילתה מבצעת שני תפקידים:
     * 1. סינון: מוצאת רק הודעות שבהן שדה ה-receiver תואם לערך שהתקבל.
     * 2. מיון: מחזירה את התוצאות מההודעה החדשה ביותר לישנה ביותר (SentTime Descending).
     *
     * @param receiver שם המשתמש של הנמען (זה שקיבל את ההודעה)
     * @return רשימה (List) של הודעות ממוינות לפי זמן שליחה בסדר יורד
     */
    List<Message> findByReceiverOrderBySentTimeDesc(String receiver);

    List<Message> findBySenderOrderBySentTimeDesc(String username);
}