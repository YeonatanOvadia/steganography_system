package yeonatano.steganography_system.utilities.QDCT;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Decomposer - מפרק קובץ JPEG לרשימת מקדמי DCT מקוונטטים
 * 
 * תהליך הפירוק:
 * 1. טעינת תמונת JPEG
 * 2. המרה מ-RGB ל-YCbCr (מרחב הצבעים של JPEG)
 * 3. חלוקה לבלוקים 8x8
 * 4. ביצוע DCT + קוונטיזציה על כל בלוק
 * 5. החזרת רשימת מקדמים
 * 
 * זהו הממשק בין קובץ JPEG לבין מנוע ה-F5
 */
public class Decomposer {
    
    private BufferedImage image;          // התמונה המקורית
    private int width;                    // רוחב התמונה
    private int height;                   // גובה התמונה
    
    // ערוצי YCbCr
    private int[][] yChannel;             // Luminance (בהירות)
    private int[][] cbChannel;            // Chrominance Blue
    private int[][] crChannel;            // Chrominance Red
    
    // רשימת כל הבלוקים
    private List<JPEGBlock> blocks;
    
    /**
     * טוען קובץ JPEG ומפרק אותו
     * 
     * @param inputStream קובץ JPEG
     * @throws IOException אם יש שגיאה בקריאת הקובץ
     */
    public Decomposer(InputStream inputStream) throws IOException {
        // טעינת התמונה
        this.image = ImageIO.read(inputStream);
        if (this.image == null) {
            throw new IOException("Failed to read image: ");
        }
        
        this.width = image.getWidth();
        this.height = image.getHeight();
        
        System.out.println("Loaded image: " + width + "x" + height + " pixels");
        
        // המרה ל-YCbCr
        convertRGBtoYCbCr();
        
        // פירוק לבלוקים
        decomposeToBlocks();
    }
    
    /**
     * ממיר את התמונה מ-RGB ל-YCbCr
     * 
     * YCbCr הוא מרחב הצבעים של JPEG:
     * - Y = בהירות (Luminance)
     * - Cb = כחול יחסי (Chrominance Blue)
     * - Cr = אדום יחסי (Chrominance Red)
     * 
     * הנוסחאות (ITU-R BT.601):
     * Y  =  0.299*R + 0.587*G + 0.114*B
     * Cb = -0.169*R - 0.331*G + 0.500*B + 128
     * Cr =  0.500*R - 0.419*G - 0.081*B + 128
     */
    private void convertRGBtoYCbCr() {
        // אתחול המערכים
        yChannel = new int[height][width];
        cbChannel = new int[height][width];
        crChannel = new int[height][width];
        
        // המרה פיקסל-פיקסל
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // קריאת פיקסל RGB
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // המרה ל-YCbCr
                yChannel[y][x] = clamp((int)(0.299 * r + 0.587 * g + 0.114 * b));
                cbChannel[y][x] = clamp((int)(-0.169 * r - 0.331 * g + 0.500 * b + 128));
                crChannel[y][x] = clamp((int)(0.500 * r - 0.419 * g - 0.081 * b + 128));
            }
        }
        
        System.out.println("Converted RGB to YCbCr");
    }
    
    /**
     * מפרק את כל הערוצים לבלוקים 8x8
     * 
     * אם התמונה לא מתחלקת ב-8, מבצעים padding (השלמה באפסים)
     */
   private void decomposeToBlocks() {
    blocks = new ArrayList<>();
    int blocksHorizontal = (int) Math.ceil(width / 8.0);
    int blocksVertical = (int) Math.ceil(height / 8.0);

    // בתוך Decomposer.java
    for (int by = 0; by < blocksVertical; by++) {
        for (int bx = 0; bx < blocksHorizontal; bx++) {
            // ערוץ Y
            int[][] yBlock = extractBlock(yChannel, bx * 8, by * 8);
            JPEGBlock yJpegBlock = new JPEGBlock(yBlock, JPEGBlock.BlockType.LUMINANCE);
            yJpegBlock.compress(); 
            blocks.add(yJpegBlock);
            
            // ערוץ Cb
            int[][] cbBlock = extractBlock(cbChannel, bx * 8, by * 8);
            JPEGBlock cbJpegBlock = new JPEGBlock(cbBlock, JPEGBlock.BlockType.CHROMINANCE);
            cbJpegBlock.compress();
            blocks.add(cbJpegBlock);
            
            // ערוץ Cr
            int[][] crBlock = extractBlock(crChannel, bx * 8, by * 8);
            JPEGBlock crJpegBlock = new JPEGBlock(crBlock, JPEGBlock.BlockType.CHROMINANCE);
            crJpegBlock.compress();
            blocks.add(crJpegBlock);
        }
    }
}
    
    /**
     * מחלץ בלוק 8x8 מערוץ נתון
     * אם הבלוק חורג מגבולות התמונה, משלים באפסים (padding)
     * 
     * @param channel ערוץ (Y/Cb/Cr)
     * @param startX X התחלתי
     * @param startY Y התחלתי
     * @return בלוק 8x8
     */
    private int[][] extractBlock(int[][] channel, int startX, int startY) {
        int[][] block = new int[8][8];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int y = startY + i;
                int x = startX + j;
                
                // אם בתוך גבולות התמונה - העתק פיקסל
                // אחרת - השלם באפס (padding)
                if (y < height && x < width) {
                    block[i][j] = channel[y][x];
                } else {
                    block[i][j] = 0;
                }
            }
        }
        
        return block;
    }
    
    /**
     * מחזיר את כל המקדמים המקוונטטים כרשימה שטוחה
     * זה הפורמט שמנוע F5 צריך!
     * 
     * @return רשימה של כל המקדמים (כל בלוק הופך למערך של 64 מספרים)
     */
    public List<Integer> getAllCoefficients() {
        List<Integer> allCoeffs = new ArrayList<>();
        
        for (JPEGBlock block : blocks) {
            // המרה לזיגזג (מערך 1D)
            int[] zigzag = block.toZigzagArray();
            
            // הוספה לרשימה
            for (int coeff : zigzag) {
                allCoeffs.add(coeff);
            }
        }
        
        return allCoeffs;
    }
    
    /**
     * מחזיר רק מקדמים AC (ללא DC)
     * 
     * DC = המקדם הראשון (ממוצע הבלוק) - לא נוגעים בו!
     * AC = 63 המקדמים הנותרים - אלה בהם נטמיע
     * 
     * @return רשימה של מקדמי AC בלבד
     */
    public List<Integer> getACCoefficients() {
        List<Integer> acCoeffs = new ArrayList<>();
        
        for (JPEGBlock block : blocks) {
            int[] zigzag = block.toZigzagArray();
            
            // מדלגים על האיבר הראשון (DC)
            for (int i = 1; i < 64; i++) {
                acCoeffs.add(zigzag[i]);
            }
        }
        
        return acCoeffs;
    }
    
    /**
     * מעדכן מקדמים (אחרי הטמעת F5)
     * 
     * @param allCoeffs רשימה של כל המקדמים (כולל DC)
     */
    public void setAllCoefficients(List<Integer> allCoeffs) {
        if (allCoeffs.size() != blocks.size() * 64) {
            throw new IllegalArgumentException(
                "Expected " + (blocks.size() * 64) + " coefficients, got " + allCoeffs.size()
            );
        }
        
        int idx = 0;
        for (JPEGBlock block : blocks) {
            // חילוץ 64 המקדמים של הבלוק הנוכחי
            int[] zigzag = new int[64];
            for (int i = 0; i < 64; i++) {
                zigzag[i] = allCoeffs.get(idx++);
            }
            
            // עדכון הבלוק
            block.fromZigzagArray(zigzag);
        }
    }
    
    /**
     * מחזיר את רשימת הבלוקים (גישה ישירה אם צריך)
     */
    public List<JPEGBlock> getBlocks() {
        return new ArrayList<>(blocks); // העתק למניעת שינוי חיצוני
    }
    
    /**
     * מחזיר את גודל התמונה
     */
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    /**
     * הגבלת ערך לטווח 0-255
     */
    private int clamp(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }
    
    /**
     * הדפסת סטטיסטיקות (לדיבוג)
     */
    public void printStatistics() {
        System.out.println("\n=== Decomposer Statistics ===");
        System.out.println("Image size: " + width + "x" + height);
        System.out.println("Total blocks: " + blocks.size());
        System.out.println("Total coefficients: " + (blocks.size() * 64));
        System.out.println("AC coefficients (usable for F5): " + (blocks.size() * 63));
        
        // חישוב כמה מקדמים AC הם לא-אפס
        List<Integer> acCoeffs = getACCoefficients();
        long nonZero = acCoeffs.stream().filter(c -> c != 0).count();
        System.out.println("Non-zero AC coefficients: " + nonZero);
        System.out.println("Capacity (max bits): ~" + (nonZero / 3) + " bits");
    }
}