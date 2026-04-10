package yeonatano.steganography_system.utilities;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Random;

/**
 * מחלקת תשתית וכלים עבור אלגוריתם DSSS.
 * מכילה פונקציות עזר להמרת נתונים לביטים, קריאה ושמירה של קבצי WAV,
 * ויצירת מפתח הפריסה (PN Sequence). פונקציות אלו משמשות גם את המטמיע וגם את המחלץ.
 */
public class DsssUtils 
{
    /**
     * קבוע: מספר הדגימות שעליהן מפוזר כל ביט בודד.
     * ככל שהמספר גדול יותר - ההטמעה עמידה יותר לרעש, אך קיבולת הקובץ קטנה.
     */
    public static final int SAMPLES_PER_BIT = 32;

    /**
     * מחלקת מעטפת (Wrapper) לאחסון דגימות האודיו יחד עם הפורמט המקורי שלהן.
     * חשוב כדי לשמור את תדר הדגימה (Sample Rate) ומספר הערוצים לזמן השמירה.
     */
    public static class AudioData 
    {
        public short[] samples;
        public AudioFormat format;

        public AudioData(short[] samples, AudioFormat format) 
        {
            this.samples = samples;
            this.format = format;
        }
    }

    // ==========================================
    // פונקציות המרה ועבודה עם ביטים
    // ==========================================

    /**
     * ממירה את אורך המסר (מספר שלם) למערך של 16 ביטים.
     * ה-16 ביטים הללו יוטמעו בתחילת הקובץ כדי שהמחלץ ידע כמה לקרוא.
     * 16 ביטים מאפשרים אורך הודעה מקסימלי של 65,535 תווים.
     * * @param length אורך ההודעה בבתים
     * @return מערך בגודל 16 המכיל 0 ו-1 (מ-MSB ל-LSB)
     */
    public static int[] getHeaderBits(int length)
    {
        int[] headerBits = new int[16];
        for (int i = 0; i < 16; i++)
        {
            // דחיפת הביט ה-i למקום הימני ביותר, וסינון עם 1 (כדי לקבל רק 0 או 1)
            headerBits[i] = (length >> (15 - i)) & 1;
            //בשלב הראשון הביט שיכנס הוא הביט המשמעותי 
        }
        return headerBits;
    }

    /**
     * ממירה מערך של בתים (Bytes) למערך ארוך של ביטים (Bits).
     * מפרקת כל תו/בית של ההודעה ל-8 ביטים.
     * * @param message מערך הבתים של ההודעה
     * @return מערך המכיל רצף של 0 ו-1
     */
    public static int[] bytesToBits(byte[] message)
    {
        int length = message.length;
        int totalBits = length * 8;
        int[] messageBits = new int[totalBits];
        
        // על הבתים
        for (int i = 0; i < length; i++)
        {
            int val = message[i];
            //על הביטים בכל בית
            for (int j = 0; j < 8; j++)
            {
                // קורא את הביט הספציפי מתוך הבית הנוכחי
                messageBits[(i * 8) + j] = (val >> (7 - j)) & 1;
    //מיקום באיזה בית ואיזה ביט אנו נמצאים
            }
        }
        return messageBits;
    }

    /**
     * מחברת שני מערכי ביטים (את ה-Header ואת ההודעה עצמה) לרכבת אחת ארוכה.
     * * @param headerBits ביטי האורך
     * @param messageBits ביטי ההודעה
     * @return מערך משולב שמוכן להטמעה
     */
    public static int[] concatArrays(int[] headerBits, int[] messageBits)
    {
        int totalBits = headerBits.length + messageBits.length;
        int[] combinedBits = new int[totalBits];
        
        System.arraycopy(headerBits, 0, combinedBits, 0, headerBits.length);
        System.arraycopy(messageBits, 0, combinedBits, headerBits.length, messageBits.length);
        
        return combinedBits;
    }

    /**
     * ממירה מערך של ביטים (0 ו-1) חזרה למערך של בתים קריאים.
     * * @param bits מערך הביטים שחולץ (ללא ה-Header)
     * @return מערך הבתים של ההודעה המקורית
     */
    public static byte[] bitsToBytes(int[] bits) 
    {
        int length = bits.length / 8;
        byte[] messageBytes = new byte[length];
        
        for (int i = 0; i < length; i++) 
        {
            int currentByte = 0;
            for (int j = 0; j < 8; j++) 
            {
                // מזיז את המשתנה שמאלה כדי לפנות מקום, ומשלב פנימה את הביט החדש
                currentByte = (currentByte << 1) | bits[(i * 8) + j];
            }
            messageBytes[i] = (byte) currentByte;
        }
        
        return messageBytes;
    }

    // ==========================================
    // פעולות DSSS "שחורות" (קריאה/כתיבה וחישובים)
    // ==========================================

    /**
     * קוראת קובץ WAV מן הדיסק וממירה אותו למערך של דגימות 16-bit.
     * * @param filePath נתיב הקובץ
     * @return אובייקט AudioData המכיל את הדגימות והפורמט
     */
    public static AudioData readWavSamples(String filePath) throws UnsupportedAudioFileException, IOException 
    {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found!");

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) 
        {
            AudioFormat format = ais.getFormat();
            // DSSS בנוי לעבוד מתמטית על דגימות של 16-bit
            if (format.getSampleSizeInBits() != 16) throw new IllegalArgumentException("16-bit only");

            byte[] buffer = ais.readAllBytes();

                                            // כיוון שהוא מכיל 16 ביט
            short[] samples = new short[buffer.length / 2];
            
            // המרת כל זוג בתים (Little Endian) לדגימת short אחת
            for (int i = 0; i < samples.length; i++) 
            {
                int baseIdx = i * 2; // נקודת ההתחלה של הדגימה הנוכחית
                
                //שליפת שני החלקים
                int lowByte  = buffer[baseIdx] & 0xFF;
                int highByte = buffer[baseIdx + 1] << 8;
                
                // חיבורם לדגימה אחת
                samples[i] = (short) (lowByte | highByte);
                //לדוגמה
                //   10101010 00000000  (החלק העליון)
                // | 00000000 00001111  (החלק התחתון)
                // -------------------
                //   10101010 00001111  (התוצאה הסופית)
            }
            return new AudioData(samples, format);
        }
    }

    /**
     * שומרת את מערך הדגימות בחזרה לקובץ WAV תקין.
     * כאן אנחנו מבצעים את הפעולה ההפוכה: מפרקים כל מספר (short) חזרה לשני בתים (bytes).
     */
    public static void saveWavFile(String filePath, AudioData audioData) throws IOException 
    {
        // יצירת מערך בתים שיכיל את המידע הגולמי (כל דגימה תופסת 2 בתים)
        byte[] buffer = new byte[audioData.samples.length * 2];
        
        // לולאה שעוברת על כל דגימת סאונד ומפרקת אותה
        // המרת כל דגימת short לשני בתים נפרדים
        for (int i = 0; i < audioData.samples.length; i++) 
        {
            int baseIdx = i * 2;             // המיקום הנוכחי במערך הבתים (Buffer)
            short currentSample = audioData.samples[i]; // הדגימה שאנחנו מפרקים עכשיו

            // פירוק הדגימה לשני "חצאי לבנים" (בתים)
            byte lowByte  = (byte) (currentSample & 0xFF);  // לוקחים את 8 הביטים התחתונים
            byte highByte = (byte) (currentSample >> 8);    // לוקחים את 8 הביטים העליונים

            // השמה למערך הבתים (לפי סדר Little Endian - הקטן קודם)
            buffer[baseIdx]     = lowByte;
            buffer[baseIdx + 1] = highByte;
        }

        // הפיכת מערך הבתים ל"זרם נתונים" כדי ש-Java תוכל לעבוד איתו
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        
        // חישוב אורך ה"פריימים" (כמה דגימות יש בכל ערוץ בנפרד)
        int frameLength = audioData.samples.length / audioData.format.getChannels();
        
        // יצירת אובייקט AudioInputStream שמאגד את המידע יחד עם הפורמט המקורי
        AudioInputStream ais = new AudioInputStream(bais, audioData.format, frameLength);

        // כתיבת הקובץ הסופי לדיסק כקובץ WAV
        File outputFile = new File(filePath);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
    }

    /**
     * מייצרת רצף פסאודו-אקראי (PN Sequence) המורכב מ-1 ו-(1-) בלבד.
     * הרצף דטרמיניסטי ותלוי לחלוטין בסיסמה, כך שהמחלץ יוכל ליצור אותו במדויק.
     * * @param password הסיסמה הסודית
     * @param length אורך הרצף הנדרש (בדרך כלל כאורך קובץ האודיו)
     * @return מערך PN
     */
    public static int[] generatePnSequence(String password, int length) 
    {
        long seed = password.hashCode();
        Random random = new Random(seed);
        int[] pnSequence = new int[length];
        
        for (int i = 0; i < length; i++) 
            pnSequence[i] = random.nextBoolean() ? 1 : -1;
        
        return pnSequence;
    }

    // ==========================================
    // פונקציות חדשות לעבודה עם זרם נתונים (Memory Buffer / Streams)
    // ==========================================

    /**
     * קוראת נתוני שמע (WAV) מתוך זרם זיכרון (InputStream) במקום קובץ בדיסק.
     */
    public static AudioData readWavSamplesFromStream(InputStream is) throws UnsupportedAudioFileException, IOException 
    {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(is)) 
        {
            AudioFormat format = ais.getFormat();
            // DSSS בנוי לעבוד מתמטית על דגימות של 16-bit
            if (format.getSampleSizeInBits() != 16) throw new IllegalArgumentException("16-bit only");

            byte[] buffer = ais.readAllBytes();
            short[] samples = new short[buffer.length / 2];
            
            // המרת כל זוג בתים (Little Endian) לדגימת short אחת
            for (int i = 0; i < samples.length; i++) 
            {
                int baseIdx = i * 2;
                int lowByte  = buffer[baseIdx] & 0xFF;
                int highByte = buffer[baseIdx + 1] << 8;
                samples[i] = (short) (lowByte | highByte);
            }
            return new AudioData(samples, format);
        }
    }

    /**
     * שומרת את דגימות השמע (WAV) לתוך זרם זיכרון (OutputStream) במקום לקובץ בדיסק.
     */
    public static void saveWavFileToStream(OutputStream os, AudioData audioData) throws IOException 
    {
        byte[] buffer = new byte[audioData.samples.length * 2];
        
        for (int i = 0; i < audioData.samples.length; i++) 
        {
            int baseIdx = i * 2;
            short currentSample = audioData.samples[i];

            byte lowByte  = (byte) (currentSample & 0xFF);
            byte highByte = (byte) (currentSample >> 8);

            buffer[baseIdx]     = lowByte;
            buffer[baseIdx + 1] = highByte;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        int frameLength = audioData.samples.length / audioData.format.getChannels();
        AudioInputStream ais = new AudioInputStream(bais, audioData.format, frameLength);

        // במקום לכתוב ל-File, אנו כותבים ישירות ל-OutputStream שקיבלנו
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, os);
    }
}