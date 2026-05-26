package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import yeonatano.steganography_system.utilities.DsssUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * מחלקת שירות המיישמת סטגנוגרפיה באודיו מבוססת אלגוריתם DSSS.
 * מבצעת הנחתה של אות השמע, חישוב עוצמת הטמעה (Alpha) דינמית אדפטיבית,
 * מודולציה של סיביות המסר עם רצף PN על גבי דגימות השמע,
 * ופונקציונליות חילוץ עיוור (Blind Extraction) באמצעות קורלציה ונירמול Zero-Mean.
 */
@Service
public class DSSSStegnoService 
{
    // מפתח סימטרי המשמש ליצירת רצף הפסאודו-אקראי (PN Sequence)
    private static String FIXED_PASSWORD = "a1a2a3";

    public byte[] embed(byte[] fileBytes, String messageStr) 
    {
        try 
        {
            // ---------------------------------------------------------
            // שלב 1: המרת קלט
            // ---------------------------------------------------------
            // קידוד מחרוזת המסר למערך בתים בתקן UTF-8 
            //כדי שיהיה לננו מה להצפין
            byte[] messageBytes = messageStr.getBytes(StandardCharsets.UTF_8);

            // ---------------------------------------------------------
            // שלב 2: עיבוד נתוני האודיו המקוריים וחילוץ דגימות השמע
            // ---------------------------------------------------------
            System.out.println("Analyzing audio...");
            
            // 1. טעינת נתוני הקובץ הגולמיים לזרם קלט כדי שנוכל לעבוד עם המחלקה של JAVA
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            
            AudioFormat originalFormat;
            // 2. כיוון שזה קובץ שמע נמיר אותו לסטירים של סאונד כדי שנוכל לחלץ בקלות את ההדרים של הקובץ 
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(inputStream)) {
                // 3. שומרים את הכותרת למשתנה שלנו
                originalFormat = ais.getFormat();
            }
            
            // חובה לאפס כאן את הזרם! (החזרנו את ה-inputStream.reset() שהיה חסר)
            inputStream.reset();
            
            // 4. פשוט מעבירים את המתורגמן הפתוח לפונקציה שלנו כדי שתשאב ממנו את המוזיקה!
            // (מעבירים את inputStream המאופס במקום ais)
            short[] samples = DsssUtils.readWavSamplesFromStream(inputStream);
            //כל עוצמת סאונד היא בעצם מסוג שורת (2 בתים)
            //XXXX XXXX XXXX XXXX(16)
            //אז כרגע כל תא במערך זה מכיל עוצמת קול יחידה

            // ---------------------------------------------------------
            // שלב 3: הנחתה (Attenuation)
            // ---------------------------------------------------------
            for (int i = 0; i < samples.length; i++)
            {
                // הנחתת האמפליטודה של כלל הדגימות ב-10% למניעת הגעה לתקרה (Clipping) בשלב ההטמעה
                samples[i] = (short) (samples[i] * 0.90);
            }

            // ---------------------------------------------------------
            // שלב 4: סידור מבנה הנתונים להטמעה (Bitstream Generation)
            // ---------------------------------------------------------
            int messageLength = messageBytes.length;
            
            // הפקת 16 בייטים כותרת המייצגות את אורך המסר בבתים
            int[] headerBits = DsssUtils.getHeaderBits(messageLength); //מכיל את ההדר שאנחנו צריכים לשרשר בהטמעה

            // המרת המסר ממערך בתים (Bytes) למערך בייטים (Bits)
            int[] messageBits = DsssUtils.bytesToBits(messageBytes);
            //כי אנחנו מטמיעים כל ביט בפני עצמו ולא כל בייט

            // שרשור כותרת המסר וגוף המסר למערך נתונים רציף אחד
            int[] bitsToEmbed = DsssUtils.concatArrays(headerBits, messageBits);
    

            System.out.println("Message length: " + messageLength + " bytes");
            System.out.println("Total bits to embed: " + bitsToEmbed.length);

            // ---------------------------------------------------------
            // שלב 5: מודולציה והטמעה (Embedding Loop)
            // ---------------------------------------------------------
            
            // הפקת רצף DSSS פסאודו-אקראי בגודל קובץ השמע
            // יצירת רעש מיסוך
            int[] pnSequence = DsssUtils.generatePnSequence(FIXED_PASSWORD, samples.length);

            // חישוב המיקום של השנייה הראשונה בקובץ, לצורך דילוג מטעמי יציבות אות
            // על ידי חילוץ מההדר כמה זה שנייה
            int skipIndex = (int) originalFormat.getSampleRate() * originalFormat.getChannels();
            
            //פוינטר לשמע
            int sampleIndex = skipIndex; 

            // פונינטר למסר
            int msgIndex;
            
            for (msgIndex = 0; msgIndex < bitsToEmbed.length; msgIndex++)
            {
                // וידוא כי נותרה קיבולת מספקת בקובץ להטמעת הסיבית הנוכחית
                if (sampleIndex + DsssUtils.SAMPLES_PER_BIT >= samples.length) 
                {
                    System.out.println("Error: Audio capacity too small for this message!");
                    return null;
                }

                // --- חישוב מקדם עוצמת הטמעה אדפטיבית (Local Alpha) ---
                // סוכם את כל הבלוק פריסה 
                long localSum = 0;
                for (int i = 0; i < DsssUtils.SAMPLES_PER_BIT; i++) 
                {
                    localSum += Math.abs(samples[sampleIndex + i]);
                }

                // חישוב הממוצע בבלוק הפריסה הזה
                double localAvg = localSum / (double) DsssUtils.SAMPLES_PER_BIT;
                
                // קביעת מקדם אלפא ל-21% מהעוצמה הממוצעת המקומית
                double localAlpha = localAvg * 0.21; 
                
                // חסימת ערכי קיצון למקדם האלפא (Lower & Upper bounds)
                if (localAlpha < 150) localAlpha = 150;
                if (localAlpha > 3500) localAlpha = 3500;

                // המרת הסיבית לייצוג ביפולרי (+1 או -1) עבור המתמטיקה של DSSS
                int bitToEmbed = bitsToEmbed[msgIndex];
                int bipolarBit = (bitToEmbed == 1) ? 1 : -1;

                // פיזור (Spread) והטמעת הסיבית המאופננת על פני בלוק דגימות השמע
                for (int i = 0; i < DsssUtils.SAMPLES_PER_BIT; i++) 
                {
                    // חישוב המודולציה לפי נוסחת DSSS: Alpha * Bit * PN
                    double mod = localAlpha * bipolarBit * pnSequence[sampleIndex];
                    double newVal = samples[sampleIndex] + mod;

                    // מנגנון הגנה מפני גלישה מספרית (Overflow/Underflow) מטיפוס Short
                    if (newVal > Short.MAX_VALUE) newVal = Short.MAX_VALUE;
                    else if (newVal < Short.MIN_VALUE) newVal = Short.MIN_VALUE;

                    samples[sampleIndex] = (short) newVal;
                    sampleIndex++;
                }
            }

            // ---------------------------------------------------------
            // שלב 6: קידוד דגימות השמע ושמירה כקובץ WAV
            // ---------------------------------------------------------
            if (msgIndex == bitsToEmbed.length) 
            {
                System.out.println("\n--- Saving Stego Audio ---");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                
                // המרת מערך ה-short חזרה למערך בתים (כולל הרכבת ה-WAV Header מחדש) לפי הפורמט המקורי
                DsssUtils.saveWavFileToStream(outputStream, samples, originalFormat);
                
                return outputStream.toByteArray();
            }
            
            return null;
        } 
        catch (Exception e) 
        {
            e.printStackTrace(); 
            return null;
        }
    }

    public String extract(byte[] fileBytes) 
    {
        try 
        {
            // ---------------------------------------------------------
            // שלב 1: אתחול וקריאת נתוני האודיו המוטמעים
            // ---------------------------------------------------------
            System.out.println("Analyzing stego audio...");
            InputStream inputStream = new ByteArrayInputStream(fileBytes);

            AudioFormat originalFormat;
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(inputStream)) {
                originalFormat = ais.getFormat();
            }
            inputStream.reset();
            
            short[] samples = DsssUtils.readWavSamplesFromStream(inputStream);

            // יצירת רצף ה-PN הזהה לרצף אשר שימש בשלב ההטמעה
            int[] pnSequence = DsssUtils.generatePnSequence(FIXED_PASSWORD, samples.length);
            
            int skipIndex = (int) originalFormat.getSampleRate() * originalFormat.getChannels();
            int sampleIndex = skipIndex;

            // ---------------------------------------------------------
            // שלב 2: חילוץ אורך המסר (16 Bits) בעזרת נירמול מקומי
            // ---------------------------------------------------------
            int messageLength = 0;
            
            for (int i = 0; i < 16; i++) 
            {
                if (sampleIndex + DsssUtils.SAMPLES_PER_BIT >= samples.length) break;

                // חישוב רכיב DC המקומי (ממוצע) לצורך Zero-Mean Normalization
                double blockSum = 0;
                for (int j = 0; j < DsssUtils.SAMPLES_PER_BIT; j++) 
                {
                    blockSum += samples[sampleIndex + j];
                }
                double blockMean = blockSum / DsssUtils.SAMPLES_PER_BIT;

                double correlation = 0;
                
                // ביצוע מכפלה פנימית (קורלציה) על בלוק הדגימות המנורמל
                for (int j = 0; j < DsssUtils.SAMPLES_PER_BIT; j++) 
                {
                    double normalizedSample = samples[sampleIndex] - blockMean;
                    correlation += normalizedSample * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                // קבלת החלטה לוגית: ערך חיובי מעיד על '1', ערך שלילי מעיד על '0'
                correlation /= DsssUtils.SAMPLES_PER_BIT;
                int bit = (correlation > 0) ? 1 : 0;
                
                // ביצוע שיפט לוגי (Bitwise OR) לבניית הערך השלם המייצג את האורך
                messageLength = (messageLength << 1) | bit;
            }

            System.out.println("Detected message length: " + messageLength + " bytes");

            // ולידציה של אורך המסר כנגד קיבולת הקובץ
            int maxPossibleBytes = (samples.length - skipIndex) / (DsssUtils.SAMPLES_PER_BIT * 8);
            if (messageLength <= 0 || messageLength > maxPossibleBytes) {
                return "שגיאה: הקובץ לא מכיל מסר חוקי, או שהמוזיקה רועשת מדי לפיענוח מדויק.";
            }

            // ---------------------------------------------------------
            // שלב 3: חילוץ נתוני המסר (Payload)
            // ---------------------------------------------------------
            int totalBitsToExtract = messageLength * 8;
            int[] extractedBits = new int[totalBitsToExtract];
            int msgIndex = 0;

            System.out.println("Extracting message...");

            // חזרה על מנגנון הקורלציה והנירמול לצורך חילוץ הסיביות הנותרות
            while (msgIndex < totalBitsToExtract) 
            {
                if (sampleIndex + DsssUtils.SAMPLES_PER_BIT >= samples.length) break;

                double blockSum = 0;
                for (int j = 0; j < DsssUtils.SAMPLES_PER_BIT; j++) {
                    blockSum += samples[sampleIndex + j];
                }
                double blockMean = blockSum / DsssUtils.SAMPLES_PER_BIT;

                double correlation = 0;
                
                for (int i = 0; i < DsssUtils.SAMPLES_PER_BIT; i++) 
                {
                    if (sampleIndex >= samples.length) break; 
                    double normalizedSample = samples[sampleIndex] - blockMean;
                    correlation += normalizedSample * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                correlation /= DsssUtils.SAMPLES_PER_BIT;
                
                extractedBits[msgIndex] = (correlation > 0) ? 1 : 0;
                msgIndex++;
            }

            // ---------------------------------------------------------
            // שלב 4: המרה חוזרת ממערך סיביות לייצוג טקסטואלי
            // ---------------------------------------------------------
            byte[] messageBytes = DsssUtils.bitsToBytes(extractedBits);
            String hiddenMessage = new String(messageBytes, StandardCharsets.UTF_8);

            System.out.println("\n--- Extraction Complete ---");
            System.out.println("Hidden Message: " + hiddenMessage);
            System.out.println("---------------------------");

            return hiddenMessage;

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return "Extraction failed: " + e.getMessage();
        }
    }
}