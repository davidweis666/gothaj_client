package team.gothaj.cloud.share.cipher.impl;

import ru.constantdynamic.hpke.crypto.digest.impl.SHA256Digest;
import ru.constantdynamic.hpke.crypto.generator.impl.HKDFBytesGenerator;
import ru.constantdynamic.hpke.crypto.mode.impl.ChaCha20;
import ru.constantdynamic.hpke.crypto.param.impl.HKDFParameters;
import ru.constantdynamic.hpke.crypto.param.impl.X25519PrivateKeyParameters;
import ru.constantdynamic.hpke.crypto.param.impl.X25519PublicKeyParameters;
import team.gothaj.cloud.share.cipher.Cipher;

import java.security.SecureRandom;
import java.util.Arrays;

public class HPKECipher implements Cipher {
    public static final byte[] NONCE_Info = "GothajCloudV2HPKE".getBytes();
    public static final int KEY_SIZE = 32;
    public static final int NONCE_SIZE = 12;

    public final boolean isServer;
    public final byte[] staticKey; // Server Private Key or Client's Target Server Public Key

    public boolean initialized = false;
    public byte[] encapsulatedKey; // Only used by client to store for first packet prepending

    // Session State
    public byte[] sendKey;
    public byte[] sendBaseNonce;
    public long sendSequence = 0;

    public byte[] recvKey;
    public byte[] recvBaseNonce;
    public long recvSequence = 0;

    public final ChaCha20 encryptor;
    public final ChaCha20 decryptor;

    /**
     * @param isServer  true if acting as server, false if acting as client
     * @param staticKey If server: Server's Private Key. If client: Server's Public Key.
     */
    public HPKECipher(boolean isServer, byte[] staticKey) {
        this.isServer = isServer;
        this.staticKey = staticKey;
        this.encryptor = new ChaCha20();
        this.decryptor = new ChaCha20();
    }

    public void setupSession(byte[] sharedSecret, byte[] contextInfo) {
        // HKDF to derive keys
        // We need 88 bytes: Key (32) + Nonce (12) for C2S, Key (32) + Nonce (12) for S2C
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(sharedSecret, null, contextInfo)); // Salt is null (optional)

        byte[] keyMaterial = new byte[88];
        hkdf.generateBytes(keyMaterial, 0, keyMaterial.length);

        byte[] c2sKey = Arrays.copyOfRange(keyMaterial, 0, 32);
        byte[] c2sNonce = Arrays.copyOfRange(keyMaterial, 32, 44);
        byte[] s2cKey = Arrays.copyOfRange(keyMaterial, 44, 76);
        byte[] s2cNonce = Arrays.copyOfRange(keyMaterial, 76, 88);

        if (isServer) {
            this.recvKey = c2sKey;
            this.recvBaseNonce = c2sNonce;
            this.sendKey = s2cKey;
            this.sendBaseNonce = s2cNonce;
        } else {
            this.sendKey = c2sKey;
            this.sendBaseNonce = c2sNonce;
            this.recvKey = s2cKey;
            this.recvBaseNonce = s2cNonce;
        }
        
        this.initialized = true;
    }

    public byte[] xorNonce(byte[] baseNonce, long sequence) {
        byte[] nonce = Arrays.copyOf(baseNonce, baseNonce.length);
        // XOR the sequence into the last 8 bytes of the nonce (big endian usually, strictly following RFC9180)
        // RFC 9180: The nonce is constructed by XORing the sequence number with the base nonce.
        // We'll write the long into the last 8 bytes.
        for (int i = 0; i < 8; i++) {
            nonce[baseNonce.length - 8 + i] ^= (byte) ((sequence >>> (8 * (7 - i))) & 0xFF);
        }
        return nonce;
    }

    @Override
    public synchronized byte[] encrypt(byte[] data) {
        throw new UnsupportedOperationException("Method inlined");
        /*
        if (!initialized) {
            if (isServer) {
                // Server cannot encrypt before handbook handshake completion (receiving first packet)
                throw new IllegalStateException("Server cannot send data before handshake completion");
            } else {
                // Client initialization
                initializeClient();
            }
        }

        // Prepare Cipher
        byte[] nonce = xorNonce(sendBaseNonce, sendSequence);
        encryptor.init(true, new ParametersWithIV(new KeyParameter(sendKey), nonce));
        sendSequence++;

        // Encrypt
        byte[] output = new byte[encryptor.getOutputSize(data.length)];
        int len = encryptor.processByte(data, 0, data.length, output, 0);
        try {
            encryptor.doFinal(output, len);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }

        // If client and this is the very first packet (sequence was 0 before increment, now 1)
        // Wait, if I just initialized, encapsulatedKey is set.
        if (!isServer && encapsulatedKey != null) {
            byte[] result = new byte[encapsulatedKey.length + output.length];
            System.arraycopy(encapsulatedKey, 0, result, 0, encapsulatedKey.length);
            System.arraycopy(output, 0, result, encapsulatedKey.length, output.length);
            encapsulatedKey = null; // Clear it, subsequent packets are raw
            return result;
        }

        return output;
        */
    }

    @Override
    public synchronized byte[] decrypt(byte[] data) {
        throw new UnsupportedOperationException("Method inlined");
        /*
        if (!initialized) {
            if (isServer) {
                initializeServer(data);
                // Adjust data to point to payload only
                data = Arrays.copyOfRange(data, 32, data.length);
            } else {
                throw new IllegalStateException("Client received data before sending handshake");
            }
        }

        // Prepare Cipher
        byte[] nonce = xorNonce(recvBaseNonce, recvSequence);
        decryptor.init(false, new ParametersWithIV(new KeyParameter(recvKey), nonce));
        recvSequence++;

        // Decrypt
        byte[] output = new byte[decryptor.getOutputSize(data.length)];
        int len = decryptor.processByte(data, 0, data.length, output, 0);
        try {
            decryptor.doFinal(output, len);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }

        return output;
        */
    }

    public void initializeClient() {
        SecureRandom random = new SecureRandom();
        X25519PrivateKeyParameters clientPrivate = new X25519PrivateKeyParameters(random);
        X25519PublicKeyParameters serverPublic = new X25519PublicKeyParameters(staticKey);
        
        byte[] sharedSecret = new byte[X25519PrivateKeyParameters.SECRET_SIZE];
        clientPrivate.generateSecret(serverPublic, sharedSecret, 0);

        byte[] enc = clientPrivate.generatePublicKey().getEncoded();
        this.encapsulatedKey = enc;

        // Context info: "GothajCloudV2HPKE" + enc + serverPublic
        byte[] context = new byte[NONCE_Info.length + enc.length + staticKey.length];
        System.arraycopy(NONCE_Info, 0, context, 0, NONCE_Info.length);
        System.arraycopy(enc, 0, context, NONCE_Info.length, enc.length);
        System.arraycopy(staticKey, 0, context, NONCE_Info.length + enc.length, staticKey.length);

        setupSession(sharedSecret, context);
    }

    public void initializeServer(byte[] data) {
        if (data.length < 32) {
            throw new IllegalArgumentException("Handshake packet too short");
        }
        byte[] enc = Arrays.copyOfRange(data, 0, 32);
        
        X25519PublicKeyParameters clientPublic = new X25519PublicKeyParameters(enc);
        X25519PrivateKeyParameters serverPrivate = new X25519PrivateKeyParameters(staticKey);
        
        byte[] sharedSecret = new byte[X25519PrivateKeyParameters.SECRET_SIZE];
        serverPrivate.generateSecret(clientPublic, sharedSecret, 0);

        // Reconstruct context info exactly as client did
        // Context info: "GothajCloudV2HPKE" + enc + serverPublic (which is my staticKey public part)
        // Wait, staticKey is Private Key for server!
        // We need to derive the public key from the private key.
        byte[] serverPublicBytes = serverPrivate.generatePublicKey().getEncoded();

        byte[] context = new byte[NONCE_Info.length + enc.length + serverPublicBytes.length];
        System.arraycopy(NONCE_Info, 0, context, 0, NONCE_Info.length);
        System.arraycopy(enc, 0, context, NONCE_Info.length, enc.length);
        System.arraycopy(serverPublicBytes, 0, context, NONCE_Info.length + enc.length, serverPublicBytes.length);

        setupSession(sharedSecret, context);
    }
}
