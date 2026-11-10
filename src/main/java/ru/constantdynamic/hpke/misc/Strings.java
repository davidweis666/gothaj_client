package ru.constantdynamic.hpke.misc;

public class Strings {
    public static byte[] toByteArray(char[] var0) {
        byte[] var1 = new byte[var0.length];

        for (int var2 = 0; var2 != var1.length; ++var2) {
            var1[var2] = (byte) var0[var2];
        }

        return var1;
    }

    public static byte[] toByteArray(String var0) {
        byte[] var1 = new byte[var0.length()];

        for (int var2 = 0; var2 != var1.length; ++var2) {
            char var3 = var0.charAt(var2);
            var1[var2] = (byte) var3;
        }

        return var1;
    }

    public static int toByteArray(String var0, byte[] var1, int var2) {
        int var3 = var0.length();

        for (int var4 = 0; var4 < var3; ++var4) {
            char var5 = var0.charAt(var4);
            var1[var2 + var4] = (byte) var5;
        }

        return var3;
    }
}
