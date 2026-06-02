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
import java.util.Random;

/**
 * מחלקת שירות המיישמת סטגנוגרפיה באודיו מבוססת אלגוריתם DSSS.
 * מבצעת הנחתה של אות השמע, חישוב עוצמת הטמעה (Alpha) דינמית אדפטיבית,
 * מודולציה של סיביות המסר עם רצף PN על גבי דגימות השמע,
 * ופונקציונליות חילוץ עיוור (Blind Extraction) באמצעות קורלציה ונירמול Zero-Mean.
 * * סיבוכיות מקום (Space Complexity): O(S)
 * כאשר S הוא מספר דגימות האודיו (Samples). כל הדגימות נטענות למערך בזיכרון, וכן נוצר רצף PN 
 * באותו הגודל. הקצאת הזיכרון גדלה באופן ליניארי ביחס לאורך קובץ השמע.
 */
@Service
public class DSSSStegnoService 
{
    // מפתח סימטרי המשמש ליצירת רצף הפסאודו-אקראי (PN Sequence)
    private String SECRET_SEED = DsssUtils.SECRET_SEED;
    private int SEED_HASH = SECRET_SEED.hashCode();
    private int SAMPLES_PER_BIT = DsssUtils.SAMPLES_PER_BIT;
    private int HEADER_BITS = DsssUtils.HEADER_BITS;
    private int MIN_SKIP = DsssUtils.MIN_SKIP;
    private int MAX_SKIP = DsssUtils.MAX_SKIP;

    /**
     * פונקציית ההטמעה (Embedding) המאפננת את מסר הטקסט לתוך גלי הקול של השמע המקורי.
     * הלוגיקה מבוססת על הנחתת השמע ב-10%, בדיקת קיבולת מקדימה (Fail-Fast) עם חישוב דילוגים מקסימלי,
     * חישוב עוצמת הטמעה אדפטיבית (Local Alpha) לכל בלוק, ופיזור (Spread) של הביטים בעזרת רצף ה-PN.
     * לאחר כל ביט מתבצע דילוג אקראי המסונכרן בעזרת Seed.
     *
     * @param fileBytes נתוני קובץ האודיו המקורי (WAV) במערך בתים.
     * @param messageStr המסר הסודי להטמעה בקידוד UTF-8.
     * @return מערך בתים (byte[]) המייצג את קובץ ה-Stego-Audio המוכן.
     * @throws IllegalArgumentException אם הקובץ קצר מדי ולא יכול להכיל את המסר והדילוגים.
     * @throws Exception עבור שגיאות קריאה, כתיבה או המרת פורמטים.
     * * זמן ריצה (Time Complexity) במקרה הגרוע (Worst Case):
     * O(S)
     * כאשר S הוא המספר הכולל של הדגימות (Samples). כל לולאות העזר (הנחתה, יצירת PN) רצות 
     * באופן ליניארי O(S). לולאת ההטמעה מתקדמת קדימה בלבד ללא חזרות, ולכן סך הגישות 
     * למערך חסום תמיד על ידי S.
     */
    public byte[] embed(byte[] fileBytes, String messageStr) throws Exception 
    {
        try 
        {
            Random seededRandom = new Random(SEED_HASH);
            
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
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(inputStream)) 
            {
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
            // שלב 3: הנמכה (Attenuation)
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

            // ==========================================
            // חומת המגן - Fail-Fast Validation
            // חישוב התרחיש הגרוע ביותר: כל ביט לוקח פריסה + דילוג מקסימלי
            int maxRequiredSamples = bitsToEmbed.length * (SAMPLES_PER_BIT + MAX_SKIP);

            int skipIndex = (int) originalFormat.getSampleRate() * originalFormat.getChannels();
            int totalUsableSamples = samples.length - skipIndex;
            int maxTotalBits = totalUsableSamples / SAMPLES_PER_BIT;
            int maxMessageBytes = (maxTotalBits - 16) / 8; // מחסירים 16 ביטים של כותרת ומחלקים ב-8

            if (maxRequiredSamples > samples.length) 
            {
                if (maxMessageBytes <= 0) 
                    throw new IllegalArgumentException("קובץ השמע קצר מדי ולא יכול להכיל מסר. אנא העלה קובץ ארוך יותר");
                else 
                    throw new IllegalArgumentException("הקובץ הנוכחי יכול להכיל מקסימום " + maxMessageBytes + " תווים, אך המסר שלך דורש " + messageLength);
                    // עוצר את התוכנית מיד, לפני שעשינו אפילו פעולה מתמטית אחת
            }
            // ==========================================

            System.out.println("Message length: " + messageLength + " bytes");
            System.out.println("Total bits to embed: " + bitsToEmbed.length);

            // ---------------------------------------------------------
            // שלב 5: מודולציה והטמעה (Embedding Loop)
            // ---------------------------------------------------------
            
            // הפקת רצף DSSS פסאודו-אקראי בגודל קובץ השמע
            // יצירת רעש מיסוך
            int[] pnSequence = DsssUtils.generatePnSequence(SECRET_SEED, samples.length);
            
            //פוינטר לשמע
            int sampleIndex = 0; 

            // פונינטר למסר
            int msgIndex;
            
            // לולאה ראשית אשר עוברת על כל הקובץ
            for (msgIndex = 0; msgIndex < bitsToEmbed.length; msgIndex++)
            {
                // וידוא כי נותרה קיבולת מספקת בקובץ להטמעת הסיבית הנוכחית
                if (sampleIndex + SAMPLES_PER_BIT + MAX_SKIP >= samples.length)                
                {
                    System.out.println("Error: Audio capacity too small for this message!");
                    return null;
                }

                // --- חישוב מקדם עוצמת הטמעה אדפטיבית (Local Alpha) ---
                // סוכם את כל הבלוק פריסה 
                // לולאה שעוברת על כל הקטע המיועד להטמעה וסוכמת אותו 
                // כדי לחשב ממוצע עוצמת סאונד בקטע זה
                long localSum = 0;
                for (int i = 0; i < SAMPLES_PER_BIT; i++) 
                {
                    localSum += Math.abs(samples[sampleIndex + i]);
                }

                // חישוב הממוצע בבלוק הפריסה הזה
                double localAvg = localSum / (double) SAMPLES_PER_BIT;
                
                // קביעת מקדם אלפא ל-21% מהעוצמה הממוצעת המקומית
                double localAlpha = localAvg * 0.21; 
                
                // חסימת ערכי קיצון למקדם האלפא (Lower & Upper bounds)
                if (localAlpha < 150) localAlpha = 150;
                if (localAlpha > 3500) localAlpha = 3500;

                // המרת הסיבית לייצוג ביפולרי (+1 או -1) עבור המתמטיקה של DSSS
                int bitToEmbed = bitsToEmbed[msgIndex];
                int bipolarBit = (bitToEmbed == 1) ? 1 : -1;

                // פיזור (Spread) והטמעת הביטים על פני בלוק דגימות השמע
                for (int i = 0; i < SAMPLES_PER_BIT; i++) 
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
                // חישוב וביצוע הדילוג לקראת הביט הבא
                int seededSkip = seededRandom.nextInt(MIN_SKIP, MAX_SKIP);
                //שימוש בפונקצייה המעבירה את המספר שהתקבל מהזרע חישובים חשבוניים ומוציאה מספר חדש
                //שהוא בעצם יהיה הדילוג שלנו 
                
                sampleIndex += seededSkip;

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
        catch (IllegalArgumentException e) 
        {
            // אלו שגיאות הלוגיקה והולידציה שלנו (כמו קובץ קטן מדי) - נעביר אותן בדיוק כמו שהן
            throw e;
        }
        catch (Exception e) 
        {
            // אלו שגיאות מערכת לא צפויות (קריסת זיכרון, קובץ פגום, NullPointer) - נעטוף אותן
            throw new Exception("שגיאה: " + e.getMessage(), e);
        }
    }

    /**
     * פונקציית חילוץ עיוור (Blind Extraction) המפענחת את המסר מתוך האודיו הנגוע.
     * הפונקציה משחזרת את רצף ה-PN והדילוגים האקראיים (Skips) בעזרת אותו מפתח Seed ששימש להטמעה.
     * היא מחלצת תחילה את אורך המסר ולאחר מכן את המסר עצמו על ידי נירמול מקומי (Zero-Mean) 
     * וביצוע מכפלה פנימית (קורלציה) עם רצף הרעש.
     *
     * @param fileBytes נתוני קובץ השמע המוצפן (Stego-Audio).
     * @return מחרוזת טקסט המכילה את המסר הסודי שפוענח.
     * @throws IllegalArgumentException אם האורך שחולץ אינו הגיוני (מה שמעיד על העדר מסר או רעש קיצוני).
     * @throws Exception במקרה של שגיאת מערכת לא צפויה.
     *
     * זמן ריצה (Time Complexity) במקרה הגרוע (Worst Case):
     * O(S)
     * בדומה להטמעה, הלולאות לחילוץ האורך והמסר מתקדמות על פני המערך במעבר רציף (Single Pass) 
     * ולכן הפעולה מתבצעת בזמן ליניארי לחלוטין ביחס לגודל קובץ השמע (מספר הדגימות S).
     */
    public String extract(byte[] fileBytes) throws Exception 
    {
        try 
        {
            Random seededRandom = new Random(SEED_HASH);
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
            int[] pnSequence = DsssUtils.generatePnSequence(SECRET_SEED, samples.length);
            
            //פוינטר לשמע
            int sampleIndex = 0;

            // ---------------------------------------------------------
            // שלב 2: חילוץ אורך המסר (HEADER_BITS ) בעזרת נירמול מקומי
            // ---------------------------------------------------------

            // פונינטר למסר
            int messageLength = 0;
            
            for (int i = 0; i < HEADER_BITS; i++) 
            {
                if (sampleIndex + SAMPLES_PER_BIT >= samples.length) break;

                // חישוב רכיב DC המקומי (ממוצע) לצורך Zero-Mean Normalization
                double blockSum = 0;
                for (int j = 0; j < SAMPLES_PER_BIT; j++) 
                {
                    blockSum += samples[sampleIndex + j];
                }
                double blockMean = blockSum / SAMPLES_PER_BIT;

                double correlation = 0;
                
                // ביצוע מכפלה פנימית (קורלציה) על בלוק הדגימות המנורמל
                for (int j = 0; j < SAMPLES_PER_BIT; j++) 
                {
                    double normalizedSample = samples[sampleIndex] - blockMean;
                    correlation += normalizedSample * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                // קבלת החלטה לוגית: ערך חיובי מעיד על '1', ערך שלילי מעיד על '0'
                correlation /= SAMPLES_PER_BIT;
                int bit = (correlation > 0) ? 1 : 0;
                
                // ביצוע שיפט לוגי (Bitwise OR) לבניית הערך השלם המייצג את האורך
                messageLength = (messageLength << 1) | bit;

                // דילוג אחרי קריאת כל ביט של כותרת
                int seededSkip = seededRandom.nextInt(MIN_SKIP, MAX_SKIP);
                //שימוש בפונקצייה המעבירה את המספר שהתקבל מהזרע חישובים חשבוניים ומוציאה מספר חדש
                //שהוא בעצם יהיה הדילוג שלנו 
                sampleIndex += seededSkip;
            }

            System.out.println("Detected message length: " + messageLength + " bytes");

            // ולידציה של אורך המסר כנגד קיבולת הקובץ
            int maxPossibleBytes = (samples.length) / (SAMPLES_PER_BIT + MIN_SKIP) / 8;

            if (messageLength <= 0 || messageLength > maxPossibleBytes) 
                throw new IllegalArgumentException("הקובץ פגום / ללא מסר");            

            // ---------------------------------------------------------
            // שלב 3: חילוץ נתוני המסר (Payload)
            // ---------------------------------------------------------
            int totalBitsToExtract = messageLength * 8;
            int[] extractedBits = new int[totalBitsToExtract];
            int msgIndex = 0;

            System.out.println("Extracting message...");

            // חזרה על מנגנון הקורלציה והנירמול לצורך חילוץ הסיביות הנותרות
            //
            while (msgIndex < totalBitsToExtract) 
            {
                if (sampleIndex + SAMPLES_PER_BIT >= samples.length) break;

                double blockSum = 0;
                for (int j = 0; j < SAMPLES_PER_BIT; j++) 
                {
                    blockSum += samples[sampleIndex + j];
                }
                double blockMean = blockSum / SAMPLES_PER_BIT;

                double correlation = 0;
                
                for (int i = 0; i < SAMPLES_PER_BIT; i++) 
                {
                    if (sampleIndex >= samples.length) break; 
                    double normalizedSample = samples[sampleIndex] - blockMean;
                    correlation += normalizedSample * pnSequence[sampleIndex];
                    sampleIndex++;
                }
                
                correlation /= SAMPLES_PER_BIT;
                
                extractedBits[msgIndex] = (correlation > 0) ? 1 : 0;
                msgIndex++;

                // דילוג אחרי קריאת כל ביט של המסר
                int seededSkip = seededRandom.nextInt(MIN_SKIP, MAX_SKIP);
                //שימוש בפונקצייה המעבירה את המספר שהתקבל מהזרע חישובים חשבוניים ומוציאה מספר חדש
                //שהוא בעצם יהיה הדילוג שלנו 
                sampleIndex += seededSkip;
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
        catch (IllegalArgumentException e) 
        {
            // שגיאות לוגיקה שיזמנו - נזרוק אותן הלאה
            throw e;
        }
        catch (Exception e) 
        {
            // שגיאות מערכת לא צפויות
            throw new Exception("שגיאה: " + e.getMessage(), e);
        }
    }
}