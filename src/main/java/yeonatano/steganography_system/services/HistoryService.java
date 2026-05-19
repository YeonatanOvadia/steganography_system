package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;

import yeonatano.steganography_system.datamodels.Files;
import yeonatano.steganography_system.repositories.FilesRepository;
import java.util.List;

@Service
public class HistoryService 
{
    private final FilesRepository filesRepository;

    public HistoryService(FilesRepository filesRepository) 
    {
        this.filesRepository = filesRepository;
    }

    // החלף את הפונקציה הקיימת בזו:
    public List<Files> getActiveUserHistory(String username) 
    {
        return filesRepository.findHistoryWithoutData(username);
    }

    public void softDeleteFile(String fileId) 
    {
        filesRepository.findById(fileId).ifPresent(file -> 
        {
            file.setDeleted(true);      
            filesRepository.save(file); 
        });
    }

        // -- הפונקציה החדשה --
        // מושכת את המידע הבינארי של קובץ בודד לפי דרישה
        public byte[] getFileData(String fileId) {
        try {
            // אנחנו מוודאים שהשליפה נקודתית ומהירה
            return filesRepository.findById(fileId)
                    .map(Files::getImageData)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error fetching file data: " + e.getMessage());
            return null;
        }
    }
}