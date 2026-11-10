package team.gothaj.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.network.play.server.*;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

public class AntiCrash extends Module {

    public AntiCrash() {
        super("AntiCrash", "", ModuleCategory.MISC);
    }

    @Listen
    public void onTick(TickEvent ev) {

        if (mc.theWorld != null && mc.thePlayer != null) {
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityDragon && mc.thePlayer.getEntityWorld().provider.getDimensionId() != 1) {
                    mc.theWorld.removeEntity(e);
                }
            }
        }
    }

    @Listen
    public void onPacket(PacketEvent event) {

        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {

            if (event.getPacket() instanceof S0FPacketSpawnMob packet) {
                if (packet.getEntityType() == 63 && mc.theWorld.provider.getDimensionId() != 1) {
                    event.setCancelled(true);
                }
            }
            if (event.getPacket() instanceof S2APacketParticles packet) {
                if (Double.isNaN(packet.getXCoordinate()) || Double.isInfinite(packet.getXCoordinate())
                        || Double.isNaN(packet.getYCoordinate()) || Double.isInfinite(packet.getYCoordinate())
                        || Double.isNaN(packet.getZCoordinate()) || Double.isInfinite(packet.getZCoordinate())
                        || Float.isNaN(packet.getXOffset()) || Float.isInfinite(packet.getXOffset())
                        || Float.isNaN(packet.getYOffset()) || Float.isInfinite(packet.getYOffset())
                        || Float.isNaN(packet.getZOffset()) || Float.isInfinite(packet.getZOffset())
                        || Float.isNaN(packet.getParticleSpeed()) || Float.isInfinite(packet.getParticleSpeed())
                        || packet.getParticleCount() < 0 || packet.getParticleCount() > 10000) {
                    event.setCancelled(true);
                }
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook packet) {
                if (Double.isNaN(packet.x) || Double.isNaN(packet.y) || Double.isNaN(packet.z) ||
                        Double.isInfinite(packet.x) || Double.isInfinite(packet.y) || Double.isInfinite(packet.z)) {
                    event.setCancelled(true);
                }

                if (Float.isNaN(packet.yaw) || Float.isNaN(packet.pitch) ||
                        Float.isInfinite(packet.yaw) || Float.isInfinite(packet.pitch)) {
                    event.setCancelled(true);
                }

                if (Math.abs(packet.x) > 30000000 || Math.abs(packet.y) > 30000000 || Math.abs(packet.z) > 30000000) {
                    event.setCancelled(true);
                }

                if (packet.y < -1000 || packet.y > 1000) {
                    event.setCancelled(true);
                }
            }
            if (event.getPacket() instanceof S2BPacketChangeGameState state) {
                if (state.getGameState() >= 10 || state.getGameState() == 5 || state.getGameState() == 7) {
                    event.setCancelled(true);
                }
            }
            if (event.getPacket() instanceof S27PacketExplosion packet) {
                if (packet.getStrength() > 50 || packet.getStrength() == Float.MAX_VALUE
                        || packet.getX() == Double.MAX_VALUE || packet.getY() == Double.MAX_VALUE
                        || packet.getZ() == Double.MAX_VALUE || packet.func_149149_c() == Double.MAX_VALUE
                        || packet.func_149144_d() == Double.MAX_VALUE || packet.func_149147_e() == Double.MAX_VALUE) {
                    event.setCancelled(true);
                }
            } else if (event.getPacket() instanceof S02PacketChat chat) {
                String txt = chat.getChatComponent().getUnformattedText();
                if (txt.contains("+++ YOUR PC IS DEAD +++")
                        || txt.contains("§r§4Your PC is DEAD")
                        || txt.contains("§4CHEATER!")
                        || txt.contains("CHEATER!")
                        || txt.matches(".*CHEATER!.*CHEATER!.*CHEATER!.*")) {

                    mc.ingameGUI.setDisplayedTitle("");
                    mc.ingameGUI.setRecordPlaying("");
                    mc.ingameGUI.setDisplayedSubTitle("");
                    event.setCancelled(true);

                }
            } else if (event.getPacket() instanceof S29PacketSoundEffect sound) {
                if (sound.getSoundName().equalsIgnoreCase("mob.guardian.curse")
                        || sound.getSoundName().equalsIgnoreCase("mob.bat.death")
                        || sound.getSoundName().equalsIgnoreCase("mob.cat.meow")
                        || sound.getSoundName().equalsIgnoreCase("mob.endermen.stare")
                        || sound.getSoundName().equalsIgnoreCase("mob.enderdragon.end")
                        || sound.getVolume() > 4.0f) {
                    event.setCancelled(true);
                }

            }
        }
    }
}
