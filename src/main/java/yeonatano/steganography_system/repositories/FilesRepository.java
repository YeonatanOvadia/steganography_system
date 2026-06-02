package yeonatano.steganography_system.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.Files;

/**
 * ממשק רפוזיטורי לניהול הקבצים (Files) במסד הנתונים MongoDB.
 */
@Repository
public interface FilesRepository extends MongoRepository<Files, String> 
{
    /*
     * מכיוון שהממשק יורש מ-MongoRepository, Spring Data מספק לנו 
     * מימוש מוכן לפונקציות כמו:
     * - save(Files file): שמירת קובץ חדש או עדכון קיים.
     * - findById(String id): שליפת קובץ לפי ה-ID שלו.
     * - findAll(): שליפת כל הקבצים הקיימים.
     * - deleteById(String id): מחיקת קובץ.
     */
     
    /**
     * שליפת כל הקבצים המשויכים למשתמש מסוים.
     * @param userId מזהה המשתמש.
     * @return רשימת הקבצים של המשתמש.
     */
    List<Files> findAllByUserId(String userId);


    /**
     * שליפת המידע הבינארי (imageData) בלבד עבור קובץ ספציפי, לחסכון בתעבורת רשת.
     * @param id מזהה הקובץ.
     * @return אובייקט Files המכיל רק את המידע הבינארי של התמונה/השמע.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'imageData': 1, '_id': 0 }")
    Files findImageDataById(String id);

    // הוסף את זה לתוך FilesRepository
    /**
     * שליפת היסטוריית הקבצים הפעילים של המשתמש לטובת התצוגה.
     * מסנן החוצה את המידע הבינארי הכבד (imageData: 0) כדי למנוע קריסת זיכרון.
     * @param userId מזהה המשתמש.
     * @return רשימת קבצים (מטא-דאטה בלבד) שאינם מסומנים כמחוקים.
     */
    @Query(value = "{ 'userId': ?0, 'isDeleted': false }", fields = "{ 'imageData': 0 }")
    List<Files> findHistoryWithoutData(String userId);
}