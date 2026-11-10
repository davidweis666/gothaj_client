package team.gothaj.client.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.impl.module.visual.Hud;

public interface Utils {

    Minecraft mc = Minecraft.getMinecraft();

    ScaledResolution sr = new ScaledResolution(mc);

    Hud hud = Hud.module;


}
