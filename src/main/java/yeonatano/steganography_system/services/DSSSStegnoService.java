package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import yeonatano.steganography_system.utilities.DsssUtils; 

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * מחלקת שירות לאלגוריתם DSSS.
 * מבצעת הנמכה של האודיו, חישוב עוצמת הטמעה (Alpha) באופן דינמי,
 * ופיזור (Spread) של ביטי המסר על פני דגימות האודיו יחד עם רצף ה-PN.
 * כוללת גם חילוץ עיוור (Blind Extraction) - אינה יודעת את עוצמת האלפא או את האודיו המקורי,
 * ומסתמכת אך ורק על חישוב קורלציה (מכפלה פנימית) בעזרת סיסמת ה-PN.
 */
@Service
public class DSSSStegnoService 
{
    // private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    // סיסמה קבועה המשמשת ליצירת רצף ה-PN עבור כל ההטמעות והחילוצים
    private static final String FIXED_PASSWORD = "a1a2a3";

    public byte[] embed(MemoryBuffer audioFile, String messageStr) 
    {
        try {
            // התאמה ל-Stream: מקבלים את המידע ישירות מה-Buffer
            InputStream inputStream = audioFile.getInputStream();

            // --- תחילת הלוגיקה שלך ---

            // ---------------------------------------------------------
            // שלב 1: קלט ובדיקות מקדמיות
            // ---------------------------------------------------------
            // מגבלת גודל קובץ כדי למנוע קריסת זיכרון (OutOfMemoryError)
            // (הערה: ניתן לבדוק גם ברמת ה-Upload של Vaadin)
            
            // המרת המחרוזת לבייטים לפי תקן UTF-8 (תומך גם בעברית)
            byte[] messageBytes = messageStr.getBytes(StandardCharsets.UTF_8);

            // ---------------------------------------------------------
            // שלב 2: קריאת נתוני האודיו
            // ---------------------------------------------------------
            System.out.println("Analyzing audio...");
            DsssUtils.AudioData audioData = DsssUtils.readWavSamplesFromStream(inputStream);
            short[] samples = audioData.samples;

            // ---------------------------------------------------------
            // שלב 3: הנמכה (Attenuation) וחישוב אלפא (Alpha)
            // ---------------------------------------------------------
            long sum = 0;
            for (int i = 0; i < samples.length; i++) 
            {
                // הנמכת כל הקובץ ב-10% כדי להשאיר 'מרווח נשימה' להטמעה ללא קליפינג
                samples[i] = (short) (samples[i] * 0.90);
                sum += Math.abs(samples[i]);
            }
            
            // קביעת עוצמת ההטמעה (25% מהעוצמה הממוצעת של הקובץ)
            double avgAmplitude = sum / (double) samples.length;
            double alpha = avgAmplitude * 0.25;
            
            // הגבלות קצוות: מונע עוצמה חלשה מדי (שתימחק) או חזקה מדי (שתרעיש)
            if (alpha < 50) alpha = 50;
            if (alpha > 1000) alpha = 1000;

            // ---------------------------------------------------------
            // שלב 4: הכנת "רכבת הביטים" להטמעה
            // ---------------------------------------------------------
            int messageLength = messageBytes.length;
            
            // יצירת 16 ביטים המייצגים את אורך ההודעה (ה-Header)
            int[] headerBits = DsssUtils.getHeaderBits(messageLength); 
            
            // פירוק ההודעה עצמה לביטים
            int[] messageBits = DsssUtils.bytesToBits(messageBytes);
            
            // איחוד ה-Header וההודעה למערך אחד רציף
            int[] bitsToEmbed = DsssUtils.concatArrays(headerBits, messageBits);

            System.out.println("Message length: " + messageLength + " bytes");
            System.out.println("Total bits to embed: " + bitsToEmbed.length);

            // ---------------------------------------------------------
            // שלב 5: תהליך ההטמעה (Embedding Loop)
            // ---------------------------------------------------------
            
            // יצירת מפתח הפריסה על כל אורך הקובץ
            int[] pnSequence = DsssUtils.generatePnSequence(FIXED_PASSWORD, samples.length);

            // חישוב כמות הדגימות של שנייה אחת (כדי לדלג עליהן ולמנוע רעש התחלתי)
            int skipIndex = (int) audioData.format.getSampleRate() * audioData.format.getChannels();
            
            int sampleIndex = skipIndex; // המיקום באודיו
            int msgIndex;
            // המיקום במערך הביטים להטמעה
            for (msgIndex = 0; msgIndex < bitsToEmbed.length; msgIndex++)
            {
                // בדיקת קיבולת: האם נשאר מספיק מקום בקובץ עבור הביט הבא?
                if (sampleIndex + DsssUtils.SAMPLES_PER_BIT >= samples.length) 
                {
                    System.out.println("Error: Audio capacity too small for this message!");
                    return null;
                }

                // המרת הביט מ-(0) לפורמט ביפולרי (1-) לטובת האלגוריתם המתמטי
                int bitToEmbed = bitsToEmbed[msgIndex];
                int bipolarBit = (bitToEmbed == 1) ? 1 : -1;

                // פיזור (Spread) הביט הבודד על פני SAMPLES_PER_BIT דגימות
                for (int i = 0; i < DsssUtils.SAMPLES_PER_BIT; i++) 
                {
                    // הנוסחה המרכזית: דגימה חדשה = דגימה נוכחית + (אלפא * ביט * PN)
                    double mod = alpha * bipolarBit * pnSequence[sampleIndex];

                    double newVal = samples[sampleIndex] + mod;

                    // הגנת גלישה (Clipping): הבטחה שלא נחרוג מגבולות טיפוס ה-short
                    if (newVal > Short.MAX_VALUE) newVal = Short.MAX_VALUE;
                    else if (newVal < Short.MIN_VALUE) newVal = Short.MIN_VALUE;

                    // שמירת הדגימה המעודכנת
                    samples[sampleIndex] = (short) newVal;
                    sampleIndex++;
                }
            }

            // ---------------------------------------------------------
            // שלב 6: שמירת התוצאה
            // ---------------------------------------------------------
            if (msgIndex == bitsToEmbed.length) 
            {
                System.out.println("\n--- Saving Stego Audio ---");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                DsssUtils.saveWavFileToStream(outputStream, audioData);
                
                return convertToBuffer(outputStream, audioFile.getFileName(), audioFile.getFileData().getMimeType());
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] convertToBuffer(ByteArrayOutputStream os, String fileName, String mimeType) {
        // כאן קורה ה"קסם": הפקודה toByteArray יוצרת מערך חדש 
        // בדיוק באורך של כל הביטים שנשפכו לתוך ה-OutputStream.
        // שום ביט לא הולך לאיבוד כי המערך נוצר רק אחרי שכל הכתיבה הסתיימה.
        
        if (os == null) {
            return null;
        }
        
        byte[] result = os.toByteArray();
        
        // הדפסה לדיבאג כדי שתוכל לראות בטרמינל שהמערך אכן נוצר בגודל הנכון
        System.out.println("File: " + fileName + " converted to byte array. Size: " + result.length + " bytes.");
        
        return result; 
    }

    public String extract(MemoryBuffer stegoFile) {
        try {
            // ---------------------------------------------------------
            // שלב 1: טעינת נתונים ומפתחות
            // ---------------------------------------------------------
            System.out.println("Analyzing stego audio...");
            InputStream inputStream = stegoFile.getInputStream();
            DsssUtils.AudioData audioData = DsssUtils.readWavSamplesFromStream(inputStream);
            short[] samples = audioData.samples;

            // יצירת אותו רצף PN בדיוק שבו השתמש המטמיע
            int[] pnSequence = DsssUtils.generatePnSequence(FIXED_PASSWORD, samples.length);
            
            // דילוג על השנייה הראשונה בדיוק כפי שעשה המטמיע
            int skipIndex = (int) audioData.format.getSampleRate() * audioData.format.getChannels();
            int sampleIndex = skipIndex;

            // ---------------------------------------------------------
            // שלב 2: חילוץ אורך המסר (Header - 16 Bits)
            // ---------------------------------------------------------
            int messageLength = 0;
            
            // לולאה הרצה 16 פעמים כדי לחלץ את 16 הביטים של האורך
            for (int i = 0; i < 16; i++) 
            {
                double correlation = 0;
                
                // חישוב קורלציה עבור בלוק דגימות של ביט אחד
                for (int j = 0; j < DsssUtils.SAMPLES_PER_BIT; j++) 
                {
                    // סכום המכפלות: דגימה מוטמעת כפול ערך ה-PN המקומי
                    correlation += samples[sampleIndex] * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                // ממוצע הקורלציה. אם הוא חיובי - הוטמע 1. אם שלילי - הוטמע 0.
                correlation /= DsssUtils.SAMPLES_PER_BIT;
                int bit = (correlation > 0) ? 1 : 0;
                
                // דחיפת הביט שנמצא אל תוך המספר הסופי (משמאל לימין)
                messageLength = (messageLength << 1) | bit;
            }

            System.out.println("Detected message length: " + messageLength + " bytes");

            // בדיקת סבירות (Sanity Check) כדי למנוע קריסה במקרה של סיסמה שגויה
            if (messageLength < 1 || messageLength > 65535) 
            {
                return "Error: Invalid length. Wrong password or corrupted file.";
            }

            // ---------------------------------------------------------
            // שלב 3: חילוץ המסר עצמו
            // ---------------------------------------------------------
            int totalBitsToExtract = messageLength * 8;
            int[] extractedBits = new int[totalBitsToExtract];
            int msgIndex = 0;

            System.out.println("Extracting message...");

            // לולאה הרצה עד שחילצנו את כל ביטי ההודעה
            while (msgIndex < totalBitsToExtract) 
            {
                double correlation = 0;
                
                // חישוב קורלציה לביט הנוכחי
                for (int i = 0; i < DsssUtils.SAMPLES_PER_BIT; i++) 
                {
                    if (sampleIndex >= samples.length) break; // הגנת חריגה
                    correlation += samples[sampleIndex] * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                correlation /= DsssUtils.SAMPLES_PER_BIT;
                
                // החלטה ושמירה במערך החילוץ
                extractedBits[msgIndex] = (correlation > 0) ? 1 : 0;
                msgIndex++;
            }

            // ---------------------------------------------------------
            // שלב 4: הרכבה מחדש והצגה
            // ---------------------------------------------------------
            
            // המרת הביטים לבתים, והבתים למחרוזת UTF-8
            byte[] messageBytes = DsssUtils.bitsToBytes(extractedBits);
            String hiddenMessage = new String(messageBytes, StandardCharsets.UTF_8);

            System.out.println("\n--- Extraction Complete ---");
            System.out.println("Hidden Message: " + hiddenMessage);
            System.out.println("---------------------------");

            return hiddenMessage;

        } catch (Exception e) {
            e.printStackTrace();
            return "Extraction failed: " + e.getMessage();
        }
    }
}