package yeonatano.steganography_system.utilities;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * מחלקת עזר (Utility Class) עבור אלגוריתם הסטגנוגרפיה PVD.
 * מרכזת את כל הלוגיקה המתמטית של חישובי הטווחים, פעולות בינאריות (Bitwise),
 * וקריאה/כתיבה בטוחה של פיקסלים מתוך התמונה.
 */
public class PvdUtils {

    // ========== הגבלות מערכת ==========
    
    // הגבלת מימדי התמונה כדי למנוע קריסת זיכרון (OutOfMemoryError) בשרת.
    // תמונות ענקיות ידרשו עיבוד ארוך מאוד ומערכי זיכרון עצומים.
    public static final int MAX_IMAGE_WIDTH = 2000;
    public static final int MAX_IMAGE_HEIGHT = 2000;
    
    // מספר הביטים שנקצה בתחילת התמונה כדי לשמור את אורך המסר.
    // 16 ביטים מאפשרים לנו לשמור מספרים עד 65,535 (כלומר הודעה באורך של כ-65 קילו-בייט).
    public static final int MESSAGE_LENGTH_BITS = 16;

    // ========== טבלת הטווחים (Quantization Range Table) ==========
    
    /**
     * לב האלגוריתם של PVD.
     * האלגוריתם מסתמך על העובדה שהעין האנושית פחות רגישה לשינויים באזורים "רועשים" בתמונה 
     * (היכן שההפרש בין שני פיקסלים סמוכים הוא גדול) לעומת אזורים חלקים.
     * 
     * חוקיות מתמטית קריטית: אם אנחנו רוצים להחביא n ביטים, הרוחב של הטווח חייב להיות בדיוק 2 בחזקת n.
     * (לדוגמה: כדי להחביא 3 ביטים, הטווח חייב לכלול 8 מספרים).
     * 
     * מבנה כל שורה במערך: {גבול תחתון, גבול עליון, מספר הביטים להטמעה (n)}
     */
    private static final int[][] RANGE_TABLE = 
    {
            // MIN  MAX  BITS (n)
            {   0,   7,   3 },  // טווח של 8 ערכים (0 עד 7) -> יכול להכיל 3 ביטים (2^3 = 8)
            {   8,  15,   3 },  // טווח של 8 ערכים (8 עד 15) -> 3 ביטים
            {  16,  31,   4 },  // טווח של 16 ערכים (16 עד 31) -> יכול להכיל 4 ביטים (2^4 = 16)
            {  32,  63,   5 },  // טווח של 32 ערכים -> 5 ביטים
            {  64, 127,   6 },  // טווח של 64 ערכים -> 6 ביטים
            { 128, 255,   7 }   // טווח של 128 ערכים -> 7 ביטים (האזור הכי רועש, מכיל הכי הרבה מידע)
    };

    /**
     * פונקציה המקבלת את ההפרש המוחלט בין שני פיקסלים ומחזירה באיזה אינדקס (שורה) בטבלה הוא נמצא.
     */
    public static int getRangeIndex(int absDifference) 
    {
        for (int i = 0; i < RANGE_TABLE.length; i++) 
        {
            // בודק אם ההפרש נופל בין הגבול התחתון לעליון של השורה הנוכחית
            if (absDifference >= RANGE_TABLE[i][0] && absDifference <= RANGE_TABLE[i][1])
                return i;
    
        }
        return RANGE_TABLE.length - 1; // מקרה קצה אבטחתי (Fallback)
    }

    /**
     * מקבלת אינדקס של שורה בטבלה ומחזירה את הגבול התחתון (MIN) שלה.
     * משמשת בשלב ההטמעה כדי לחשב את ההפרש החדש (MIN + הערך העשרוני של הביטים).
     */
    public static int getRangeStart(int rangeIndex) 
    {
        return RANGE_TABLE[rangeIndex][0];
    }

    /**
     * מקבלת אינדקס של שורה ומחזירה כמה ביטים (Capacity) מותר לנו להחביא בזוג הפיקסלים הזה.
     */
    public static int getCapacity(int rangeIndex) 
    {
        return RANGE_TABLE[rangeIndex][2];
    }

    // ========== מניפולציה על פיקסלים (Bitwise Image Operations) ==========

    /**
     * חילוץ הערוץ הכחול (Blue) מתוך פיקסל.
     * ב-Java, צבע של פיקסל (ARGB) מיוצג על ידי מספר שלם (int) בן 32 ביטים:
     * 8 ביטים ל-Alpha (שקיפות), 8 לאדום, 8 לירוק, ו-8 אחרונים לכחול.
     * הפעולה & 0xFF (AND בינארי עם 255) מאפסת את כל ה-24 ביטים הראשונים ומשאירה רק את ה-8 האחרונים (הכחול).
     */
    public static int getBlueValue(BufferedImage image, int x, int y) 
    {
        int rgb = image.getRGB(x, y);
        return rgb & 0xFF;
    }

    /**
     * הטמעת ערך כחול (Blue) חדש אל תוך הפיקסל, מבלי להרוס את שאר הצבעים.
     * זוהי פעולה קריטית! אם נדרוס את כל ה-int, הפיקסל יהפוך לשחור או שקוף והתמונה תיהרס.
     */
    public static void setBlueValue(BufferedImage image, int x, int y, int newBlue) 
    {
        // הגנה: מוודאים שערך הצבע לא גולש מתחת ל-0 או מעל ל-255
        newBlue = Math.max(0, Math.min(255, newBlue)); 
        
        // קריאת הפיקסל המקורי
        int rgb = image.getRGB(x, y);

        // חילוץ הערוצים האחרים באמצעות הזזה בינארית (Bit Shift) ימינה (>>) ומסכת 0xFF
        int alpha = (rgb >> 24) & 0xFF; // הזזה של 24 מקומות ימינה מביאה את השקיפות לסוף
        int red = (rgb >> 16) & 0xFF;   // הזזה של 16 מקומות מביאה את האדום לסוף
        int green = (rgb >> 8) & 0xFF;  // הזזה של 8 מקומות מביאה את הירוק לסוף

        // הרכבת הפיקסל מחדש: מזיזים כל צבע חזרה שמאלה (<<) למקומו, 
        // ומחברים אותם יחד עם פעולת OR בינארית (|) יחד עם הכחול החדש שלנו.
        int newRgb = (alpha << 24) | (red << 16) | (green << 8) | newBlue;
        
        image.setRGB(x, y, newRgb);
    }

    /**
     * מוודאת שהתמונה אינה חורגת מהמידות המקסימליות המותרות.
     */
    public static boolean isImageSizeValid(BufferedImage image) 
    {
        return image.getWidth() <= MAX_IMAGE_WIDTH && image.getHeight() <= MAX_IMAGE_HEIGHT;
    }

    // ========== המרות בינאריות (BitSet Conversions) ==========

    /**
     * ממירה מספר שלם (כמו אורך המסר) למבנה נתונים מסוג BitSet (מערך ביטים חכם).
     */
    public static BitSet valueToBits(int value, int numBits) 
    {
        return BitSet.valueOf(new long[] { Integer.toUnsignedLong(value) });
    }

    /**
     * פעולה הפוכה: ממירה BitSet בחזרה למספר שלם. משמש לחילוץ אורך המסר מהתמונה.
     */
    public static int bitsToValue(BitSet bits, int numBits) 
    {
        long[] longs = bits.toLongArray();
        if (longs.length == 0) return 0; // אם אין ביטים דלוקים, הערך הוא 0
        return (int) longs[0];
    }

    /**
     * ממירה מחרוזת טקסט למערך של ביטים (BitSet).
     * שימוש ב-UTF_8 קריטי כדי לתמוך בשפות כמו עברית או סימנים מיוחדים.
     */
    public static BitSet textToBits(String text) 
    {
        return BitSet.valueOf(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * ממירה את הביטים שחולצו מהתמונה בחזרה לטקסט קריא.
     * 
     * @param bits הביטים שחולצו
     * @param numBytes מספר הבייטים המקורי (שחולץ קודם לכן מ-16 הביטים הראשונים)
     * @return הטקסט המקורי
     */
    public static String bitsToText(BitSet bits, int numBytes) 
    {
        // מתודת toByteArray של ג'אווה חותכת אוטומטית "אפסים מובילים" (אפסים בסוף המערך).
        // כדי שהטקסט לא ייחתך או ייהרס בסוף (מה שיוצר ג'יבריש), אנחנו מוודאים שהמערך 
        // המומר חוזר בדיוק לאורך המקורי שלו בעזרת ריפוד מחדש (Padding) של אפסים במידת הצורך.
        byte[] bytes = bits.toByteArray();
        
        if (bytes.length < numBytes) 
        {
            byte[] paddedBytes = new byte[numBytes];
            // מעתיקים את הבייטים הקיימים למערך החדש (השאר יישארו 0 כברירת מחדל)
            System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);
            return new String(paddedBytes, StandardCharsets.UTF_8);
        }
        
        return new String(bytes, StandardCharsets.UTF_8);
    }
}