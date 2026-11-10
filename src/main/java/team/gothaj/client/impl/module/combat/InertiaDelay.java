package team.gothaj.client.impl.module.combat;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InertiaDelay extends Module {


    public KillAura killAura;

    public final NumberProperty delay = new NumberProperty("Delay", 400, 0, 1500, 10);

    public final NumberProperty chance = new NumberProperty("Lag Chance", 100, 1, 100, 1);

    private final ConcurrentLinkedQueue<PacketUtil.DelayedPacket> velocityPackets = new ConcurrentLinkedQueue<>();

    private volatile boolean isDelaying = false;

    public InertiaDelay() {
        super("InertiaDelay", "Delays velocity momentum of the player", ModuleCategory.COMBAT);
        registerProperties(delay, chance);
    }


    @Override
    public String getSuffix() {
        return delay.getProperty().intValue() + "ms";
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (killAura == null || !killAura.isToggled() || killAura.target == null) {
            if (!velocityPackets.isEmpty()) {
                Logger.debugChat(this, "Flushing " + velocityPackets.size() + " packets on disable");
                Iterator<PacketUtil.DelayedPacket> it = velocityPackets.iterator();
                while (it.hasNext()) {
                    PacketUtil.receiveNoEvent(it.next().packet());
                    it.remove();
                }
            }
            isDelaying = false;
            return;
        }

        int targetId = killAura.target.getEntityId();
        Packet<?> pkt = event.getPacket();

        if (pkt instanceof S12PacketEntityVelocity velPkt) {
            if (velPkt.getEntityID() == targetId && Math.random() <= (chance.getProperty() / 100.0)) {
                isDelaying = true;
                velocityPackets.add(new PacketUtil.DelayedPacket(velPkt, System.currentTimeMillis()));
                event.setCancelled(true);
                Logger.debugChat(this, "S12 velocity captured for entity " + targetId + " | queue size: " + velocityPackets.size());
            }
        }

        if (isDelaying) {
            if (pkt instanceof S14PacketEntity movePkt && movePkt.entityId == targetId) {
                velocityPackets.add(new PacketUtil.DelayedPacket(movePkt, System.currentTimeMillis()));
                event.setCancelled(true);
                Logger.debugChat(this, "S14 move frozen for entity " + targetId + " | queue size: " + velocityPackets.size());
            } else if (pkt instanceof S18PacketEntityTeleport tpPkt && tpPkt.getEntityId() == targetId) {
                velocityPackets.add(new PacketUtil.DelayedPacket(tpPkt, System.currentTimeMillis()));
                event.setCancelled(true);
                Logger.debugChat(this, "S18 teleport frozen for entity " + targetId + " | queue size: " + velocityPackets.size());
            }
        }
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isDelaying || velocityPackets.isEmpty()) return;

        long firstTimestamp = velocityPackets.peek().delay();
        long elapsed = System.currentTimeMillis() - firstTimestamp;

        Logger.debugChat(this, "Waiting... elapsed: " + elapsed + "ms / " + delay.getProperty() + "ms | queued: " + velocityPackets.size());

        if (elapsed >= delay.getProperty().longValue()) {
            Logger.debugChat(this, "Releasing " + velocityPackets.size() + " packets after " + elapsed + "ms");
            Iterator<PacketUtil.DelayedPacket> it = velocityPackets.iterator();
            while (it.hasNext()) {
                PacketUtil.receiveNoEvent(it.next().packet());
                it.remove();
            }
            isDelaying = false;
            Logger.debugChat(this, "Delay window closed, queue cleared");
        }
    }

    @Override
    public void onEnable() {
        if (killAura == null) killAura = GothajClient.getInstance().getModuleManager().getAndCast(KillAura.class);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!velocityPackets.isEmpty()) {
            Logger.debugChat(this, "Flushing " + velocityPackets.size() + " packets on disable");
            Iterator<PacketUtil.DelayedPacket> it = velocityPackets.iterator();
            while (it.hasNext()) {
                PacketUtil.receiveNoEvent(it.next().packet());
                it.remove();
            }
        }
        isDelaying = false;
        super.onDisable();
    }

}
