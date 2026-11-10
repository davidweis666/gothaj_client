package team.gothaj.client.impl.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.config.ConfigMenu;
import team.gothaj.client.impl.gui.theme.SelectThemeMenu;

import java.awt.*;
import java.io.IOException;

public abstract class ClientClickGUI extends GuiScreen {


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        ScaledResolution scaledResolution = new ScaledResolution(mc);

        Fonts.nunito16.drawStringWithShadow("-> Config Menu" , scaledResolution.getScaledWidth() - Fonts.nunito16.getStringWidth("-> Config Menu") - 5, scaledResolution.getScaledHeight() / 2f, new Color(128, 128, 128, 220));
        Fonts.nunito16.drawStringWithShadow("<- Themes" , 2, scaledResolution.getScaledHeight() / 2f, new Color(128, 128, 128, 220));

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {

        if (keyCode == Keyboard.KEY_RIGHT) {
            mc.displayGuiScreen(new ConfigMenu());
        }

        if (keyCode == Keyboard.KEY_LEFT) {
            mc.displayGuiScreen(new SelectThemeMenu());
        }

        super.keyTyped(typedChar, keyCode);
    }
}
