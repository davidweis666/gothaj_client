package team.gothaj.client.impl.event.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

@AllArgsConstructor
@Getter
@Setter
public class JumpEvent extends Event {
    private float jumpMotion;
}
