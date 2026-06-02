package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import yeonatano.steganography_system.utilities.PvdUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * שירות (Service) המיישם את אלגוריתם PVD (Pixel Value Differencing).
 * מחלקה זו מנוהלת על ידי Spring Boot ומאפשרת הזרקה (Dependency Injection) לשכבת הניתוב.
 * האלגוריתם מסתיר מידע על ידי שינוי ההפרשים בין זוגות פיקסלים סמוכים, תוך ניצול
 * חוסר הרגישות של העין האנושית לשינויים באזורים "רועשים" בתמונה.
 * * סיבוכיות מקום כוללת (Space Complexity): O(W * H) או O(N)
 * כאשר N הוא מספר הפיקסלים הכולל. נדרשת הקצאת זיכרון ליצירת עותק ARGB של התמונה בזיכרון (Buffer),
 * וכן מערכי בתים (Byte Arrays) לקריאה וכתיבה של התמונה.
 */
@Service
public class PVDStegnoService 
{
    /**
     * פונקציית ההטמעה (Embedding) המטמיעה מסר סודי בערוץ הכחול (Blue Channel) של התמונה.
     * * לוגיקת הפעולה:
     * 1. המרת התמונה לפלטת צבעים מלאה (ARGB) למניעת פגיעה באיכות מבוססת אינדקסים.
     * 2. אימות גודל וקיבולת מקסימלית של התמונה לאחסון המסר.
     * 3. הזרקת 16 ביטים המייצגים את אורך המסר (Header), ולאחריהם ביטי המסר.
     * 4. עדכון זוגות פיקסלים על בסיס טבלת טווחים (Quantization Table) והגנה מפני חריגות צבע.
     * * @param fileBytes מערך הבתים המכיל את התמונה המקורית (Cover Image) שהועלתה.
     * @param secretMessage המסר הסודי (Payload) להטמעה בקידוד UTF-8.
     * @return מערך בתים (byte[]) המייצג את התמונה המוצפנת (Stego-Image) בפורמט Lossless PNG.
     * @throws IllegalArgumentException (Fail-Fast) אם ממדי התמונה חורגים מהמותר או אם המסר חורג מהקיבולת.
     * @throws Exception במקרה של שגיאת המרה, פגם בקובץ או קריסת זיכרון.
     * * זמן ריצה (Time Complexity) במקרה הגרוע (Worst Case):
     * O(W * H) או O(N)
     * כאשר W הוא רוחב התמונה, H הוא הגובה, ו-N הוא סך הפיקסלים.
     * הלולאה החיצונית סורקת את השורות (H) והפנימית סורקת את העמודות בקפיצות של 2 (W/2).
     * במקרה שהמסר תופס את כל קיבולת התמונה, הלולאה תסרוק את כלל הפיקסלים (N/2 זוגות).
     * * סיבוכיות מקום (Space Complexity): O(N)
     * יצירת עותק `BufferedImage` חדש בפורמט ARGB דורשת זיכרון פרופורציונלי למספר הפיקסלים.
     */
    public byte[] embed(byte[] fileBytes, String secretMessage) throws Exception 
    {
        try
        {
            System.out.println("=== PVD Steganography - Embedding ===");

            // עטיפת מערך הבייטים ב-Stream לקריאה רציפה כקובץ תמונה
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            // המרת ייצוג התמונה לייצוג של פלטת צבעים רחבה (16 מיליון צבעים + שקיפות ARGB).
            // מונע בעיות גלישה או עיגול של צבעים בתמונות עם פלטה מוגבלת (Indexed Colors).
            BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            int width = image.getWidth();
            int height = image.getHeight();

            // הגנה על זיכרון השרת: מניעת עיבוד תמונות ענקיות (לדוגמה 4K)
            if (!PvdUtils.isImageSizeValid(image)) 
                throw new IllegalArgumentException("מידות התמונה חורגות מהמקסימום המותר");        

            // הכנת המסר הסודי וחישוב אורכו
            byte[] messageBytes = secretMessage.getBytes(StandardCharsets.UTF_8);
            int messageLengthBytes = messageBytes.length;

            // בדיקת קיבולת אדפטיבית מול התמונה הספציפית
            int totalRequiredBits = PvdUtils.MESSAGE_LENGTH_BITS + (messageLengthBytes * 8);
            long availableCapacityBits = PvdUtils.calculateMaxCapacityInBits(image);

            if (totalRequiredBits > availableCapacityBits) 
            {
                long maxPayloadBytes = (availableCapacityBits - PvdUtils.MESSAGE_LENGTH_BITS) / 8;
                throw new IllegalArgumentException(
                    String.format("המסר גדול מדי. הקיבולת המקסימלית לתמונה היא %d תווים, אך ניסית להכניס %d.", 
                    maxPayloadBytes, messageLengthBytes)
                );
            }

            // המרת הנתונים לשרשרת בינארית רציפה (BitSet)
            BitSet messageBits = PvdUtils.textToBits(secretMessage);
            BitSet lengthBits = PvdUtils.valueToBits(messageLengthBytes, PvdUtils.MESSAGE_LENGTH_BITS);

            BitSet totalBitsToHide = new BitSet();
            int currentBitIndex = 0;
            
            // דחיפת ביטי הכותרת (Header)
            for (int i = 0; i < PvdUtils.MESSAGE_LENGTH_BITS; i++)
            {
                if (lengthBits.get(i))
                    totalBitsToHide.set(currentBitIndex);
                currentBitIndex++;
            }
            
            // דחיפת ביטי המסר עצמו (Payload)
            for (int i = 0; i < messageLengthBytes * 8; i++) 
            {
                if (messageBits.get(i))
                    totalBitsToHide.set(currentBitIndex);
                currentBitIndex++;
            }

            int totalBitsLength = currentBitIndex;
            int bitPointer = 0; 

            // ריצה על התמונה ברמת פיקסלים והטמעת המסר הסודי
            MainLoop:
            for (int y = 0; y < height; y++) 
            {
                // קפיצות בזוגות, כי האלגוריתם מחשב הפרשים (Differencing) בין שני פיקסלים סמוכים
                for (int x = 0; x < width - 1; x += 2) 
                {
                    // תנאי עצירה: כל הביטים הוצפנו
                    if (bitPointer >= totalBitsLength) 
                        break MainLoop; 

                    int p1 = PvdUtils.getBlueValue(image, x, y);
                    int p2 = PvdUtils.getBlueValue(image, x + 1, y);

                    // חישוב ההפרש המוחלט הנוכחי (d)
                    int d = Math.abs(p2 - p1);
                    
                    // מציאת טווח הקוונטיזציה (Quantization Range) הרלוונטי ואת הקיבולת שלו בביטים
                    int rangeIndex = PvdUtils.getRangeIndex(d);
                    int capacity = PvdUtils.getCapacity(rangeIndex);

                    // משיכת כמות הביטים המותרת והמרתם לערך עשרוני (b)
                    int b = 0;
                    for (int i = 0; i < capacity; i++) 
                    {
                        if (bitPointer < totalBitsLength && totalBitsToHide.get(bitPointer))
                            b |= (1 << i); 
                        
                        bitPointer++;
                    }

                    // בניית ההפרש המוצפן החדש
                    int l = PvdUtils.getRangeStart(rangeIndex);
                    int newAbsD = l + b;

                    // חלוקת שגיאת ההפרש (error) באופן שווה למזעור פגיעה ויזואלית (Artifacts)
                    int error = newAbsD - d;
                    int errorUp = (int) Math.ceil(error / 2.0);
                    int errorDown = error - errorUp;

                    int newP1, newP2;

                    // עדכון ערכי הפיקסלים תוך שמירה על יחסי הסדר ביניהם
                    if (p1 >= p2) 
                    {
                        newP1 = p1 + errorUp;
                        newP2 = p2 - errorDown;
                    } 
                    else 
                    {
                        newP1 = p1 - errorUp;
                        newP2 = p2 + errorDown;
                    }

                    // מנגנון חלוקת עומס (Elevator Logic) למניעת חריגות מטווח הצבעים (0-255)
                    if (newP1 > 255) 
                    {
                        int overflow = newP1 - 255;
                        newP1 -= overflow;
                        newP2 -= overflow;
                    } 
                    else if (newP1 < 0) 
                    {
                        int underflow = 0 - newP1;
                        newP1 += underflow;
                        newP2 += underflow;
                    }

                    if (newP2 > 255) 
                    {
                        int overflow = newP2 - 255;
                        newP1 -= overflow;
                        newP2 -= overflow;
                    } 
                    else if (newP2 < 0) 
                    {
                        int underflow = 0 - newP2;
                        newP1 += underflow;
                        newP2 += underflow;
                    }
                    
                    // שמירת הפיקסלים המוצפנים בתמונה
                    PvdUtils.setBlueValue(image, x, y, newP1);
                    PvdUtils.setBlueValue(image, x + 1, y, newP2);
                }
            }
            
            if (bitPointer < totalBitsLength) 
                throw new Exception("שגיאה בהטמעה: חריגה בלתי צפויה מקיבולת התמונה במהלך ההטמעה.");

            System.out.println("Message embedded successfully! Total bits hidden: " + bitPointer);

            // המרה חזרה למערך בייטים בפורמט PNG השומר על שלמות הפיקסלים (Lossless)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            
            return baos.toByteArray();
        } 
        catch (IllegalArgumentException e) 
        {
            throw e; // שגיאות ולידציה יזומות (מועברות ל-UI כפי שהן)
        } 
        catch (Exception e) 
        {
            throw new Exception("שגיאת מערכת במהלך ההטמעה (PVD): " + e.getMessage(), e);
        }
    }

    /**
     * פונקציית החילוץ (Extraction) המחלצת מסר סודי מתוך תמונת ה-Stego.
     * הלוגיקה מבוססת על סריקת זוגות פיקסלים, חישוב ההפרש (d), חילוץ הטווח
     * ושליפת שארית הביטים שסופחו לגבול התחתון (l).
     * * @param fileBytes נתוני התמונה הנגועה (Stego-Image).
     * @return מחרוזת הטקסט המכילה את המסר שפוענח.
     * @throws IllegalArgumentException אם הקובץ אינו תמונה חוקית או שניסיון קריאת כותרת המסר נכשל.
     * @throws Exception שגיאות קריאה ברמת המערכת.
     * * זמן ריצה (Time Complexity) במקרה הגרוע (Worst Case):
     * O(W * H) או O(N)
     * ככל שהמסר הסודי ארוך יותר, כך לולאת החילוץ תסרוק יותר פיקסלים.
     * במקרה הגרוע ביותר (בו התמונה אינה מכילה מסר או מכילה כותרת מזויפת ענקית המאלצת 
     * מעבר על כלל התמונה), הלולאה תבצע סריקה מלאה של O(N/2) איטרציות.
     * * סיבוכיות מקום (Space Complexity): O(N)
     * כמו בהטמעה, `ImageIO.read` טוען את הקובץ לאובייקט `BufferedImage` התופס זיכרון יחסי למספר הפיקסלים.
     * מבנה ה-`BitSet` התופס מעט מאוד זיכרון (O(L) היכן ש-L אורך המסר) זניח ביחס לתמונה.
     */
    public String extract(byte[] fileBytes) throws Exception 
    {
        try
        {
            System.out.println("=== PVD Steganography - Extraction ===");
            
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) 
                throw new IllegalArgumentException("הקובץ שהועלה אינו פורמט תמונה חוקי לזיהוי.");

            int width = image.getWidth();
            int height = image.getHeight();

            BitSet extractedBits = new BitSet();
            int bitIndex = 0;
            int messageLengthBytes = 0;
            boolean lengthFound = false;

            // סריקת הפיקסלים לשליפת האורך, ואחריו את המסר
            MainLoop:
            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width - 1; x += 2) 
                {
                    int p1 = PvdUtils.getBlueValue(image, x, y);
                    int p2 = PvdUtils.getBlueValue(image, x + 1, y);

                    // מציאת גבולות הגזרה לקביעת כמות הביטים המוסתרים בפיקסל זה
                    int d = Math.abs(p2 - p1);
                    int rangeIndex = PvdUtils.getRangeIndex(d);
                    int capacity = PvdUtils.getCapacity(rangeIndex);
                    int l = PvdUtils.getRangeStart(rangeIndex);

                    // שליפת הערך העשרוני והמרתו חזרה לביטים
                    int m = d - l;

                    for (int i = 0; i < capacity; i++) 
                    {
                        if ((m & (1 << i)) != 0) 
                            extractedBits.set(bitIndex);
                        
                        bitIndex++;
                    }

                    // זיהוי ראשוני: פיענוח 16 ביטי הכותרת (אורך המסר)
                    if (!lengthFound && bitIndex >= PvdUtils.MESSAGE_LENGTH_BITS) 
                    {
                        BitSet lengthBitsOnly = extractedBits.get(0, PvdUtils.MESSAGE_LENGTH_BITS);
                        messageLengthBytes = PvdUtils.bitsToValue(lengthBitsOnly, PvdUtils.MESSAGE_LENGTH_BITS);
                        lengthFound = true;
                        System.out.println("Message Size Extracted: " + messageLengthBytes + " bytes");
                    }

                    // תנאי עצירה: ברגע שמספר הביטים שחולץ שווה לאורך המסר המוצהר
                    if (lengthFound) 
                    {
                        int totalBitsNeeded = PvdUtils.MESSAGE_LENGTH_BITS + (messageLengthBytes * 8);
                        if (bitIndex >= totalBitsNeeded) 
                            break MainLoop;
                    }
                }
            }

            // חומת מגן: מניעת שגיאת חריגת זיכרון/קורלציה אם הקובץ לא באמת מכיל מסר PVD אמין
            if (!lengthFound || messageLengthBytes <= 0 || messageLengthBytes > 65535) 
                throw new IllegalArgumentException("שגיאה: התמונה פגומה או שאינה מכילה מסר סטגנוגרפי חוקי.");

            // המרת רצף הביטים למחרוזת
            BitSet msgBits = extractedBits.get(PvdUtils.MESSAGE_LENGTH_BITS, PvdUtils.MESSAGE_LENGTH_BITS + (messageLengthBytes * 8));
            String extractedMessage = PvdUtils.bitsToText(msgBits, messageLengthBytes);
            
            System.out.println("\nExtracted Message:\n[" + extractedMessage + "]");
            
            return extractedMessage;
        } 
        catch (IllegalArgumentException e) 
        {
            throw e;
        } 
        catch (Exception e) 
        {
            throw new Exception("שגיאת מערכת במהלך פענוח PVD: " + e.getMessage(), e);
        } 
    }
}