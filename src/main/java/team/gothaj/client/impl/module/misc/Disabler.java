package team.gothaj.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Disabler extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", "Spectate", "Spectate", "Polar Boat", "Polar TimeOut");

    private final BooleanProperty blockS08 = new BooleanProperty("Block S08", false);

    private final LinkedHashSet<PacketUtil.DelayedPacket> packets = new LinkedHashSet<>();
    private boolean delayPackets;

    private final List<Packet<INetHandlerPlayClient>> bufferedPackets = new ArrayList<>();
    private boolean buffering;
    private Entity targetEntity;
    private boolean desyncActive;

    public Disabler() {
        super("Disabler", "Disables few checks on anti-cheat", ModuleCategory.MISC);
        this.registerProperties(mode, blockS08);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Listen
    public void onWorldLoad(WorldChangeEvent event) {
        if (mode.is("Polar TimeOut")) {

            GothajClient.getInstance().getNotificationManager().add("Polar Fly", "Please wait 5 seconds before flying to avoid bans.", Notification.Type.WARNING);
        }
    }

    @Listen
    public void onPacket(PacketEvent event) {

        if (mode.is("Polar TimeOut")) return;

        if (mode.is("Polar Boat")) {
            onNetanyahuPacket(event);
            return;
        }

        if (mc.thePlayer.ticksExisted < 20) {
            packets.clear();
            return;
        }

        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                if (mc.thePlayer.capabilities.allowFlying && !delayPackets) {
                    delayPackets = true;
                } else if (delayPackets) {
                    packets.add(new PacketUtil.DelayedPacket(event.getPacket(), System.currentTimeMillis()));
                    GothajClient.getInstance().getNotificationManager().add("Disabler", "Flag detected!", Notification.Type.WARNING);
                }
            }

            if (delayPackets) {
                if (event.getPacket() instanceof S32PacketConfirmTransaction packet) {
                    packets.add(new PacketUtil.DelayedPacket(packet, System.currentTimeMillis()));
                    event.setCancelled(true);

                    PacketUtil.sendSilent(new C0FPacketConfirmTransaction(0, (short) 0, true));
                }
            }
        }

    }

    @Listen
    public void onNormalUpdate(NormalUpdateEvent event) {
        if (mode.is("Polar TimeOut")) return;
        if (!mode.is("Polar Boat")) return;
        if (!buffering || targetEntity == null) return;

        boolean boatDisappeared = targetEntity.isDead || !mc.theWorld.loadedEntityList.contains(targetEntity);

        if (boatDisappeared) {
            desyncActive = true;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mode.is("Polar TimeOut")) return;
        if (mode.is("Polar Boat")) {
            startBoatDesync();
        }
    }

    @Override
    public void onDisable() {
        if (mode.is("Polar TimeOut")) {
            super.onDisable();
            return;
        }
        if (mode.is("Polar Boat")) {
            desyncActive = false;
            buffering = false;
            bufferedPackets.clear();
            targetEntity = null;
        } else {
            for (PacketUtil.DelayedPacket packet : packets) {
                PacketUtil.receiveNoEvent(packet.packet());
            }
            packets.clear();
            delayPackets = false;
        }
        super.onDisable();
    }

    private void startBoatDesync() {
        bufferedPackets.clear();
        buffering = false;
        targetEntity = null;
        desyncActive = false;

        if (mc.thePlayer == null || mc.thePlayer.ridingEntity == null) {
            Logger.clientChat(EnumChatFormatting.DARK_PURPLE + "Polar Boat" + EnumChatFormatting.GRAY + " Movement Disabler");
            Logger.clientChat(EnumChatFormatting.GRAY + "1. " + EnumChatFormatting.WHITE + "Get into a boat");
            Logger.clientChat(EnumChatFormatting.GRAY + "2. " + EnumChatFormatting.WHITE + "Turn on this module");
            Logger.clientChat(EnumChatFormatting.GRAY + "3. " + EnumChatFormatting.WHITE + "Wait for the boat to break");
            Logger.clientChat(EnumChatFormatting.GRAY + "4. " + EnumChatFormatting.WHITE + "Enjoy the full movement disabler.");
            Logger.clientChat(EnumChatFormatting.GRAY + "5. " + EnumChatFormatting.RED + "(Patched as of 2026.08.04)");
            setToggled(false);
            return;
        }

        targetEntity = mc.thePlayer.ridingEntity;

        for (int i = 0; i < 5; i++) {
            PacketUtil.send(new C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.ATTACK));
        }

        buffering = true;
    }

    private void onNetanyahuPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;

        if (desyncActive && event.getPacket() instanceof S08PacketPlayerPosLook) {
            if (blockS08.getProperty()) {
                event.setCancelled(true);
            }
            return;
        }

        if (buffering && event.getPacket() instanceof S32PacketConfirmTransaction packet) {
            event.setCancelled(true);
            bufferedPackets.add(packet);
        }
    }
}
