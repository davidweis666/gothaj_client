package team.gothaj.client.impl.module.visual;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.eventbus.Listen;

public class AntiInvisibles extends Module {


    public AntiInvisibles() {
        super("AntiInvisibles", "Prevents players from going invisible client-side", ModuleCategory.VISUAL);
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {

        for (EntityPlayer player : mc.theWorld.playerEntities) {

            if (!(player instanceof EntityOtherPlayerMP)) continue;
            
            if (player.isInvisible()) {
                player.setInvisible(false);
            }
        }
    }
}
