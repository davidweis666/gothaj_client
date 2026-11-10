package net.minecraft.util;

import net.minecraft.client.settings.GameSettings;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.main.GothajClient;

public class MovementInputFromOptions extends MovementInput
{
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn)
    {
        this.gameSettings = gameSettingsIn;
    }

    @Override
    public void updatePlayerMoveState() {
        boolean fwd = gameSettings.keyBindForward.isKeyDown();
        boolean back = gameSettings.keyBindBack.isKeyDown();
        boolean left = gameSettings.keyBindLeft.isKeyDown();
        boolean right = gameSettings.keyBindRight.isKeyDown();
        boolean jumpK = gameSettings.keyBindJump.isKeyDown();
        boolean sneakK = gameSettings.keyBindSneak.isKeyDown();

        float forward = 0f, strafe = 0f;
        if (fwd)  forward += 1f;
        if (back) forward -= 1f;
        if (left) strafe  += 1f;
        if (right)strafe  -= 1f;

        realMoveForward = forward;
        realMoveStrafe = strafe;

        MoveInputEvent evt = new MoveInputEvent(forward, strafe, jumpK, sneakK,
                0.3D,
                fwd, back, left, right, jumpK, sneakK
        );

        GothajClient.getInstance().getEventBus().fire(evt);

        evt.recomputeFromKeys();

        this.moveForward = evt.getForward();
        this.moveStrafe  = evt.getStrafe();
        this.jump = evt.isJump();
        this.sneak = evt.isSneak();

        if (this.sneak) {
            this.moveStrafe  *= (float) evt.getSlowdown();
            this.moveForward *= (float) evt.getSlowdown();
        }
    }
}
