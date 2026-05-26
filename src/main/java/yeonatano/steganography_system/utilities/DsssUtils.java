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
    public static final int SAMPLES_PER_BIT = 100;

    // ==========================================
    // פונקציות המרה ועבודה עם ביטים
    // ==========================================

    /**
     * ממירה את אורך המסר למערך של 16 ביטים (Header).
     * שישמש בתור ההדר של הקובץ המוטמע
     */
    public static int[] getHeaderBits(int length)
    {
        int[] headerBits = new int[16];
        for (int i = 0; i < 16; i++)
        {
            headerBits[i] = (length >> (15 - i)) & 1;
        }
        return headerBits;
    }

    /**
     * ממירה מערך של בתים (Bytes) למערך ארוך של ביטים (Bits).
     */
    public static int[] bytesToBits(byte[] message)
    {
        int length = message.length;
        int totalBits = length * 8;
        int[] messageBits = new int[totalBits];
        
        for (int i = 0; i < length; i++)
        {
            int val = message[i];
            for (int j = 0; j < 8; j++)
            {
                messageBits[(i * 8) + j] = (val >> (7 - j)) & 1;
            }
        }
        return messageBits;
    }

    /**
     * מחברת שני מערכי ביטים (את ה-Header ואת ההודעה עצמה) לרכבת אחת ארוכה.
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
                currentByte = (currentByte << 1) | bits[(i * 8) + j];
            }
            messageBytes[i] = (byte) currentByte;
        }
        
        return messageBytes;
    }

    // ==========================================
    // פונקציית יצירת מפתח PN
    // ==========================================

    /**
     * מייצרת רצף פסאודו-אקראי (PN Sequence) המורכב מ-1 ו-(1-) בלבד.
     */
    public static int[] generatePnSequence(String password, int length) 
    {
        long seed = password.hashCode();
        Random random = new Random(seed);
        int[] pnSequence = new int[length];
        
        for (int i = 0; i < length; i++) {
            pnSequence[i] = random.nextBoolean() ? 1 : -1;
        }
        
        return pnSequence;
    }

    // ==========================================
    // פעולות קריאה/כתיבה ללא AudioData (עבודה ישירה עם מערכי short)
    // ==========================================
   

    /**
     * קוראת נתוני שמע (WAV) מתוך זרם זיכרון (InputStream) ומחזירה מערך דגימות.
     */
    public static short[] readWavSamplesFromStream(InputStream is) throws UnsupportedAudioFileException, IOException 
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(is);
        
            if (ais.getFormat().getSampleSizeInBits() != 16) 
                throw new IllegalArgumentException("16-bit only");
            

            byte[] buffer = ais.readAllBytes();
            short[] samples = new short[buffer.length / 2];
            
            for (int i = 0; i < samples.length; i++) 
            {
                int baseIdx = i * 2;
                int lowByte  = buffer[baseIdx] & 0xFF;
                int highByte = buffer[baseIdx + 1] << 8;
                samples[i] = (short) (lowByte | highByte);
            }
            return samples;
    }

    /**
     * שומרת את מערך הדגימות לקובץ WAV בדיסק, לפי הפורמט שסופק.
     */
    public static void saveWavFile(String filePath, short[] samples, AudioFormat format) throws IOException 
    {
        byte[] buffer = new byte[samples.length * 2];
        
        for (int i = 0; i < samples.length; i++) 
        {
            int baseIdx = i * 2;
            short currentSample = samples[i];

            buffer[baseIdx]     = (byte) (currentSample & 0xFF);
            buffer[baseIdx + 1] = (byte) (currentSample >> 8);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        int frameLength = samples.length / format.getChannels();
        AudioInputStream ais = new AudioInputStream(bais, format, frameLength);

        File outputFile = new File(filePath);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
    }

    /**
     * שומרת את מערך הדגימות לתוך זרם זיכרון (OutputStream), לפי הפורמט שסופק.
     */
    public static void saveWavFileToStream(OutputStream os, short[] samples, AudioFormat format) throws IOException 
    {
        byte[] buffer = new byte[samples.length * 2];
        
        for (int i = 0; i < samples.length; i++) 
        {
            int baseIdx = i * 2;
            short currentSample = samples[i];

            buffer[baseIdx]     = (byte) (currentSample & 0xFF);
            buffer[baseIdx + 1] = (byte) (currentSample >> 8);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        int frameLength = samples.length / format.getChannels();
        AudioInputStream ais = new AudioInputStream(bais, format, frameLength);

        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, os);
    }
}