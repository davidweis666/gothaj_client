package team.gothaj.client.impl.module.visual;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class KillEffect extends Module {

    private boolean isDead;

    private EntityPlayer target;

    public final ModeProperty effect = new ModeProperty("Effect", "Blood", "Blood");

    public KillEffect() {
        super("KillEffect", "", ModuleCategory.VISUAL);
        registerProperties(effect);
    }

    @Listen
    public void onTick(TickEvent event) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityPlayer player) {
            target = player;
        }
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player instanceof EntityPlayerSP) continue;

                if (target == player && target != null) {
                    if (target.deathTime == 1 || isDead) {
                        BlockPos pos = new BlockPos(target.posX, target.getEntityBoundingBox().minY + target.getEyeHeight(), target.posZ);
                        IBlockState blockState = Blocks.redstone_block.getDefaultState();
                        mc.renderGlobal.playAuxSFX(null, 2001, pos, Block.getStateId(blockState));
                        isDead = false;
                    }
                } else {
                    isDead = false;
                }
        }
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (Hud.widgets.getOptionState("Session Info")) {
            if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
                if (event.getPacket() instanceof S02PacketChat packet) {
                    if (packet.getChatComponent().getUnformattedText().contains("You killed") || packet.getChatComponent().getUnformattedText().contains("by " + mc.session.getUsername())) {
                        isDead = true;
                    }
                }
            }
        }
    }
}
