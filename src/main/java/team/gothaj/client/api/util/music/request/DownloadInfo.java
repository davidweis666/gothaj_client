package team.gothaj.client.api.util.music.request;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class DownloadInfo {
    private String url;
    private String file;
    @JsonProperty("completed_in")
    private double completedIn;
    private String downloader;
    private String format;
}
