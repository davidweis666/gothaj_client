package team.gothaj.client.impl.module.player;

import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.eventbus.Listen;

public class BalanceAbuse extends Module {

    private long tb = 0L;
    private long lastC03 = System.currentTimeMillis();

    private float timer = 1.0f;

    public BalanceAbuse() {
        super("BalanceAbuse", "Abuse TB for an advantage", ModuleCategory.PLAYER);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastC03 = System.currentTimeMillis();
        timer = 1.0f;
        tb = 0L;
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.timer.timerSpeed = 1.0f;
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        lastC03 = System.currentTimeMillis();
        timer = 1.0f;
    }

    @Listen
    public void onTick(TickEvent event) {
        timer = Math.max(timer, 0.1f);
        mc.timer.timerSpeed = timer;
    }

    @Listen
    public void onPacketSend(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.SEND) {
            if (event.getPacket() instanceof C03PacketPlayer) {
                var now = System.currentTimeMillis();
                var elapsed = now - lastC03;

                tb += (50 - elapsed);
                tb = Math.clamp(tb, -130L, 135L);
                lastC03 = now;

                if (tb <= 47L) {
                    timer = 1.5f;
                } else {
                    if (tb >= 130L) {
                        timer = 0.7f;
                    }
                    timer *= 0.75f;
                }
                if (mc.thePlayer.hurtTime == 1) {
                    timer = 0.85f;
                }
                if (mc.thePlayer.hurtTime >= 2 && mc.thePlayer.hurtTime <= 4) {
                    timer *= 1.3f;
                }
            } else if (event.getPacket() instanceof C02PacketUseEntity c) {
                if (c.getAction() == C02PacketUseEntity.Action.ATTACK)
                    timer = 3f;
            }
        }
    }
}