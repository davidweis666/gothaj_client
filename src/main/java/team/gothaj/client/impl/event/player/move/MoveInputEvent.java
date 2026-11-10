package team.gothaj.client.impl.event.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class MoveInputEvent extends Event {
    private float forward;
    private float strafe;
    private boolean jump;
    private boolean sneak;
    private double slowdown;

    private boolean keyForward;
    private boolean keyBack;
    private boolean keyLeft;
    private boolean keyRight;
    private boolean keyJump;
    private boolean keySneak;

    public void recomputeFromKeys() {
        float f = 0f, s = 0f;
        if (keyForward) f += 1f;
        if (keyBack)    f -= 1f;
        if (keyLeft)    s += 1f;
        if (keyRight)   s -= 1f;
        this.forward = f;
        this.strafe  = s;
        this.jump    = keyJump;
        this.sneak   = keySneak;
    }
}
