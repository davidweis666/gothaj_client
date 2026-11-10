package team.gothaj.client.api.account;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class Account {
    private String username;
    private UUID uuid;
    private String email;
    private String accessToken;
    private String refreshToken;
    private boolean cracked;
    private boolean favorite;
    private long timeAdded;
    private long expiresAt;
    private boolean invalidCredentials;
    private long playTime;

    /**
     * If expiration <= 0, ban is permanent.
     * If server IP absent or ban expired, user is not banned on that server.
     */
    private final Map<String, Long> bannedServers = new HashMap<>();

    public Account(String username, boolean cracked) {
        this.username = username;
        this.uuid = UUID.randomUUID();
        this.cracked = cracked;
        this.timeAdded = System.currentTimeMillis();
        this.expiresAt = -1;
        this.playTime = 0;
        markValid();
    }

    public Account(String username, UUID uuid, String email, String accessToken, String refreshToken, boolean cracked, long expiresAt) {
        this.username = username;
        this.uuid = uuid;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.cracked = cracked;
        this.timeAdded = System.currentTimeMillis();
        this.expiresAt = expiresAt;
        this.playTime = 0;
        markValid();
    }

    public Account(String username, UUID uuid, String email, String accessToken, String refreshToken, boolean cracked) {
        this.username = username;
        this.uuid = uuid;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.cracked = cracked;
        this.timeAdded = System.currentTimeMillis();
        this.expiresAt = System.currentTimeMillis() + 86400000L;
        this.playTime = 0;
        markValid();
    }

    public boolean isExpired() {
        return !cracked && expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public void markInvalid() {
        this.invalidCredentials = true;
    }

    public void markValid() {
        this.invalidCredentials = false;
    }

    /**
     * Ban this account on given server IP until expiresAt timestamp.
     * Pass expiresAt <= 0 for permanent ban.
     */
    public void banOnServer(String serverIp, long expiresAt) {
        bannedServers.put(serverIp, expiresAt);
    }

    /**
     * Unban this account from given server IP.
     */
    public void unbanOnServer(String serverIp) {
        bannedServers.remove(serverIp);
    }

    /**
     * Check if account is currently banned on a given server IP.
     * Expired bans are automatically cleaned up.
     */
    public boolean isBannedOn(String serverIp) {
        long now = System.currentTimeMillis();
        Long banExpiry = bannedServers.get(serverIp);
        if (banExpiry == null) return false;
        if (banExpiry <= 0) return true; // permanent ban
        if (banExpiry > now) return true; // ban valid
        // ban expired
        bannedServers.remove(serverIp);
        return false;
    }

    /**
     * Check if account is banned on any server.
     * Cleans expired bans before checking.
     */
    public boolean isBanned() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = bannedServers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Long expiry = entry.getValue();
            if (expiry > 0 && expiry <= now) {
                it.remove(); // remove expired ban
            }
        }
        return !bannedServers.isEmpty();
    }

    /**
     * Get the ban expiry timestamp for a given server IP, or null if not banned.
     * Returns expiry time in millis or <= 0 if permanent.
     */
    public Long getBanExpiryOnServer(String serverIp) {
        if (!isBannedOn(serverIp)) return null;
        return bannedServers.get(serverIp);
    }

    /**
     * Returns the UUID as a string without dashes, required by the Minecraft Session object.
     */
    public String getUuidString() {
        if (uuid.toString() == null) return "";
        if (uuid.toString().contains("-")) return uuid.toString().replace("-", "");
        return uuid.toString();
    }

    /**
     * Returns the session type. Microsoft accounts (standard for tokens) require "msa".
     */
    public String getAccountType() {
        return cracked ? "legacy" : "msa";
    }
}
