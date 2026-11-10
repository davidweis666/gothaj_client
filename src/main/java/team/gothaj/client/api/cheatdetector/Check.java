package team.gothaj.client.api.cheatdetector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.impl.event.network.PacketEvent;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
public abstract class Check implements Utils {

    private final String name;
    private final int minVL;
    private final int maxVL;
    @Setter
    private String reason;

    public Check(String name, int minVL, int maxVL) {
        this.name = name;
        this.minVL = minVL;
        this.maxVL = maxVL;
    }


    public abstract boolean checkPlayer(EntityPlayer player);

    public void onPacket(PacketEvent event) {}

}
