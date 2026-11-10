package ru.constantdynamic.hpke.crypto.digest;

import ru.constantdynamic.hpke.crypto.service.CryptoServiceProperties;
import ru.constantdynamic.hpke.crypto.service.CryptoServicePurpose;

public class Utils {
    static CryptoServiceProperties getDefaultProperties(Digest digest, CryptoServicePurpose purpose) {
        return new DefaultProperties(digest.getDigestSize() * 4, digest.getAlgorithmName(), purpose);
    }

    public static CryptoServiceProperties getDefaultProperties(Digest digest, int prfBitsOfSecurity, CryptoServicePurpose purpose) {
        return new DefaultPropertiesWithPRF(digest.getDigestSize() * 4, prfBitsOfSecurity, digest.getAlgorithmName(), purpose);
    }

    // Service Definitions
        private record DefaultPropertiesWithPRF(int bitsOfSecurity, int prfBitsOfSecurity, String algorithmName,
                                                CryptoServicePurpose purpose)
                implements CryptoServiceProperties {

        @Override
        public int bitsOfSecurity() {
                if (purpose == CryptoServicePurpose.PRF) {
                    return prfBitsOfSecurity;
                }

                return bitsOfSecurity;
            }

            public String getServiceName() {
                return algorithmName;
            }

            public Object getParams() {
                return null;
            }
        }

    // Service Definitions
        private record DefaultProperties(int bitsOfSecurity, String algorithmName, CryptoServicePurpose purpose)
                implements CryptoServiceProperties {

        public String getServiceName() {
                return algorithmName;
            }

            public Object getParams() {
                return null;
            }
        }
}
