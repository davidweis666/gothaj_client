package ru.constantdynamic.hpke.misc;

public class Arrays {
    private Arrays() {
    }

    public static void clear(byte[] data) {
        if (null != data) {
            java.util.Arrays.fill(data, (byte) 0x00);
        }
    }

    public static boolean areAllZeroes(byte[] buf, int off, int len) {
        int bits = 0;
        for (int i = 0; i < len; ++i) {
            bits |= buf[off + i];
        }
        return bits == 0;
    }

    public static byte[] clone(byte[] data) {
        return null == data ? null : data.clone();
    }

    public static boolean areEqual(byte[] a, byte[] b) {
        return java.util.Arrays.equals(a, b);
    }

    public static boolean segmentsOverlap(int aOff, int aLen, int bOff, int bLen) {
        return aLen > 0
                && bLen > 0
                && aOff - bOff < bLen
                && bOff - aOff < aLen;
    }

    public static boolean constantTimeAreEqual(int len, byte[] a, int aOff, byte[] b, int bOff) {
        if (null == a) {
            throw new NullPointerException("'a' cannot be null");
        }
        if (null == b) {
            throw new NullPointerException("'b' cannot be null");
        }
        if (len < 0) {
            throw new IllegalArgumentException("'len' cannot be negative");
        }
        if (aOff > (a.length - len)) {
            throw new IndexOutOfBoundsException("'aOff' value invalid for specified length");
        }
        if (bOff > (b.length - len)) {
            throw new IndexOutOfBoundsException("'bOff' value invalid for specified length");
        }

        int d = 0;
        for (int i = 0; i < len; ++i) {
            d |= (a[aOff + i] ^ b[bOff + i]);
        }
        return 0 == d;
    }

    public static byte[] reverse(byte[] a) {
        if (a == null) {
            return null;
        }

        int p1 = 0, p2 = a.length;
        byte[] result = new byte[p2];

        while (--p2 >= 0) {
            result[p2] = a[p1++];
        }

        return result;
    }

    public static int[] reverse(int[] a) {
        if (a == null) {
            return null;
        }

        int p1 = 0, p2 = a.length;
        int[] result = new int[p2];

        while (--p2 >= 0) {
            result[p2] = a[p1++];
        }

        return result;
    }

    public static void reverse(byte[] input, byte[] output) {
        int last = input.length - 1;
        for (int i = 0; i <= last; ++i) {
            output[i] = input[last - i];
        }
    }
}
