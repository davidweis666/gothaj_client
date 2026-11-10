package team.gothaj.client.impl.module.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author vajzii
 * @since 2025
 */
public class FakeLag extends Module {


    private final NumberProperty delay = new NumberProperty("Delay",  500, 1, 2000, 1);

    private final List<Packet<?>> packetQueue = new CopyOnWriteArrayList<>();
    private EntityOtherPlayerMP selfDupe;

    private final TimerUtil packetTimer = new TimerUtil();

    public FakeLag() {
        super("FakeLag", "Makes your ping look higher", ModuleCategory.PLAYER);
        registerProperties(delay);
    }

    @Override
    public String getSuffix() {
        return delay.getProperty().intValue() + "ms";
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.SEND && !this.packetQueue.contains(event.getPacket()) && !(event.getPacket() instanceof C02PacketUseEntity) && !(event.getPacket() instanceof C01PacketChatMessage)) {
            this.packetQueue.add(event.getPacket());
            event.setCancelled(true);
        }

    }

    @Listen
    public void onRender(RenderDimensionEvent event) {
        if (mc.theWorld != null) {
            if (!this.packetQueue.isEmpty() && mc.thePlayer != null) {
                renderLag();
            }
        }
    }


    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null) {
            this.packetTimer.reset();
            releasePackets();
            if (selfDupe != null) {
                this.selfDupe = null;
            }
        }

        if (getCastedModule(KillAura.class).isToggled() && KillAura.target != null && mc.thePlayer.getDistanceToEntity(KillAura.target) <= 4.5) {
            setToggled(false);
            GothajClient.getInstance().getNotificationManager().add("Module",
                    "FakeLag disabled due to close target",
                    Notification.Type.WARNING
            );
        }


        if (packetTimer.hasTimeReached(delay.getProperty().longValue())) {
            releasePackets();
            packetTimer.reset();
        }


    }

    @Override
    public void onEnable() {
        if (getCastedModule(KillAura.class).isToggled()) {
            if (KillAura.target != null) {
                toggle();
            }
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        this.packetTimer.reset();

        if (mc.thePlayer != null) {
            releasePackets();
        }
        if (selfDupe != null) {
            this.selfDupe = null;
        }
        super.onDisable();
    }

    private void releasePackets() {
        if (!packetQueue.isEmpty()) {
            this.packetQueue.forEach(mc.thePlayer.sendQueue::addToSendQueue);
            this.packetQueue.clear();
        }

    }
    private void renderLag() {
        if (mc.thePlayer != null) {

            C03PacketPlayer latestPosPacket = null;

            for (Packet<?> packet : this.packetQueue) {
                if (packet instanceof C03PacketPlayer) {
                    if (latestPosPacket == null) {
                    latestPosPacket = (C03PacketPlayer) packet;
                }
                    }
            }
            selfDupe = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(EntityPlayer.getUUID(mc.thePlayer.getGameProfile()), "realPos!"));
            if (latestPosPacket != null) {
                selfDupe.setLocationAndAngles(latestPosPacket.getPositionX(), latestPosPacket.getPositionY(), latestPosPacket.getPositionZ(), latestPosPacket.getYaw(), latestPosPacket.getPitch());
                selfDupe.setEntityId(-42069);
                if (mc.gameSettings.thirdPersonView > 0) {
                    RenderUtil.drawEntityBox(selfDupe, ColorUtil.setAlpha(Hud.getColor(5), 80));
                }
            }

        }
    }
}
