package team.gothaj.client.api.util.music.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SoundCloudTrack {
    private String id;
    private String title;
    private String uploader;
    private String url;
    private Integer duration;
    private boolean downloadable;
    private String thumbnail;
}
