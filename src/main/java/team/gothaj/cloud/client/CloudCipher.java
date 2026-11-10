package team.gothaj.cloud.client;

import team.gothaj.cloud.share.cipher.impl.HPKECipher;
import java.util.Base64;

public class CloudCipher extends HPKECipher {
    private static final String SERVER_PUBLIC_KEY_B64 = "mkzWa4ikoYDoxsNrhTrX9tSEP9ZRpc1GIj2HCrsQURc=";

    public CloudCipher() {
        super(false, Base64.getDecoder().decode(SERVER_PUBLIC_KEY_B64));
    }
}
