package yeonatano.steganography_system.utilities.QDCT;

/**
 * ניהול טבלאות הקוונטיזציה והמרות הזיגזג עבור תקן JPEG.
 * @author יהונתן עובדיה
 */
public class QuantizationTable {
    
    /** טבלת קוונטיזציה סטנדרטית לבהירות (Luminance) */
    public static final int[][] LUMINANCE_TABLE = {
        {16, 11, 10, 16, 24, 40, 51, 61},
        {12, 12, 14, 19, 26, 58, 60, 55},
        {14, 13, 16, 24, 40, 57, 69, 56},
        {14, 17, 22, 29, 51, 87, 80, 62},
        {18, 22, 37, 56, 68, 109, 103, 77},
        {24, 35, 55, 64, 81, 104, 113, 92},
        {49, 64, 78, 87, 103, 121, 120, 101},
        {72, 92, 95, 98, 112, 100, 103, 99}
    };

    /** טבלת קוונטיזציה סטנדרטית לצבע (Chrominance) */
    public static final int[][] CHROMINANCE_TABLE = {
        {17, 18, 24, 47, 99, 99, 99, 99},
        {18, 21, 26, 66, 99, 99, 99, 99},
        {24, 26, 56, 99, 99, 99, 99, 99},
        {47, 66, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99}
    };

    /**
     * מחשבת טבלת קוונטיזציה מותאמת לפי רמת איכות (1-100).
     */
    public static int[][] scaleQuantizationTable(int[][] baseTable, int quality) {
        double scale = (quality < 50) ? (5000.0 / quality) : (200.0 - 2.0 * quality);
        int[][] scaledTable = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int value = (int) Math.round((baseTable[i][j] * scale + 50.0) / 100.0);
                scaledTable[i][j] = Math.max(1, Math.min(255, value));
            }
        }
        return scaledTable;
    }

    /** ביצוע קוונטיזציה על בלוק DCT */
    public static int[][] quantize(double[][] dct, int[][] table) {
        int[][] q = new int[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                q[i][j] = (int) Math.round(dct[i][j] / table[i][j]);
        return q;
    }

    /** המרה מבלוק 8x8 למערך Zigzag של 64 איברים */
    public static int[] toZigzag(int[][] block) {
        int[] zigzag = new int[64];
        int[] order = {0,1,5,6,14,15,27,28,2,4,7,13,16,26,29,42,3,8,12,17,25,30,41,43,9,11,18,24,31,40,44,53,10,19,23,32,39,45,52,54,20,22,33,38,46,51,55,60,21,34,37,47,50,56,59,61,35,36,48,49,57,58,62,63};
        int idx = 0;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                zigzag[order[idx++]] = block[i][j];
        return zigzag;
    }

    /** המרה ממערך Zigzag חזרה לבלוק 8x8 */
    public static int[][] fromZigzag(int[] zigzag) {
        int[][] block = new int[8][8];
        int[] order = {0,1,5,6,14,15,27,28,2,4,7,13,16,26,29,42,3,8,12,17,25,30,41,43,9,11,18,24,31,40,44,53,10,19,23,32,39,45,52,54,20,22,33,38,46,51,55,60,21,34,37,47,50,56,59,61,35,36,48,49,57,58,62,63};
        int idx = 0;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                block[i][j] = zigzag[order[idx++]];
        return block;
    }
}