package yeonatano.steganography_system.services;

import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.springframework.stereotype.Service;
import yeonatano.steganography_system.utilities.PvdUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.BitSet;

/**
 * שירות (Service) המיישם את אלגוריתם PVD - Pixel Value Differencing.
 * מחלקה זו מנוהלת על ידי Spring Boot ומאפשרת הזרקה לשכבת הניתוב.
 */
@Service
public class PVDStegoService 
{

    /**
     * פונקציית ההטמעה (Embedding)
     * @param buffer האובייקט שמכיל את הקובץ שהועלה מהמשתמש (דרך Vaadin)
     * @param secretMessage המסר הסודי שאנו רוצים להחביא
     * @return מערך בייטים (byte[]) של התמונה המוצפנת (תואם לאלגוריתמים האחרים במערכת)
     */
    public byte[] embed(MemoryBuffer buffer, String secretMessage) throws Exception 
    {
        
        System.out.println("=== PVD Steganography - Embedding ===");

        // חילוץ זרם הנתונים (Stream) מהבאפר והמרתו לאובייקט תמונה
        InputStream inputStream = buffer.getInputStream();
        BufferedImage originalImage = ImageIO.read(inputStream);
        
        if (originalImage == null) 
        {
            throw new IllegalArgumentException("The uploaded file is not a valid image.");
        }

        // קריטי: מונע באגים בתמונות עם פלטת צבעים קבועה (Indexed Colors) שמעגלות פיקסלים ומהרסות את האלגוריתם.
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        // 1. טעינת התמונה המקורית 
        int width = image.getWidth();
        int height = image.getHeight();

        // בדיקת מגבלת גודל מה-Utils (כדי למנוע קריסת זיכרון של השרת)
        if (!PvdUtils.isImageSizeValid(image)) 
        {
            throw new IllegalArgumentException("Image exceeds maximum allowed dimensions.");
        }

        // 2. הכנת המסר והמרתו למערך בייטים
        byte[] messageBytes = secretMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // חישוב אורך המסר בבייטים כדי להכניס אותו גם לתמונה (כדי שהמחלץ ידע מתי לעצור)
        int messageLengthBytes = messageBytes.length;

        // המרת הטקסט והאורך שלו לייצוג בינארי באמצעות BitSet בעזרת מתודות מחלקת העזר
        BitSet messageBits = PvdUtils.textToBits(secretMessage);
        BitSet lengthBits = PvdUtils.valueToBits(messageLengthBytes, PvdUtils.MESSAGE_LENGTH_BITS);

        // יצירת מבנה נתונים חדש ("סרט" ביטים) שיכיל את כל המידע שיוחבא
        // תחילה נכניס 16 ביטים המייצגים את האורך, ולאחריהם את ביטי ההודעה עצמה
        BitSet totalBitsToHide = new BitSet();
        int currentBitIndex = 0;
        
        // לולאה לדחיפת 16 ביטי אורך המסר
        for (int i = 0; i < PvdUtils.MESSAGE_LENGTH_BITS; i++)
        {
            if (lengthBits.get(i)) totalBitsToHide.set(currentBitIndex);
            currentBitIndex++;
        }
        
        // לולאה לדחיפת ביטי המסר עצמו (אורך ההודעה בבייטים * 8)
        for (int i = 0; i < messageLengthBytes * 8; i++) 
        {
            if (messageBits.get(i)) totalBitsToHide.set(currentBitIndex);
            currentBitIndex++;
        }

        // שמירת אורך סך כל הביטים שייכנסו לתמונה
        int totalBitsLength = currentBitIndex;
        
        // מצביע (Pointer) שעוקב איזה ביט מה-"סרט" אנו עתידים להחביא בזוג הפיקסלים הנוכחי
        int bitPointer = 0; 

        // 3. ריצה על התמונה והצפנת המידע
        // תווית (Label) המאפשרת יציאה מוחלטת מהלולאה החיצונית ברגע שסיימנו
        MainLoop:
        for (int y = 0; y < height; y++) 
        {
            // קפיצות בזוגות, כיוון ש-PVD מחשב הפרשים בין זוגות פיקסלים סמוכים
            for (int x = 0; x < width - 1; x += 2) 
            {
                
                // תנאי סיום: אם החבאנו את כל הביטים הדרושים, אין צורך להמשיך לסרוק את התמונה
                if (bitPointer >= totalBitsLength) 
                    break MainLoop; 

                // קריאת ערכי הצבע בערוץ הכחול (Blue) של שני הפיקסלים
                int p1 = PvdUtils.getBlueValue(image, x, y);
                int p2 = PvdUtils.getBlueValue(image, x + 1, y);

                // חישוב ההפרש המוחלט (d) בין שני הפיקסלים כפי שנדרש באלגוריתם PVD
                int d = Math.abs(p2 - p1);
                
                // חיפוש הטווח שאליו ההפרש (d) שייך בטבלת הטווחים (למשל: 0-7, 8-15)
                int rangeIndex = PvdUtils.getRangeIndex(d);
                
                // קביעת מספר הביטים שניתן להחביא בזוג זה בהתבסס על הטווח
                int capacity = PvdUtils.getCapacity(rangeIndex);
                
                // מציאת הגבול התחתון של הטווח (לדוגמה: אם הטווח הוא 8-15, l יהיה 8)
                int l = PvdUtils.getRangeStart(rangeIndex);

                // משיכת הביטים מההודעה והמרתם לערך עשרוני (M) שיוכנס לתמונה
                int m = 0;
                for (int i = 0; i < capacity; i++) 
                {
                    // אם יש עוד ביטים להחביא והביט הנוכחי הוא 1
                    if (bitPointer < totalBitsLength && totalBitsToHide.get(bitPointer))
                        m |= (1 << i); // פעולת OR בינארית כדי להדליק את הביט ה-i בערך M
                    
                    // התקדמות למקום הבא ב-"סרט" הביטים הכללי
                    bitPointer++;
                }

                // חישוב ההפרש החדש והמוצפן (d') על ידי הוספת ערך המסר העשרוני (M) לגבול התחתון
                int newAbsD = l + m;

                // חישוב שגיאת ההפרש (error): ההפרש בין ההפרש החדש והרצוי (d') להפרש המקורי (d)
                int error = newAbsD - d;

                // --- תוקן: שגיאת העיגול המתמטי ---
                // חלוקת שגיאת ההפרש לשניים. בג'אווה, חלוקת מספר שלילי (למשל 7-) ל-2 מחזירה 3- (מאבדת את השארית).
                // כדי שלא נאבד את הביט הזה, errorDown מחושב כהפרש המדויק מ-error!
                int errorUp = (int) Math.ceil(error / 2.0);
                int errorDown = error - errorUp;

                // משתנים להחזקת ערכי הפיקסלים לאחר ההצפנה
                int newP1, newP2;

                // עדכון הפיקסלים: הרחבת או כיווץ הפער כך שההפרש החדש ייווצר.
                // החוקיות דורשת שמירה על מי שהיה גדול יותר - יישאר גדול יותר או שווה
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

                // הגנה מפני חריגות (Overflow/Underflow): 
                // ערכי צבע חייבים להיות בין 0 ל-255. 
                // שיטת "המעלית": אם אחד הפיקסלים חורג, מזיזים את *שני הפיקסלים* יחד באותו כיוון ובאותה מידה.
                // כך אנחנו נשארים בתחום החוקי, מבלי לשנות את ההפרש החדש והמוצפן!
                
                // בדיקה לפיקסל הראשון
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

                // בדיקה לפיקסל השני
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
                
                // הטמעת ערכי הפיקסלים המעודכנים (לאחר ההצפנה וההגנה) בחזרה לתוך התמונה
                PvdUtils.setBlueValue(image, x, y, newP1);
                PvdUtils.setBlueValue(image, x + 1, y, newP2);
            }
        }
        
        // הגנה: אם עברנו על כל התמונה ולא הספקנו להחביא את כל המסר
        if (bitPointer < totalBitsLength) 
            throw new Exception("Image capacity is too small to hide the entire message using PVD.");

        System.out.println("Message embedded successfully! Total bits hidden: " + bitPointer);

        // 4. שמירת התמונה עם המידע המוסתר והמרתה למערך בייטים
        // חובה להשתמש בפורמט PNG, מכיוון שזהו פורמט Lossless (ללא אובדן נתונים).
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        
        // מחזיר את התמונה כ-byte array כך ששכבת התצוגה (Vaadin) תוכל להוריד או להציג אותה
        return baos.toByteArray();
    }


    /**
     * פונקציית החילוץ (Extraction)
     * @param buffer האובייקט שמכיל את התמונה הנגועה (Stego-Image) שהמשתמש העלה
     * @return המחרוזת (המסר הסודי) שחולצה מתוך התמונה
     */
    public String extract(MemoryBuffer buffer) throws Exception 
    {
        
        System.out.println("=== PVD Steganography - Extraction ===");
        
        // טעינת התמונה המוצפנת מתוך ה-MemoryBuffer
        InputStream inputStream = buffer.getInputStream();
        BufferedImage image = ImageIO.read(inputStream);
        
        if (image == null) 
            throw new IllegalArgumentException("The uploaded file is not a valid image.");
        

        int width = image.getWidth();
        int height = image.getHeight();

        // משתנים לניהול החילוץ
        BitSet extractedBits = new BitSet();     // כל הביטים שנחלץ
        int bitIndex = 0;                        // מצביע לביט הנוכחי
        int messageLengthBytes = 0;              // אורך המסר בבייטים (יתמלא אחרי 16 ביטים)
        boolean lengthFound = false;             // דגל: האם חילצנו את האורך

        // לולאה רציפה שמחלצת תחילה את האורך ואחר כך את המסר
        MainLoop:
        for (int y = 0; y < height; y++) 
        {               // שורות
            for (int x = 0; x < width - 1; x += 2) 
            {     // זוגות פיקסלים
                
                // קריאת ערכי כחול של הזוג
                int p1 = PvdUtils.getBlueValue(image, x, y);
                int p2 = PvdUtils.getBlueValue(image, x + 1, y);

                // חישוב ההפרש (תמיד משתמשים בערך מוחלט למציאת הטווח)
                int d = Math.abs(p2 - p1);

                // הטווח (הקופסה) באמת מגדיר לנו את גבולות הגזרה: הוא מזהה כמה רעש יש באזור הזה
                // בתמונה, ולפי זה נותן לנו אינדיקציה כמה מותר לנו לשחק עם הרווח החדש בלי שישימו לב.
                int rangeIndex = PvdUtils.getRangeIndex(d);       // באיזה טווח?
                int capacity = PvdUtils.getCapacity(rangeIndex);  // כמה ביטים?
                int l = PvdUtils.getRangeStart(rangeIndex);       // התחלת הטווח

                // חישוב הערך המוחבא (m): ההפרש המוחלט הנוכחי פחות התחלת הטווח
                int m = d - l;

                // חילוץ הביטים מהערך ושמירה ב-BitSet
                for (int i = 0; i < capacity; i++) 
                {
                    // בדיקה אם ביט i של m הוא 1
                    if ((m & (1 << i)) != 0) 
                        extractedBits.set(bitIndex);  // שמירת 1 ב-BitSet
                   
                    // אם הביט 0 - BitSet כבר מאותחל ל-0
                    bitIndex++;  // התקדמות למיקום הבא
                }

                // בדיקה: האם חילצנו את 16 הביטים הראשונים (אורך המסר)?
                if (!lengthFound && bitIndex >= PvdUtils.MESSAGE_LENGTH_BITS) 
                {
                    // --- תוקן: חיתוך מדויק ---
                    // אנו חותכים בדיוק את 16 הביטים הראשונים כדי שלא נמיר בטעות "רעש" מביטים הבאים
                    BitSet lengthBitsOnly = extractedBits.get(0, PvdUtils.MESSAGE_LENGTH_BITS);
                    
                    // המרת 16 הביטים הראשונים למספר = אורך המסר בבייטים
                    messageLengthBytes = PvdUtils.bitsToValue(lengthBitsOnly, PvdUtils.MESSAGE_LENGTH_BITS);
                    lengthFound = true;
                    System.out.println("Message Size Extracted: " + messageLengthBytes + " bytes");
                }

                // בדיקת סיום: האם חילצנו את כל המסר?
                if (lengthFound) 
                {
                    // סך הביטים = 16 (אורך) + (מספר בייטים × 8)
                    int totalBitsNeeded = PvdUtils.MESSAGE_LENGTH_BITS + (messageLengthBytes * 8);
                    if (bitIndex >= totalBitsNeeded) break MainLoop;  // סיימנו - יציאה מוחלטת מהלולאה
                }
            }
        }

        // הגנה: אם משום מה לא הצלחנו לחלץ אורך תקין מהתמונה (תמונה שהושחתה או גדולה מדי)
        if (!lengthFound || messageLengthBytes <= 0 || messageLengthBytes > 65535) 
            return "Error: No valid hidden message found or image is corrupted.";

        // חילוץ רק ביטי המסר (מדלג על 16 הראשונים שהכילו רק את האורך)
        BitSet msgBits = extractedBits.get(PvdUtils.MESSAGE_LENGTH_BITS, PvdUtils.MESSAGE_LENGTH_BITS + (messageLengthBytes * 8));
        
        // המרה לטקסט סופי (המרת מערך הביטים חזרה ל-String)
        String extractedMessage = PvdUtils.bitsToText(msgBits, messageLengthBytes);
        System.out.println("\nExtracted Message:\n[" + extractedMessage + "]");
        
        return extractedMessage;
    }
}