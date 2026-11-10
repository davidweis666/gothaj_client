package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.component.SessionComponent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.util.*;

import static java.awt.Color.WHITE;

/**
 * @author horizon, vajzii
 * @since 2025
 */
public class SessionInfoWidget extends Widget {
    private final Map<String, String> items = new LinkedHashMap<>();

    public SessionInfoWidget() {
        super("Session Info", 8, 51, 120, 50);
    }

    @Override
    public void onRender(ScaledResolution sr) {

        SessionComponent sessionComponent = GothajClient.getInstance().getComponentManager().getAndCast(SessionComponent.class);

        final IFontRenderer font = Fonts.nunito14;

        float iconSize = 11;
        float iconPadding = 2;
        float padding = 5;
        float topPadding = 3;

        double calculatedHeight = topPadding;
        double maxWidth = 0;

        long totalSeconds = sessionComponent.getPlayTime() / 20;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        items.put("Session Time", String.format("%02d:%02d:%02d", hours, minutes, seconds));
        items.put("Username", mc.thePlayer.getName());
        items.put("Kills", String.valueOf(sessionComponent.getKills()));
        items.put("Deaths", String.valueOf(sessionComponent.getDeaths()));

        for (final Map.Entry<String, String> entry : items.entrySet()) {

            double labelWidth = font.getStringWidth(entry.getKey() + ": ");
            double valueWidth = font.getStringWidth(entry.getValue());
            double textWidth = labelWidth + valueWidth + iconSize + iconPadding + padding * 2;
            maxWidth = Math.max(maxWidth, textWidth);
            calculatedHeight += font.getHeight() + padding + 1;
        }

        height = Math.max((float) (calculatedHeight + 16.5f), 36.5f);
        width = Math.max((float) maxWidth, 120);

        double yPos = y + 16.5f;
        double xPos = x;

        if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
            RoundedUtil.drawRound((float) x, (float) y, (float) width, (float) height, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));

        RoundedUtil.drawGradientLine((float) (x + 5), (float) (y + 16), (float) (width - 10), 0.7F, 1, Color.GRAY, Color.GRAY);

        Fonts.nunito18.drawCenteredStringWithShadow("Session Info", (float) (x + width / 2f), (float) (y + 5f), -1);

        yPos += topPadding;

        for (final Map.Entry<String, String> entry : items.entrySet()) {
            float baseX = (float) (xPos + iconSize + iconPadding + padding);
            float baseY = (float) yPos + 3.5f;

            String label = entry.getKey() + ": ";
            font.drawString(label, baseX, baseY, WHITE.getRGB(), true);

            float labelWidth = font.getStringWidth(label);
            font.drawString("§7" + entry.getValue(), baseX + labelWidth, baseY, WHITE.getRGB(), true);

            yPos += font.getHeight() + padding;
        }

        super.onRender(sr);
    }


    @Override
    public void onShader(ShaderOverlayEvent event) {
        final Color backgroundColor = Color.BLACK;
        RoundedUtil.drawShaderRound((float) x, (float) y, (float) width, (float) height, 6f, backgroundColor);
        super.onShader(event);
    }
}
