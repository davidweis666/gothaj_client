package team.gothaj.client.api.config;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.config.ConfigManager.Type;
import team.gothaj.client.api.util.render.AvatarUtil;
import team.gothaj.client.main.GothajClient;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;

@Getter
public class Config {

    private final String profile;
    private final Type type;
    @Setter
    private String author, uid, url, saveDate;

    public Config(String profile) {
        this.profile = profile;
        this.type = Type.LOCAL;
        this.uid = GothajClient.getInstance().getUser().getUid();
        this.author = GothajClient.getInstance().getUser().getUsername();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
        this.saveDate = dateFormat.format(new Date());
    }

    public Config(String profile, Type type) {
        this.profile = profile;
        this.type = type;
        this.uid = GothajClient.getInstance().getUser().getUid();
        this.author = GothajClient.getInstance().getUser().getUsername();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
        this.saveDate = dateFormat.format(new Date());
    }

    public Config(String profile, String author, String saveDate, Type type) {
        this.profile = profile;
        this.uid = AvatarUtil.getUID(author);
        this.type = type;
        this.author = author;
        this.saveDate = saveDate;
    }

    public Config(String profile, String author, String url, String saveDate) {
        this.profile = profile;
        this.uid = AvatarUtil.getUID(author);
        this.type = Type.ONLINE;
        this.author = author;
        this.url = url;
        this.saveDate = saveDate;
    }

    public boolean isOnline() {
        return type == Type.ONLINE;
    }

    public boolean isLocal() {
        return type == Type.LOCAL;
    }
}
