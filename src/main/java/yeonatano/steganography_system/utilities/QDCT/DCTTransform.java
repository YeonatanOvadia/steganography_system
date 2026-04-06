package yeonatano.steganography_system.utilities.QDCT;

/**
 * מבצעת התמרת קוסינוס דיסקרטית (DCT).
 * הופכת בלוק פיקסלים (מרחב) למקדמי תדר (תדר).
 */
public class DCTTransform {
    private static final double INV_SQRT_2 = 1.0 / Math.sqrt(2.0);
    private static final double[][] COS_TABLE = new double[8][8];

    static {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                COS_TABLE[i][j] = Math.cos(((2 * i + 1) * j * Math.PI) / 16.0);
    }

    public static double[][] forwardDCT(int[][] block) {
        double[][] dct = new double[8][8];
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                double sum = 0.0;
                for (int x = 0; x < 8; x++)
                    for (int y = 0; y < 8; y++)
                        sum += (block[x][y] - 128.0) * COS_TABLE[x][u] * COS_TABLE[y][v];
                
                double c = (u == 0 ? INV_SQRT_2 : 1.0) * (v == 0 ? INV_SQRT_2 : 1.0);
                dct[u][v] = 0.25 * c * sum;
            }
        }
        return dct;
    }

    public static int[][] inverseDCT(double[][] dct) {
        int[][] block = new int[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                double sum = 0.0;
                for (int u = 0; u < 8; u++) {
                    for (int v = 0; v < 8; v++) {
                        double c = (u == 0 ? INV_SQRT_2 : 1.0) * (v == 0 ? INV_SQRT_2 : 1.0);
                        sum += c * dct[u][v] * COS_TABLE[x][u] * COS_TABLE[y][v];
                    }
                }
                block[x][y] = Math.max(0, Math.min(255, (int) Math.round(0.25 * sum + 128.0)));
            }
        }
        return block;
    }
}