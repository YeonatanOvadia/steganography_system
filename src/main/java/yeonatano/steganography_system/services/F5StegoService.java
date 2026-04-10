package yeonatano.steganography_system.services;

import org.springframework.stereotype.Service;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import yeonatano.steganography_system.utilities.F5Utility;
import yeonatano.steganography_system.utilities.QDCT.Decomposer;
import yeonatano.steganography_system.utilities.QDCT.F5JpegReader;
import yeonatano.steganography_system.utilities.QDCT.F5JpegWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * מחלקת שירות (Service) המיישמת את אלגוריתם הסטגנוגרפיה F5 עבור קבצי JPEG.
 * האלגוריתם מבצע הטמעה וחילוץ של מידע סודי אל תוך מקדמי ה-DCT של התמונה
 * תוך שימוש בקידוד מטריצה (Matrix Encoding) כדי למזער את הפגיעה באיכות התמונה.
 */
@Service
public class F5StegoService 
{

    // private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * פונקציה להטמעת הודעה סודית בתוך תמונת JPEG.
     * התהליך כולל קריאת מקדמי ה-DCT, המרת ההודעה לביטים (כולל הוספת Header של 32 ביט לאורך המסר),
     * והטמעתם בעזרת Matrix Encoding על מקדמים שאינם אפס.
     *
     * @param imgFile קובץ התמונה המקורי שהועלה על ידי המשתמש (מתוך ה-Web Buffer).
     * @param secretMsg ההודעה הסודית שהמשתמש רוצה להסתיר בתמונה.
     * @return מערך בתים (byte[]) המייצג את התמונה החדשה (Stego-Image) לאחר ההטמעה, או null במקרה של שגיאה או חוסר מקום.
     */
    public byte[] embed(MemoryBuffer imgFile, String secretMsg) 
    {
        try {
            // התאמה ל-Stream: מקבלים את המידע ישירות מה-Buffer
            InputStream inputStream = imgFile.getInputStream();

            // --- תחילת הלוגיקה שלך ---
            byte[] messageBytes = secretMsg.getBytes(StandardCharsets.UTF_8);

            // 2. פירוק התמונה
            System.out.println("Analyzing image...");
            Decomposer decomposer = new Decomposer(inputStream);
            // List<Integer> - רשימה דינמית של מקדמי DCT
            //רשימת המקדמים
            List<Integer> DCTlist = decomposer.getAllCoefficients();

            // 1. מכינים את ה-Header (32 ביטים של האורך)
            int messageLength = messageBytes.length;
            int[] headerBits = F5Utility.getHeaderBits(messageLength);
            
            // 2. מכינים את הביטים של ההודעה עצמה
            int[] messageBits = F5Utility.bytesToBits(messageBytes);
            
            // 3. משרשרים אותם לרכבת אחת ארוכה המוכנה להטמעה
            int[] bitsToEmbed = F5Utility.concatArrays(headerBits, messageBits);
            
            System.out.println("Message length: " + messageLength + " bytes");
            System.out.println("Total bits to embed: " + bitsToEmbed.length + " (32 header + " + messageBits.length + " message)");


            // 5. לולאת ההטמעה
            // bitIndex - מונה הביטים שהוטמעו בהצלחה
            int bitIndex = 0;
            int msgIndex = 0;// המיקום במערך המסר
            int imgIndex = 0; //המיקום בטבלת המקדמים

            int[] m = new int[2]; //דגימה של המסר ממערך המסר 

            // int[] B = new int[3]; // מכיל את האינדקסים שאנחנו כרגע עליהם שאינם שווים ל0

            int[] bLsb = new int[3]; // מכיל את הביטים שדגמנו 

            int[] h = new int[2]; //מכיל את תוצאות הקסורים 

            int b1;
            int b2;
            int b3;

            boolean Zflag = false;


            // לולאה על המקדמים בסדר אקראי
            //הלולאה תרוץ כל עוד גודל מערך המסר גדול מהמיקום שאנו מנצאים בו
            while (msgIndex <= bitsToEmbed.length-1)
            {
               
                m[0] = bitsToEmbed[msgIndex];

                if (msgIndex + 1 < bitsToEmbed.length) 
                    m[1] = bitsToEmbed[msgIndex + 1];
                
                else m[1] = 0; // ריפוד במקרה של ביט אחרון בודד

                int[] IdxInDCT_List = new int[3];
                int validCount = 0;

                while (validCount < 3 && imgIndex < DCTlist.size()) 
                {
                    if (DCTlist.get(imgIndex) != 0)
                    {
                        IdxInDCT_List[validCount] = imgIndex;
                        validCount++;
                    }
                    imgIndex++;
                }
                   
                // בדיקה: האם יצאנו מהלולאה כי מצאנו 3 מקדמים או כי נגמרה הרשימה
                // אם validCount < 3 זה אומר שהגענו לסוף DCTlist בלי למצוא מספיק מקדמים לא-אפס.
                // במקרה זה, IdxInDCT_List לא מלא ולא יכול לשמש לגישה בטוחה למקדמים.
                // זה קורה כאשר התמונה חלקה מדי (שמיים/רקע) או שהמסר ארוך מדי לקיבולת התמונה.
                if (validCount < 3) 
                {
                    System.out.println("Error: Not enough coefficients remaining!");
                    break;
                }

                b1 = DCTlist.get(IdxInDCT_List[0]);
                b2 = DCTlist.get(IdxInDCT_List[1]);
                b3 = DCTlist.get(IdxInDCT_List[2]);

                bLsb[0] = F5Utility.getLSB(Math.abs(b1));
                bLsb[1] = F5Utility.getLSB(Math.abs(b2));
                bLsb[2] = F5Utility.getLSB(Math.abs(b3));

                h[0] = bLsb[0] ^ bLsb[2];
                h[1] = bLsb[1] ^ bLsb[2];

                if (h[0] != m[0] && h[1] != m[1])
                {
                    if (b3 > 0) b3 -= 1;
                        
                    else b3 += 1;

                    if (b3 == 0) Zflag = true;

                    DCTlist.set(IdxInDCT_List[2], b3);
                        
                }
                
                else if(h[0] != m[0]) //כאשר הקסור של הדגימה הראשונה והאחרונה לא שווים לביט הראשון של המסר נוריד 1 מהערך המוחלט של המספר המלא של אותו הביט הראשון
                {
                    if (b1 > 0) b1 -= 1;
                    
                    else b1 += 1;

                    if (b1 == 0) Zflag = true;
                    
                    DCTlist.set(IdxInDCT_List[0], b1);

                }

                else if(h[1] != m[1]) //כאשר הקסור של הדגימה השנייה והאחרונה לא שווים לביט הראשון של המסר, נוריד 1 מהערך המוחלט של המספר המלא של אותו הביט השני
                {
                    if (b2 > 0) b2 -= 1;

                    else b2 += 1;

                    if (b2 == 0) Zflag = true;

                    DCTlist.set(IdxInDCT_List[1], b2);
                        
                }


                if (Zflag)
                {
                    Zflag=false;
                    imgIndex = IdxInDCT_List[0];
                    continue;
                }

                msgIndex += 2;
                bitIndex = msgIndex;
            }

            if (bitIndex < bitsToEmbed.length) 
            {
                System.out.println("Error: Image capacity too small for this message!");
                return null;
            } 
            else 
            {
                decomposer.setAllCoefficients(DCTlist);
                F5JpegWriter directWriter = new F5JpegWriter(decomposer.getWidth(), decomposer.getHeight(), decomposer.getBlocks());
                
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // וודא ש-F5JpegWriter תומך בכתיבה ל-Stream
                directWriter.writeRawDCT(outputStream); 
                
                return convertToBuffer(outputStream, imgFile.getFileName(), imgFile.getFileData().getMimeType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * פונקציית עזר להמרת זרם נתונים (OutputStream) למערך בתים סופי.
     * * @param os זרם הנתונים המכיל את התמונה לאחר ההטמעה.
     * @param fileName שם קובץ המקור (לצורכי הדפסה ודיבאג).
     * @param mimeType סוג הקובץ (למשל image/jpeg).
     * @return מערך הבתים (byte[]) המייצג את התמונה, או null אם הזרם ריק.
     */
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

    /**
     * פונקציה לחילוץ הודעה סודית מתוך תמונת JPEG שעברה סטגנוגרפיה (Stego-Image).
     * הפונקציה קוראת את מקדמי ה-DCT של התמונה, מחלצת תחילה את אורך המסר (32 ביטים ראשונים),
     * ולאחר מכן מחלצת את תוכן ההודעה בעזרת פעולות XOR על הביטים הפחות משמעותיים (LSB).
     *
     * @param stegoFile קובץ התמונה המכיל את המידע הסודי (מתוך ה-Web Buffer).
     * @return מחרוזת המכילה את ההודעה הסודית שחולצה, או הודעת שגיאה במקרה של כשל.
     */
    public String extract(MemoryBuffer stegoFile) {
    try {
        // 1. טעינת התמונה המכילה את המסר (התאמה ל-InputStream של ה-Web)
        InputStream inputStream = stegoFile.getInputStream();
        
        // 1. פירוק התמונה כדי לקבל את המקדמים (חייבים לעשות את זה ראשון!)
        System.out.println("Analyzing stego image...");
        
        // שימוש ב-F5JpegReader המקורי מהקונסול (וודא שהמחלקה תומכת ב-InputStream או קובץ זמני)
        F5JpegReader reader = new F5JpegReader(inputStream); 
        List<Integer> DCTlist = reader.getQuantizedCoefficients();

        // 2. משתנים לשלב גילוי האורך
        int imgIndex = 0;
        int messageLength = 0;

        // הלולאה עוברת על ה-DCTlist, שולפת 32 ביטים ומכניסה אותם ל-messageLength
        for (int i = 0; i < 16; i++)
        {
            int[] validCoeffs = new int[3];
            int validCount = 0;

            // בדיוק מה שכתבת: מחפשים 3 מקדמים שלא שווים לאפס
            while (validCount < 3 && imgIndex < DCTlist.size())
            {
                int val = DCTlist.get(imgIndex);
                if (val != 0)
                {
                    validCoeffs[validCount] = val;
                    validCount++;
                }
                imgIndex++; // מתקדמים בתמונה
            }

            if (validCount < 3) break; // הגנה: אם נגמרה התמונה

            // שומרים את הערכים המקוריים
            int b1 = validCoeffs[0];
            int b2 = validCoeffs[1];
            int b3 = validCoeffs[2];

            // חילוץ ה-LSB למערך
            int[] bLsb = new int[3];
            bLsb[0] = F5Utility.getLSB(Math.abs(b1));
            bLsb[1] = F5Utility.getLSB(Math.abs(b2));
            bLsb[2] = F5Utility.getLSB(Math.abs(b3));

            // חישוב שני הביטים שלנו למערך h (זוגיות)
            int[] h = new int[2];
            h[0] = bLsb[0] ^ bLsb[2];
            h[1] = bLsb[1] ^ bLsb[2];

            // הלוגיקה החדשה: דוחפים את 2 הביטים שחילצנו לתוך מספר שלם
            messageLength = (messageLength << 1) | h[0];
            messageLength = (messageLength << 1) | h[1];
        }

        System.out.println("Detected message length: " + messageLength + " bytes");

        // הגנה נוספת ל-Web (למנוע NegativeArraySizeException אם האורך לא תקין)
        if (messageLength <= 0 || messageLength > DCTlist.size()) {
            return "No hidden message detected or invalid file.";
        }

        // 3. אחרי שלולאת ה-for סיימה ויש לנו אורך אמיתי - עכשיו ניצור את המערך!
        int totalBitsToExtract = messageLength * 8;
        int[] extractedBits = new int[totalBitsToExtract];
        int msgIndex = 0; // מונה שמתחיל מ-0 עבור המערך

        System.out.println("Extracting message...");

        while (msgIndex < totalBitsToExtract) {
            // א. חיפוש 3 מקדמים שאינם 0 (בדיוק כמו בהטמעה)
            int[] validCoeffs = new int[3];
            int validCount = 0;
            while (validCount < 3 && imgIndex < DCTlist.size()) 
            {
                int val = DCTlist.get(imgIndex);
                if (val != 0) 
                {
                    validCoeffs[validCount] = val;
                    validCount++;
                }
                imgIndex++;
            }

            // אם נגמרה התמונה לפני שסיימנו לקרוא
            if (validCount < 3) 
            {
                System.out.println("Reached the end of the image before extracting the full message.");
                break;
            }

            // ב. שומרים את הערכים המקוריים
            int b1 = validCoeffs[0];
            int b2 = validCoeffs[1];
            int b3 = validCoeffs[2];

            // ג. חילוץ LSB
            int[] bLsb = new int[3];
            bLsb[0] = F5Utility.getLSB(Math.abs(b1));
            bLsb[1] = F5Utility.getLSB(Math.abs(b2));
            bLsb[2] = F5Utility.getLSB(Math.abs(b3));

            // ד. חישוב ה-Hash (זוגיות) - אלו הם ביטי המסר שלנו!
            int[] h = new int[2];
            h[0] = bLsb[0] ^ bLsb[2];
            h[1] = bLsb[1] ^ bLsb[2];

            // ה. שמירת הביטים במערך המסר
            extractedBits[msgIndex] = h[0];
            if (msgIndex + 1 < totalBitsToExtract)
            {
                extractedBits[msgIndex + 1] = h[1];
            }

            // מתקדמים ב-2 ביטים
            msgIndex += 2;
        }

        // 4. המרת מערך הביטים חזרה לטקסט קריא
        byte[] messageBytes = F5Utility.bitsToBytes(extractedBits);
        String hiddenMessage = new String(messageBytes, StandardCharsets.UTF_8);

        System.out.println("\n--- Extraction Complete ---");
        System.out.println("Hidden Message: " + hiddenMessage);
        
        return hiddenMessage;

    } catch (Exception e) {
        e.printStackTrace();
        return "Extraction failed: " + e.getMessage();
    }

    }
}