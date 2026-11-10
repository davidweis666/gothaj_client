package team.gothaj.client.impl.cheatdetector.misc;

import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.other.cheatdetector.NameAnalysis;

/**
 * @author vajzii
 * @since 2025
 */
public class InvalidNameCheck extends Check implements Utils {

    public InvalidNameCheck() {
        super("InvalidName", 0, 0);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        NameAnalysis nameAnalysis = NameAnalysis.analyzePlayerName(player.getName());

        if (nameAnalysis.isUnderscoreName()) {
            return true;
        } else return nameAnalysis.isContainsCurrentMonth();
    }
}
