package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;
import java.util.List;

/**
 * @author vajzii
 * @since 2025
 */
public class TabGUIWidget extends Widget {

    private int selected = 0;

    private float currentSelectedY = 0;

    public TabGUIWidget() {
        super("TabGUI", 7, 60, 80, (Hud.tabGuiOpt.getOptions().size() * 14));
    }

    @Override
    public void onRender(ScaledResolution sr) {

        List<String> activeCategories = Hud.tabGuiOpt.getProperty();

        if (activeCategories.isEmpty()) return;

        int catHeight = 14;
        this.height = (activeCategories.size() * catHeight);

        if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
            RoundedUtil.drawRound((float) x, (float) y, (float) width, (float) height, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
        }

        float targetY =  (selected * catHeight);

        currentSelectedY = RenderUtil.animate(targetY, currentSelectedY, 0.35f);

        float renderY = (float) y + currentSelectedY;

        int maxIndex = activeCategories.size() - 1;
        boolean isFirst = (selected == 0);
        boolean isLast = (selected == maxIndex);

        float radiusTop = isFirst ? 6f : 0f;
        float radiusBottom = isLast ? 6f : 0f;

        Color primary = Hud.primaryColor.getProperty().brighter();
        Color primaryRGB = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 120);
        Color secondary = Hud.secondaryColor.getProperty().brighter();
        Color secondaryRGB = new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 120);

        RoundedUtil.drawGradientRound(
                (float) x - 1, renderY - 1, (float) width + 2, (float) catHeight + 2,
                radiusBottom, radiusBottom,
                radiusTop, radiusTop,
                primaryRGB, secondaryRGB
        );

        int i = 0;
        for (String cat : activeCategories) {

            float textY = (float) (y + (i * catHeight)) + (catHeight / 2f) - 3;

            float targetX = (float) (x + 5) + (selected != i ? 2 : 0);
            float currentX = (float) (x + 5) + (selected == i ? 2 : 0);

            currentX = RenderUtil.animate(targetX, currentX, 0.35f);

            Fonts.nunito17.drawStringWithShadow(cat, currentX, textY, -1);
            i++;
        }

        super.onRender(sr);
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        RoundedUtil.drawShaderRound((float) x, (float) y, (float) width, (float) height, 6f, Color.BLACK);
        super.onShader(event);
    }

    @Override
    public void onInput(InputEvent event) {
        if (event.isKey()) {
            List<String> activeCategories = Hud.tabGuiOpt.getProperty();
            int maxIndex = activeCategories.size() - 1;

            if (event.getCode() == Keyboard.KEY_DOWN && selected < maxIndex) {
                selected++;
            } else if (event.getCode() == Keyboard.KEY_UP && selected > 0) {
                selected--;
            }
        }
        super.onInput(event);
    }
}
