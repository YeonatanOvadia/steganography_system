package yeonatano.steganography_system.utilities.QDCT;

/**
 * מייצגת בלוק 8x8 בתהליך ה-JPEG.
 */
public class JPEGBlock {
    private int[][] pixels;
    private int[][] quantizedCoeffs;
    private final int[][] quantizationTable;
    private final BlockType type;

    public enum BlockType { LUMINANCE, CHROMINANCE }

    /** בנאי ליצירת בלוק מפיקסלים (שלב ההטמעה) */
    public JPEGBlock(int[][] pixels, BlockType type) {
        this.pixels = pixels;
        this.type = type;
        this.quantizationTable = QuantizationTable.scaleQuantizationTable(
            type == BlockType.LUMINANCE ? QuantizationTable.LUMINANCE_TABLE : QuantizationTable.CHROMINANCE_TABLE, 100);
    }

    /** בנאי ליצירת בלוק ממקדמים (שלב החילוץ) */
    public JPEGBlock(int[][] quantizedCoeffs, BlockType type, boolean fromCoeffs) {
        this.quantizedCoeffs = quantizedCoeffs;
        this.type = type;
        this.quantizationTable = QuantizationTable.scaleQuantizationTable(
            type == BlockType.LUMINANCE ? QuantizationTable.LUMINANCE_TABLE : QuantizationTable.CHROMINANCE_TABLE, 100);
    }

    public void compress() {
        double[][] dct = DCTTransform.forwardDCT(pixels);
        this.quantizedCoeffs = QuantizationTable.quantize(dct, quantizationTable);
    }

    public int[] toZigzagArray() { return QuantizationTable.toZigzag(quantizedCoeffs); }
    
    public void fromZigzagArray(int[] zigzag) { this.quantizedCoeffs = QuantizationTable.fromZigzag(zigzag); }

    public int[][] getQuantizedCoefficients() { return quantizedCoeffs; }
    
    public BlockType getType() { return type; }
}