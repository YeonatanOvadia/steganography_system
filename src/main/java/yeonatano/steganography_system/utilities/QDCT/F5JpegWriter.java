package yeonatano.steganography_system.utilities.QDCT;

import java.io.*;
import java.util.List;

/**
 * מחלקה האחראית על כתיבת מקדמי DCT מקוונטטים ישירות לקובץ JPEG.
 * המימוש מבצע Entropy Encoding (Huffman + RLE) ללא חזרה למרחב הפיקסלים,
 * מה שמבטיח שביטי המסר שהוטמעו (LSBs) לא ייפגעו.
 * * @author יהונתן עובדיה
 */
public class F5JpegWriter {

    private final int width;
    private final int height;
    private final List<JPEGBlock> blocks;
    private int bufferPutBits;
    private int bufferPutBuffer;

    // --- טבלאות Huffman סטנדרטיות (Luminance) ---
    private static final int[] DC_LUM_BITS = {0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] DC_LUM_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AC_LUM_BITS = {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125};
    private static final int[] AC_LUM_VAL = 
    {
        0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
        0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
        0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
        0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
        0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
        0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
        0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };

    // --- טבלאות Huffman סטנדרטיות (Chrominance) ---
    private static final int[] DC_CHR_BITS = {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
    private static final int[] DC_CHR_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AC_CHR_BITS = {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119};
    private static final int[] AC_CHR_VAL = 
    {
        0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
        0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
        0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
        0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
        0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
        0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
    };

    public F5JpegWriter(int width, int height, List<JPEGBlock> blocks) {
        this.width = width;
        this.height = height;
        this.blocks = blocks;
    }

    /**
     * פונקציית הכתיבה הראשית. פותחת קובץ וכותבת את כל המרקרים והנתונים.
     */
    public void writeRawDCT(ByteArrayOutputStream outputStream) throws IOException 
    {
        try (BufferedOutputStream bos = new BufferedOutputStream(new BufferedOutputStream(outputStream))) {
            writeHeaders(bos);
            encodeHuffmanAndWrite(bos);
            bos.write(0xFF); bos.write(0xD9); // Marker: End of Image (EOI)
        }
    }

    private void writeHeaders(BufferedOutputStream bos) throws IOException {
        bos.write(new byte[]{(byte)0xFF, (byte)0xD8}); // SOI
        bos.write(new byte[]{(byte)0xFF, (byte)0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00});

        // שימוש באיכות מקסימלית (100) כדי ששינויי LSB לא יגרמו לפיקסול
        int[][] scaledLuma = QuantizationTable.scaleQuantizationTable(QuantizationTable.LUMINANCE_TABLE, 100);
        int[][] scaledChroma = QuantizationTable.scaleQuantizationTable(QuantizationTable.CHROMINANCE_TABLE, 100);

        writeDQT(bos, 0x00, scaledLuma);
        writeDQT(bos, 0x01, scaledChroma);

        // הגדרת Frame (SOF0) ל-3 ערוצי צבע
        bos.write(new byte[]{(byte)0xFF, (byte)0xC0, 0x00, 0x11, 0x08});
        bos.write((height >> 8) & 0xFF); bos.write(height & 0xFF);
        bos.write((width >> 8) & 0xFF);  bos.write(width & 0xFF);
        bos.write(0x03); 
        bos.write(new byte[]{0x01, 0x11, 0x00}); // Y
        bos.write(new byte[]{0x02, 0x11, 0x01}); // Cb
        bos.write(new byte[]{0x03, 0x11, 0x01}); // Cr

        // כתיבת טבלאות Huffman (Luma & Chroma)
        writeDHT(bos, 0x00, DC_LUM_BITS, DC_LUM_VAL);
        writeDHT(bos, 0x10, AC_LUM_BITS, AC_LUM_VAL);
        writeDHT(bos, 0x01, DC_CHR_BITS, DC_CHR_VAL);
        writeDHT(bos, 0x11, AC_CHR_BITS, AC_CHR_VAL);

        // Marker: Start of Scan (SOS)
        bos.write(new byte[]{(byte)0xFF, (byte)0xDA, 0x00, 0x0C, 0x03});
        bos.write(new byte[]{0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 0x3F, 0x00});
    }

    private void writeDQT(BufferedOutputStream bos, int id, int[][] table) throws IOException {
        bos.write(0xFF); bos.write(0xDB); bos.write(0x00); bos.write(0x43); bos.write(id);
        for (int i = 0; i < 64; i++) bos.write(table[i/8][i%8]);
    }

    private void writeDHT(BufferedOutputStream bos, int classId, int[] bits, int[] val) throws IOException {
        int length = 2 + 1 + 16 + val.length;
        bos.write(0xFF); bos.write(0xC4);
        bos.write((length >> 8) & 0xFF); bos.write(length & 0xFF);
        bos.write(classId);
        for (int b : bits) bos.write(b);
        for (int v : val) bos.write(v);
    }

    private void encodeHuffmanAndWrite(BufferedOutputStream bos) throws IOException {
        bufferPutBits = 0; bufferPutBuffer = 0;
        int[] lastDc = new int[3];

        // הכנת עצי הקידוד
        Object[] dcLum = computeHuffmanCodes(DC_LUM_BITS, DC_LUM_VAL);
        Object[] acLum = computeHuffmanCodes(AC_LUM_BITS, AC_LUM_VAL);
        Object[] dcChr = computeHuffmanCodes(DC_CHR_BITS, DC_CHR_VAL);
        Object[] acChr = computeHuffmanCodes(AC_CHR_BITS, AC_CHR_VAL);

        // ריצה על שלישיות בלוקים (Y, Cb, Cr)
        for (int i = 0; i < blocks.size(); i += 3) {
            for (int c = 0; c < 3; c++) {
                if (i + c >= blocks.size()) break;
                int[] zigzag = blocks.get(i + c).toZigzagArray();
                
                int[] dcCodes = (c == 0) ? (int[])dcLum[0] : (int[])dcChr[0];
                int[] dcSizes = (c == 0) ? (int[])dcLum[1] : (int[])dcChr[1];
                int[] acCodes = (c == 0) ? (int[])acLum[0] : (int[])acChr[0];
                int[] acSizes = (c == 0) ? (int[])acLum[1] : (int[])acChr[1];

                // קידוד DC
                int diff = zigzag[0] - lastDc[c]; lastDc[c] = zigzag[0];
                int nbits = getCategory(diff);
                writeBits(bos, dcCodes[nbits], dcSizes[nbits]);
                if (nbits > 0) writeBits(bos, getBitPattern(diff, nbits), nbits);

                // קידוד AC בשיטת Run-Length Encoding
                int runLength = 0;
                for (int k = 1; k < 64; k++) {
                    if (zigzag[k] == 0) {
                        runLength++;
                    } else {
                        while (runLength >= 16) {
                            writeBits(bos, acCodes[0xF0], acSizes[0xF0]); // ZRL
                            runLength -= 16;
                        }
                        int cat = getCategory(zigzag[k]);
                        writeBits(bos, acCodes[(runLength << 4) | cat], acSizes[(runLength << 4) | cat]);
                        writeBits(bos, getBitPattern(zigzag[k], cat), cat);
                        runLength = 0;
                    }
                }
                if (runLength > 0) writeBits(bos, acCodes[0x00], acSizes[0x00]); // EOB
            }
        }
        // ניקוי החוצץ האחרון
        if (bufferPutBits > 0) writeBits(bos, (1 << (8 - bufferPutBits)) - 1, 8 - bufferPutBits);
    }

    private int getCategory(int val) {
        int abs = Math.abs(val); int cat = 0;
        while (abs > 0) { cat++; abs >>= 1; }
        return cat;
    }

    private int getBitPattern(int val, int category) {
        if (val < 0) val += (1 << category) - 1;
        return val;
    }

    private void writeBits(BufferedOutputStream bos, int value, int size) throws IOException {
        bufferPutBuffer = (bufferPutBuffer << size) | (value & ((1 << size) - 1));
        bufferPutBits += size;
        while (bufferPutBits >= 8) {
            int c = (bufferPutBuffer >> (bufferPutBits - 8)) & 0xFF;
            bos.write(c);
            if (c == 0xFF) bos.write(0x00); // Bit Stuffing לתקן JPEG
            bufferPutBits -= 8;
        }
    }

    private Object[] computeHuffmanCodes(int[] bits, int[] val) {
        int[] codes = new int[256]; int[] sizes = new int[256];
        int code = 0; int k = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < bits[i]; j++) {
                codes[val[k]] = code++;
                sizes[val[k]] = i + 1;
                k++;
            }
            code <<= 1;
        }
        return new Object[]{codes, sizes};
    }
}