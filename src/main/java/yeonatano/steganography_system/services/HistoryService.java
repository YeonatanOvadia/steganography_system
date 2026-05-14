package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.FilesRepository;

import java.util.List;

/**
 * מחלקת שירות (Service) המנהלת את היסטוריית הפעולות של המשתמשים.
 * השירות מאפשר לשלוף את כל הקבצים והפעולות שבוצעו על ידי משתמש ספציפי מתוך מסד הנתונים.
 */
@Service
public class HistoryService 
{

    // הרפוזיטורי המשמש לגישה לנתוני הקבצים המעובדים במסד הנתונים
    private FilesRepository filesRepository;

    /**
     * בנאי המחלקה להזרקת תלויות על ידי Spring.
     * 
     * @param stgnoRepository גישה לנתוני הסטגנוגרפיה והקבצים השמורים
     */
    public HistoryService(FilesRepository filesRepository) 
    {
        this.filesRepository = filesRepository;
    }

    /**
     * שולפת את כל היסטוריית הקבצים עבור משתמש מסוים.
     * הפונקציה מחפשת ב-DB את כל הרשומות שבהן מזהה המשתמש תואם ל-userId שהתקבל.
     * 
     * @param userId שם המשתמש או המזהה הייחודי שלו
     * @return רשימה של אובייקטי Files המכילים את נתוני הקבצים, סוג הפעולה וזמן הביצוע
     */
    public List<Files> getActiveUserHistory(String username) 
    {
        return filesRepository.findByUserIdAndIsDeletedFalse(username);
    }

    public void softDeleteFile(String fileId) 
    {
        filesRepository.findById(fileId).ifPresent(file -> 
    {
        file.setDeleted(true);
        filesRepository.save(file);
    });
}
}