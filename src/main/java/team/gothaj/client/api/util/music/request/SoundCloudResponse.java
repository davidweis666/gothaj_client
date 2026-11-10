package team.gothaj.client.api.util.music.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SoundCloudResponse {
    private int code;
    private List<SoundCloudTrack> msg;
}
