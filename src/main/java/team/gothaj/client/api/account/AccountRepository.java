package team.gothaj.client.api.account;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.api.util.other.ExternalMicrosoftAuthenticator;
import team.gothaj.client.api.util.other.ExternalMicrosoftAuthenticator.AuthResult;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class AccountRepository extends ListManager<Account> {

    @Getter @Setter
    private Type currentType = Type.DEFAULT;

    private final File accountsFile = new File(GothajClient.getInstance().getClientDir() + File.separator + "accounts.json");
    private final File currentAccountFile = new File(GothajClient.getInstance().getClientDir() + File.separator + "current-account.txt");
    private final File currentAccountJsonFile = new File(GothajClient.getInstance().getClientDir() + File.separator + "current-account.json");

    @Getter
    private Account currentAccount;


    @Override
    public void init() {
        load();

        Account savedCurrent = findSavedCurrentAccount();
        if (savedCurrent != null) {
            setCurrentAccount(savedCurrent);
            refreshCurrentAccountIfNeeded();
        } else {
            Session currentSession = Minecraft.getMinecraft().session;

            if (currentSession != null) {
                currentAccount = new Account(currentSession.getUsername(), currentSession.getUsername().startsWith("Player"));
            }
        }
        super.init();
    }

    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(accountsFile)) {
            gson.toJson(items, writer);
            saveCurrentAccount();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
        applySession(currentAccount);
        saveCurrentAccount();
        save();
    }

    @Override
    public void remove(Account item) {
        super.remove(item);
        if (currentAccount == item || sameAccount(currentAccount, item)) {
            currentAccount = null;
            saveCurrentAccount();
        }
    }

    private Account findSavedCurrentAccount() {
        Account jsonAccount = findSavedCurrentAccountJson();
        if (jsonAccount != null)
            return jsonAccount;

        if (!currentAccountFile.exists())
            return null;

        try {
            String savedUuid = Files.readString(currentAccountFile.toPath(), StandardCharsets.UTF_8).trim();
            if (savedUuid.isEmpty()) {
                return null;
            }

            for (Account account : super.getItems()) {
                if (account.getUuid() != null && account.getUuid().toString().equalsIgnoreCase(savedUuid)) {
                    return account;
                }
            }
        } catch (IOException e) {
            Logger.log("Failed to load current account: " + e.getMessage());
        }

        return null;
    }

    private Account findSavedCurrentAccountJson() {
        if (!currentAccountJsonFile.exists())
            return null;

        try (Reader reader = new FileReader(currentAccountJsonFile)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            String uuid = readString(object, "uuid");
            String username = readString(object, "username");
            String email = readString(object, "email");
            boolean cracked = object.has("cracked") && object.get("cracked").getAsBoolean();

            for (Account account : super.getItems()) {
                if (matchesSavedAccount(account, uuid, username, email, cracked))
                    return account;
            }
        } catch (Exception e) {
            Logger.log("Failed to load current account json: " + e.getMessage());
        }

        return null;
    }

    private String readString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private boolean matchesSavedAccount(Account account, String uuid, String username, String email, boolean cracked) {
        if (account == null)
            return false;

        if (!uuid.isEmpty() && account.getUuid() != null
                && (account.getUuid().toString().equalsIgnoreCase(uuid)
                || account.getUuidString().equalsIgnoreCase(uuid.replace("-", "")))) {
            return true;
        }

        if (account.isCracked() == cracked && !username.isEmpty()
                && username.equalsIgnoreCase(account.getUsername())) {
            return true;
        }

        return !email.isEmpty() && account.getEmail() != null && email.equalsIgnoreCase(account.getEmail());
    }

    private boolean sameAccount(Account a, Account b) {
        if (a == null || b == null)
            return false;

        if (a.getUuid() != null && b.getUuid() != null && a.getUuid().equals(b.getUuid()))
            return true;

        if (a.getEmail() != null && b.getEmail() != null && a.getEmail().equalsIgnoreCase(b.getEmail()))
            return true;

        return a.isCracked() == b.isCracked() && a.getUsername() != null
                && a.getUsername().equalsIgnoreCase(b.getUsername());
    }

    private void saveCurrentAccount() {
        try {
            currentAccountFile.getParentFile().mkdirs();
            currentAccountJsonFile.getParentFile().mkdirs();
            String value = currentAccount != null && currentAccount.getUuid() != null ? currentAccount.getUuid().toString() : "";
            Files.writeString(currentAccountFile.toPath(), value, StandardCharsets.UTF_8);

            JsonObject object = new JsonObject();
            if (currentAccount != null) {
                object.addProperty("uuid", currentAccount.getUuid() != null ? currentAccount.getUuid().toString() : "");
                object.addProperty("username", currentAccount.getUsername() != null ? currentAccount.getUsername() : "");
                object.addProperty("email", currentAccount.getEmail() != null ? currentAccount.getEmail() : "");
                object.addProperty("cracked", currentAccount.isCracked());
            }
            Files.writeString(currentAccountJsonFile.toPath(),
                    new GsonBuilder().setPrettyPrinting().create().toJson(object), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.log("Failed to save current account: " + e.getMessage());
        }
    }

    private void refreshCurrentAccountIfNeeded() {
        if (currentAccount == null || currentAccount.isCracked() || currentAccount.getRefreshToken() == null
                || currentAccount.getRefreshToken().isEmpty()) {
            return;
        }

        if (!currentAccount.isInvalidCredentials() && !currentAccount.isExpired()
                && currentAccount.getAccessToken() != null && !currentAccount.getAccessToken().isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                AuthResult result = new ExternalMicrosoftAuthenticator().loginWithRefreshToken(currentAccount.getRefreshToken());
                currentAccount.setUsername(result.username());
                currentAccount.setUuid(result.uuid());
                currentAccount.setAccessToken(result.accessToken());
                currentAccount.setRefreshToken(result.refreshToken());
                currentAccount.setExpiresAt(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);
                currentAccount.markValid();
                applySession(currentAccount);
                save();
            } catch (Exception e) {
                currentAccount.markInvalid();
                save();
                Logger.log("Failed to refresh current account: " + e.getMessage());
            }
        }, "Gothaj-CurrentAccountRefresh").start();
    }

    private void applySession(Account account) {
        if (account == null) {
            return;
        }

        Minecraft.getMinecraft().session = new Session(
                account.getUsername(),
                account.getUuidString(),
                account.getAccessToken() != null ? account.getAccessToken() : "",
                account.getAccountType()
        );
    }

    public void load() {
        if (!accountsFile.exists()) {
            try {
                accountsFile.getParentFile().mkdirs();
                save();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            return;
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(accountsFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonArray()) {
                Account[] loaded = gson.fromJson(jsonElement, Account[].class);
                items.clear();
                if (loaded != null) {
                    Collections.addAll(items, loaded);
                }
            } else if (jsonElement.isJsonObject()) {
                Logger.log("accounts.json has wrong format (object instead of array). Resetting to empty array.");
                items.clear();
                save();
            } else {
                Logger.log("accounts.json has unknown format. Resetting to empty array.");
                items.clear();
                save();
            }
        } catch (JsonSyntaxException | JsonIOException e) {
            Logger.log("Failed to parse accounts.json: " + e.getMessage());
            Logger.log("Backing up corrupted file and creating new one.");
            
            try {
                File backup = new File(accountsFile.getAbsolutePath() + ".backup");
                if (accountsFile.renameTo(backup)) {
                    Logger.log("Corrupted file backed up to: " + backup.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }

            items.clear();
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Account> getItems() {
        return switch (getCurrentType()) {
            case DEFAULT -> super.getItems();
            case CRACKED -> super.getItems().stream()
                    .filter(Account::isCracked)
                    .collect(Collectors.toCollection(ArrayList::new));
            case PREMIUM -> super.getItems().stream()
                    .filter(account -> !account.isCracked())
                    .collect(Collectors.toCollection(ArrayList::new));
        };
    }



    public enum Type {
        DEFAULT, CRACKED, PREMIUM
    }
}
