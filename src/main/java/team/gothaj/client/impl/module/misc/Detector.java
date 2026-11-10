package team.gothaj.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

public class Detector extends Module {

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public Detector()
	{
        super("Detector", "", ModuleCategory.MISC);
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
            Entity entity;
            if (event.getPacket() instanceof S0BPacketAnimation packet && packet.getAnimationType() == 1) {
                entity = mc.theWorld.getEntityByID(packet.getEntityID());
            } else if (event.getPacket() instanceof S19PacketEntityStatus packet && packet.getOpCode() == 2) {
                entity = packet.getEntity(mc.theWorld);
            } else return;
            if (!(entity instanceof EntityLivingBase entityLivingBase)) return;
            if (entityLivingBase.hurtTime > 0) {
                Logger.debugChat(this,"Polar Mitigated");
            }
        }
    }
}
