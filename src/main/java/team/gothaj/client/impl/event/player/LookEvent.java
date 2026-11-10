package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

// this basically just updates the mouse object over :pray:.
@Getter
@Setter
@AllArgsConstructor
public class LookEvent extends Event {
    public float yaw, pitch;
}
