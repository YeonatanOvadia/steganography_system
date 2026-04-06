package yeonatano.steganography_system.utilities.QDCT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class F5JpegReader {
    
    private List<Integer> quantizedCoefficients;

    private static final int[] DC_LUM_BITS = {0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] DC_LUM_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AC_LUM_BITS = {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125};
    private static final int[] AC_LUM_VAL = {
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

    private static final int[] DC_CHR_BITS = {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
    private static final int[] DC_CHR_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AC_CHR_BITS = {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119};
    private static final int[] AC_CHR_VAL = {
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

    private int bitBuffer = 0;
    private int bitsLeft = 0;
    private boolean eof = false;
    private InputStream inputStream; // שימוש ב-InputStream הכללי

    private class HuffNode {
        int value = -1; HuffNode left, right;
    }

    private HuffNode dcLumTree, acLumTree, dcChrTree, acChrTree;

    public F5JpegReader(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.quantizedCoefficients = new ArrayList<>();
        dcLumTree = buildTree(DC_LUM_BITS, DC_LUM_VAL);
        acLumTree = buildTree(AC_LUM_BITS, AC_LUM_VAL);
        dcChrTree = buildTree(DC_CHR_BITS, DC_CHR_VAL);
        acChrTree = buildTree(AC_CHR_BITS, AC_CHR_VAL);
        readAndDecode();
    }

    private HuffNode buildTree(int[] bits, int[] val) {
        HuffNode root = new HuffNode();
        int valIdx = 0; int code = 0;
        for (int length = 1; length <= 16; length++) {
            for (int i = 0; i < bits[length - 1]; i++) {
                insertCode(root, code, length, val[valIdx++]);
                code++;
            }
            code <<= 1;
        }
        return root;
    }

    private void insertCode(HuffNode root, int code, int length, int value) {
        HuffNode curr = root;
        for (int i = length - 1; i >= 0; i--) {
            int bit = (code >> i) & 1;
            if (bit == 0) {
                if (curr.left == null) curr.left = new HuffNode();
                curr = curr.left;
            } else {
                if (curr.right == null) curr.right = new HuffNode();
                curr = curr.right;
            }
        }
        curr.value = value;
    }

    private int nextBit() throws IOException {
        if (bitsLeft == 0) {
            int b = inputStream.read(); // שינוי ל-inputStream
            if (b == -1) { eof = true; return 0; }
            if (b == 0xFF) {
                int next = inputStream.read(); // שינוי ל-inputStream
                if (next != 0x00) { eof = true; return 0; } // Marker
            }
            bitBuffer = b; bitsLeft = 8;
        }
        bitsLeft--;
        return (bitBuffer >> bitsLeft) & 1;
    }

    private int readBits(int count) throws IOException {
        int res = 0;
        for (int i = 0; i < count; i++) res = (res << 1) | nextBit();
        return res;
    }

    private int decodeSymbol(HuffNode root) throws IOException {
        HuffNode curr = root;
        while (curr.left != null || curr.right != null) {
            int bit = nextBit();
            if (eof) return 0;
            if (bit == 0) curr = curr.left; else curr = curr.right;
        }
        return curr.value;
    }

    private int extend(int v, int t) {
        int vt = 1 << (t - 1);
        if (v < vt) { vt = (-1 << t) + 1; v += vt; }
        return v;
    }

    private void readAndDecode() throws IOException {
        // הסרתי את השורה שפותחת FileInputStream חדש על File שלא קיים
        int prev = 0, curr;
        while ((curr = inputStream.read()) != -1) { // שימוש ב-inputStream
            if (prev == 0xFF && curr == 0xDA) { // SOS
                int len = (inputStream.read() << 8) | inputStream.read();
                for (int i = 0; i < len - 2; i++) inputStream.read();
                break;
            }
            prev = curr;
        }

        int[] lastDc = new int[3];
        int channel = 0; // 0=Y, 1=Cb, 2=Cr

        while (!eof) {
            int[] block = new int[64];
            HuffNode dcTree = (channel == 0) ? dcLumTree : dcChrTree;
            HuffNode acTree = (channel == 0) ? acLumTree : acChrTree;
            
            // קידוד DC
            int t = decodeSymbol(dcTree);
            if (eof) break;
            int diff = extend(readBits(t), t);
            lastDc[channel] += diff;
            block[0] = lastDc[channel];

            // קידוד AC
            int k = 1;
            while (k < 64) {
                int rs = decodeSymbol(acTree);
                if (eof) break;
                int r = rs >> 4; int s = rs & 15;
                if (s == 0) {
                    if (r != 15) break; // EOB
                    k += 16;            // ZRL
                } else {
                    k += r;
                    block[k] = extend(readBits(s), s);
                    k++;
                }
            }
            if (eof) break;

            for (int i = 0; i < 64; i++) quantizedCoefficients.add(block[i]);

            // מעבר בין ערוצי הצבע Y -> Cb -> Cr
            channel = (channel + 1) % 3;
        }
        // inputStream ייסגר על ידי מי שקרא לסרוויס, לא סוגרים אותו כאן באמצע
    }

    public List<Integer> getQuantizedCoefficients() {
        return quantizedCoefficients;
    }
}