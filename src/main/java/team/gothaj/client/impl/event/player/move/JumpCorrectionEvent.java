package team.gothaj.client.impl.event.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class JumpCorrectionEvent extends Event {
    private float yaw;
}
