package yeonatano.steganography_system.utilities;

public class F5Utility 
{

    /**
     * מקבלת את אורך המסר (בבתים) וממירה אותו למערך של 32 ביטים (Header).
     * המערך הזה יוטמע בתחילת התמונה כדי שאלגוריתם החילוץ ידע כמה ביטים לקרוא.
     *
     * @param length אורך המסר בבתים
     * @return מערך בגודל 32 המכיל את ייצוג הביטים של האורך (מ-MSB ל-LSB)
     */
    public static int[] getHeaderBits(int length)
    {
        int[] headerBits = new int[32];
    
        for (int i = 0; i < 32; i++)
        {
            headerBits[i] = (length >> (31 - i)) & 1;
        }
        return headerBits;
    }

    /**
     * ממירה את מערך הבתים של המסר (ההודעה עצמה) למערך של ביטים (0 ו-1).
     * הפונקציה מפרקת כל בית (byte) במסר ל-8 ביטים.
     *
     * @param message מערך הבתים של המסר להטמעה
     * @return מערך ביטים המייצג את המסר בלבד (ללא ה-Header)
     */
    public static int[] bytesToBits(byte[] message)
    {
        int length = message.length;
        int totalBits = length * 8;
        int[] messageBits = new int[totalBits];
        
        for (int i = 0; i < length; i++)
        {
            int val = message[i];
            // פירוק כל בית ל-8 ביטים
            for (int j = 0; j < 8; j++)
            {
                messageBits[(i * 8) + j] = (val >> (7 - j)) & 1;
            }
        }
        return messageBits;
    }

    /**
     * משרשרת שני מערכי ביטים למערך אחד רציף.
     * משמשת לחיבור ה-Header (אורך המסר) והמסר עצמו למערך סופי אחד המיועד להטמעה.
     *
     * @param headerBits מערך הביטים של ה-Header (בדרך כלל 32 ביטים)
     * @param messageBits מערך הביטים של המסר
     * @return מערך משולב המכיל את ה-Header ואחריו את המסר
     */
    public static int[] concatArrays(int[] headerBits, int[] messageBits)
    {
        int totalBits = headerBits.length + messageBits.length;
        int[] combinedBits = new int[totalBits];
        
        // העתקת ה-Header לתחילת המערך המשולב
        System.arraycopy(headerBits, 0, combinedBits, 0, headerBits.length);
        
        // העתקת המסר מיד אחרי ה-Header
        System.arraycopy(messageBits, 0, combinedBits, headerBits.length, messageBits.length);
        
        return combinedBits;
    }

    /**
     * מחלצת את הביט הכי פחות משמעותי (LSB - Least Significant Bit) מתוך ערך נתון.
     * משתמשת בערך המוחלט כדי למנוע בעיות עם מספרים שליליים.
     *
     * @param value מקדם ה-DCT שממנו מחלצים את הביט
     * @return 0 או 1 המייצגים את ה-LSB של הערך
     */
    public static int getLSB(int value)
    {
        return Math.abs(value) % 2;
    }

    /**
     * משנה את המקדם כך שה-LSB שלו יתאים לביט היעד (Target Bit).
     * מיישמת את אלגוריתם F5 (Shrinkage): תמיד מקטינה את הערך המוחלט של המקדם (מקרבת ל-0).
     *
     * @param value הערך המקורי של מקדם ה-DCT
     * @param targetBit הביט שאנחנו רוצים שיופיע ב-LSB (0 או 1)
     * @return הערך החדש של המקדם לאחר השינוי
     */
    public static int setLSB(int value, int targetBit)
    {
        if (value == 0) return 0;
        
        // אם ה-LSB כבר תואם - אין צורך בשינוי
        if (getLSB(value) == targetBit) return value;

        // אלגוריתם F5: הקטנת הערך המוחלט (התקרבות לאפס)
        if (value > 0) return value - 1; // ערך חיובי מוקטן ב-1

        else return value + 1; // ערך שלילי מוגדל ב-1 (מתקרב ל-0)
        
    }

    /**
     * ממירה מערך של ביטים (0 ו-1) חזרה למערך של בתים (bytes).
     * עוברת על כל 8 ביטים ודוחפת אותם לתוך משתנה בודד כדי ליצור בית שלם.
     *
     * @param bits מערך הביטים שחולץ (ללא ה-Header)
     * @return מערך הבתים המייצג את המסר המקורי שניתן להמיר לטקסט
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
                // דחיפת הביט הנוכחי פנימה מצד ימין
                currentByte = (currentByte << 1) | bits[(i * 8) + j];
            }
            messageBytes[i] = (byte) currentByte;
        }
        
        return messageBytes;
    }
}