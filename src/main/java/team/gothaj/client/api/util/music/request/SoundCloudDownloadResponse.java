package team.gothaj.client.api.util.music.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoundCloudDownloadResponse {
    private int code;
    private DownloadInfo msg;
}
