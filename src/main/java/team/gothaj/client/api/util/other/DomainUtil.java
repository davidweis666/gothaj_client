package team.gothaj.client.api.util.other;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DomainUtil {

    public static String resolve(String hostOrDomain) {
        try {
            InetAddress inetAddress = InetAddress.getByName(hostOrDomain);
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            if (e instanceof UnknownHostException) return null;
            e.printStackTrace();
            return null;
        }
    }
}