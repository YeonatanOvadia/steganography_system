package yeonatano.steganography_system.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import yeonatano.steganography_system.datamodels.Files;

/**
 * ממשק רפוזיטורי (Repository) לניהול ישויות הקבצים (Files) במסד הנתונים MongoDB.
 * הממשק יורש מ-MongoRepository, מה שמעניק למערכת יכולות לביצוע פעולות CRUD 
 * (שמירה, שליפה, עדכון ומחיקה) על קבצים המאוחסנים בבסיס הנתונים בצורה אוטומטית.
 * 
 * הפרמטרים של MongoRepository:
 * 1. Files - סוג האובייקט (ה-Entity) המנוהל.
 * 2. String - סוג הנתון של המזהה הייחודי (ID).
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
    List<Files> findAllByUserId(String userId);


    @Query(value = "{ '_id': ?0 }", fields = "{ 'imageData': 1, '_id': 0 }")
    Files findImageDataById(String id);

    // הוסף את זה לתוך FilesRepository
    @Query(value = "{ 'userId': ?0, 'isDeleted': false }", fields = "{ 'imageData': 0 }")
    List<Files> findHistoryWithoutData(String userId);
}