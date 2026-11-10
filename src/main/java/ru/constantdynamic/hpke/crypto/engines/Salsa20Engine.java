package ru.constantdynamic.hpke.crypto.engines;

import ru.constantdynamic.hpke.crypto.cipher.SkippingStreamCipher;
import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.crypto.param.impl.KeyParameter;
import ru.constantdynamic.hpke.crypto.param.impl.ParametersWithIV;
import ru.constantdynamic.hpke.exceptions.DataLengthException;
import ru.constantdynamic.hpke.exceptions.MaxBytesExceededException;
import ru.constantdynamic.hpke.exceptions.OutputLengthException;
import ru.constantdynamic.hpke.misc.Integers;
import ru.constantdynamic.hpke.misc.Pack;
import ru.constantdynamic.hpke.misc.Strings;

public class Salsa20Engine implements SkippingStreamCipher {
    public final static int DEFAULT_ROUNDS = 20;
    private final static int STATE_SIZE = 16;
    private final static int[] TAU_SIGMA = Pack.littleEndianToInt(Strings.toByteArray("expand 16-byte k" + "expand 32-byte k"), 0, 8);

    protected int rounds;
    protected int[] engineState = new int[STATE_SIZE];
    protected int[] x = new int[STATE_SIZE];
    private int index;
    private final byte[] keyStream = new byte[STATE_SIZE * 4];
    private boolean initialised;

    private int cW0, cW1, cW2;

    public Salsa20Engine() {
        this(DEFAULT_ROUNDS);
    }

    public Salsa20Engine(final int rounds) {
        if (rounds <= 0 || (rounds & 1) != 0) {
            throw new IllegalArgumentException("'rounds' must be a positive, even number");
        }

        this.rounds = rounds;
    }

    public static void salsaCore(int rounds, int[] input, int[] x) {
        if (input.length != 16) {
            throw new IllegalArgumentException();
        }

        if (x.length != 16) {
            throw new IllegalArgumentException();
        }

        if (rounds % 2 != 0) {
            throw new IllegalArgumentException("Number of rounds must be even");
        }

        int x00 = input[0],
                x01 = input[1],
                x02 = input[2],
                x03 = input[3],
                x04 = input[4],
                x05 = input[5],
                x06 = input[6],
                x07 = input[7],
                x08 = input[8],
                x09 = input[9],
                x10 = input[10],
                x11 = input[11],
                x12 = input[12],
                x13 = input[13],
                x14 = input[14],
                x15 = input[15];

        for (int i = rounds; i > 0; i -= 2) {
            x04 ^= Integers.rotateLeft(x00 + x12, 7);
            x08 ^= Integers.rotateLeft(x04 + x00, 9);
            x12 ^= Integers.rotateLeft(x08 + x04, 13);
            x00 ^= Integers.rotateLeft(x12 + x08, 18);
            x09 ^= Integers.rotateLeft(x05 + x01, 7);
            x13 ^= Integers.rotateLeft(x09 + x05, 9);
            x01 ^= Integers.rotateLeft(x13 + x09, 13);
            x05 ^= Integers.rotateLeft(x01 + x13, 18);
            x14 ^= Integers.rotateLeft(x10 + x06, 7);
            x02 ^= Integers.rotateLeft(x14 + x10, 9);
            x06 ^= Integers.rotateLeft(x02 + x14, 13);
            x10 ^= Integers.rotateLeft(x06 + x02, 18);
            x03 ^= Integers.rotateLeft(x15 + x11, 7);
            x07 ^= Integers.rotateLeft(x03 + x15, 9);
            x11 ^= Integers.rotateLeft(x07 + x03, 13);
            x15 ^= Integers.rotateLeft(x11 + x07, 18);

            x01 ^= Integers.rotateLeft(x00 + x03, 7);
            x02 ^= Integers.rotateLeft(x01 + x00, 9);
            x03 ^= Integers.rotateLeft(x02 + x01, 13);
            x00 ^= Integers.rotateLeft(x03 + x02, 18);
            x06 ^= Integers.rotateLeft(x05 + x04, 7);
            x07 ^= Integers.rotateLeft(x06 + x05, 9);
            x04 ^= Integers.rotateLeft(x07 + x06, 13);
            x05 ^= Integers.rotateLeft(x04 + x07, 18);
            x11 ^= Integers.rotateLeft(x10 + x09, 7);
            x08 ^= Integers.rotateLeft(x11 + x10, 9);
            x09 ^= Integers.rotateLeft(x08 + x11, 13);
            x10 ^= Integers.rotateLeft(x09 + x08, 18);
            x12 ^= Integers.rotateLeft(x15 + x14, 7);
            x13 ^= Integers.rotateLeft(x12 + x15, 9);
            x14 ^= Integers.rotateLeft(x13 + x12, 13);
            x15 ^= Integers.rotateLeft(x14 + x13, 18);
        }

        x[0] = x00 + input[0];
        x[1] = x01 + input[1];
        x[2] = x02 + input[2];
        x[3] = x03 + input[3];
        x[4] = x04 + input[4];
        x[5] = x05 + input[5];
        x[6] = x06 + input[6];
        x[7] = x07 + input[7];
        x[8] = x08 + input[8];
        x[9] = x09 + input[9];
        x[10] = x10 + input[10];
        x[11] = x11 + input[11];
        x[12] = x12 + input[12];
        x[13] = x13 + input[13];
        x[14] = x14 + input[14];
        x[15] = x15 + input[15];
    }

    protected void packTauOrSigma(int keyLength, int[] state, int stateOffset) {
        int tsOff = (keyLength - 16) / 4;
        state[stateOffset] = TAU_SIGMA[tsOff];
        state[stateOffset + 1] = TAU_SIGMA[tsOff + 1];
        state[stateOffset + 2] = TAU_SIGMA[tsOff + 2];
        state[stateOffset + 3] = TAU_SIGMA[tsOff + 3];
    }

    @Override
    public long skip(long numberOfBytes) {
        if (numberOfBytes >= 0) {
            long remaining = numberOfBytes;

            if (remaining >= 64) {
                final long count = remaining / 64;
                this.advanceCounter(count);

                remaining -= count * 64;
            }

            final int oldIndex = this.index;
            this.index = (this.index + (int) remaining) & 63;

            if (this.index < oldIndex) {
                this.advanceCounter();
            }
        } else {
            long remaining = -numberOfBytes;

            if (remaining >= 64) {
                final long count = remaining / 64;
                this.retreatCounter(count);

                remaining -= count * 64;
            }

            for (long i = 0; i < remaining; i++) {
                if (this.index == 0) {
                    retreatCounter();
                }

                this.index = (this.index - 1) & 63;
            }
        }

        generateKeyStream(keyStream);
        return numberOfBytes;
    }

    @Override
    public long seekTo(long position) {
        this.reset();
        return this.skip(position);
    }

    @Override
    public long getPosition() {
        return this.getCounter() * 64 + index;
    }

    @Override
    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
        if (!(params instanceof ParametersWithIV parametersWithIV)) {
            throw new IllegalArgumentException("Init parameters must include an IV.");
        }

        byte[] iv = parametersWithIV.getIv();
        if (iv == null || iv.length != getNonceSize()) {
            throw new IllegalArgumentException("Salsa20 requires exactly " + getNonceSize() + " bytes of IV.");
        }

        final CipherParameters keyParam = parametersWithIV.getParameters();
        if (keyParam == null) {
            if (!initialised) {
                throw new IllegalStateException("KeyParameter can not be null for first initialisation.");
            }

            this.setKey(null, iv);
        } else if (keyParam instanceof KeyParameter keyParameter) {
            // Pretty unsafe, not checking for constraints.
            byte[] key = keyParameter.getKey();
            this.setKey(key, iv);
        } else {
            throw new IllegalArgumentException("Salsa20 init parameters must contain a KeyParameter (or null for re-init).");
        }

        this.reset();
        this.initialised = true;
    }

    @Override
    public byte returnByte(byte in) {
        if (limitExceeded()) {
            throw new MaxBytesExceededException("2^70 byte limit per IV; Change IV.");
        }

        byte out = (byte) (keyStream[index] ^ in);
        index = (index + 1) & 63;

        if (index == 0) {
            this.advanceCounter();
            this.generateKeyStream(keyStream);
        }

        return out;
    }

    @Override
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
        if (!initialised) {
            throw new IllegalStateException("Not initialised");
        }

        if ((inOff + len) > in.length) {
            throw new DataLengthException("Input buffer too short");
        }

        if ((outOff + len) > out.length) {
            throw new OutputLengthException("Output buffer too short");
        }

        if (limitExceeded(len)) {
            throw new MaxBytesExceededException("2^70 byte limit per IV would be exceeded; Change IV");
        }

        for (int i = 0; i < len; i++) {
            out[i + outOff] = (byte) (keyStream[index] ^ in[i + inOff]);
            index = (index + 1) & 63;

            if (index == 0) {
                this.advanceCounter();
                this.generateKeyStream(keyStream);
            }
        }

        return len;
    }

    @Override
    public void reset() {
        index = 0;
        cW0 = 0;
        cW1 = 0;
        cW2 = 0;
        this.resetCounter();

        this.generateKeyStream(keyStream);
    }

    protected void resetCounter() {
        engineState[8] = engineState[9] = 0;
    }

    private boolean limitExceeded() {
        if (++cW0 == 0) {
            if (++cW1 == 0) {
                return (++cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
            }
        }

        return false;
    }

    private boolean limitExceeded(int len) {
        cW0 += len;
        if (cW0 < len && cW0 >= 0) {
            if (++cW1 == 0) {
                return (++cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
            }
        }

        return false;
    }

    protected int getNonceSize() {
        return 8;
    }

    protected long getCounter() {
        return ((long) engineState[9] << 32) | (engineState[8] & 0xffffffffL);
    }

    protected void setKey(byte[] keyBytes, byte[] ivBytes) {
        if (keyBytes != null) {
            if ((keyBytes.length != 16) && (keyBytes.length != 32)) {
                throw new IllegalArgumentException("Requires 128-bit or 256-bit key");
            }

            int tsOff = (keyBytes.length - 16) / 4;
            engineState[0] = TAU_SIGMA[tsOff];
            engineState[5] = TAU_SIGMA[tsOff + 1];
            engineState[10] = TAU_SIGMA[tsOff + 2];
            engineState[15] = TAU_SIGMA[tsOff + 3];

            // Key
            Pack.littleEndianToInt(keyBytes, 0, engineState, 1, 4);
            Pack.littleEndianToInt(keyBytes, keyBytes.length - 16, engineState, 11, 4);
        }

        // IV
        Pack.littleEndianToInt(ivBytes, 0, engineState, 6, 2);
    }

    protected void generateKeyStream(byte[] output) {
        salsaCore(rounds, engineState, x);
        Pack.intToLittleEndian(x, output, 0);
    }

    protected void retreatCounter(long diff) {
        int hi = (int) (diff >>> 32);
        int lo = (int) diff;

        if (hi != 0) {
            if ((engineState[9] & 0xffffffffL) >= (hi & 0xffffffffL)) {
                engineState[9] -= hi;
            } else {
                throw new IllegalStateException("Attempt to reduce counter past zero.");
            }
        }

        if ((engineState[8] & 0xffffffffL) >= (lo & 0xffffffffL)) {
            engineState[8] -= lo;
        } else {
            if (engineState[9] != 0) {
                --engineState[9];
                engineState[8] -= lo;
            } else {
                throw new IllegalStateException("Attempt to reduce counter past zero.");
            }
        }
    }

    protected void retreatCounter() {
        if (engineState[8] == 0 && engineState[9] == 0) {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }

        if (--engineState[8] == -1) {
            --engineState[9];
        }
    }

    protected void advanceCounter(long difference) {
        int hi = (int) (difference >>> 32);
        int lo = (int) difference;

        if (hi > 0) {
            engineState[9] += hi;
        }

        int oldState = engineState[8];

        engineState[8] += lo;

        if (oldState != 0 && engineState[8] < oldState) {
            engineState[9]++;
        }
    }

    protected void advanceCounter() {
        if (this.engineState[8]++ == 0) {
            this.engineState[9]++;
        }
    }
}
