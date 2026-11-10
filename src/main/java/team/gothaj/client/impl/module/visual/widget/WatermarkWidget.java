package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.AvatarUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static java.lang.Integer.MIN_VALUE;

/**
 * @author vajzii
 * @since 2025
 */
public class WatermarkWidget extends Widget {

    public WatermarkWidget() {
        super("Watermark", MIN_VALUE, MIN_VALUE, 80, 30);
    }

    @Override
    public void onRender(ScaledResolution sr) {

//        if (mc.currentScreen instanceof GuiChat) return;

        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;
        if (initPosition) {
            x = 8;
            y = 8;
        }

        float y = this.y;

        switch (Hud.wmMode.getProperty()) {
            case "Modern" -> {
                final String clientName = GothajClient.getInstance().getName();
                final String clientVersion = GothajClient.getInstance().getVersion();
                String nameVer = clientName + clientVersion;
                float nameVerWidth = Fonts.nunito16Bold.getStringWidth(nameVer);

                float xPos = (float)x;

                final Color backgroundColor = new Color(20,20,20, Hud.backgroundAlpha.getProperty().intValue());

                RoundedUtil.drawRound(xPos, (float) y, nameVerWidth, 12, 4, backgroundColor);
                Fonts.nunito16Bold.drawStringWithShadow(clientName, xPos + 3f, (float) y + 3.5F, new Color(240, 240, 240));
                Fonts.nunito14.drawStringWithShadow(clientVersion, xPos + 5f + Fonts.nunito16Bold.getStringWidth(clientName), (float) y + 4.2F, new Color(200, 200, 200));
                xPos += nameVerWidth + 4;

                final String ping = (mc.getCurrentServerData() == null ? 0 : mc.getCurrentServerData().pingToServer) + " ms";
                float pingWidth = Fonts.nunito14.getStringWidth(ping);
                RoundedUtil.drawRound(xPos, (float) y, pingWidth + 6, 12, 4, backgroundColor);
                Fonts.nunito14.drawStringWithShadow(ping, xPos + 3f, (float)y + 4, new Color(190, 190, 190));
                xPos += pingWidth + 6 + 4;

                String mcUsername = mc.getSession().getUsername();
                float mcUsernameWidth = Fonts.nunito14.getStringWidth(mcUsername);
                RoundedUtil.drawRound(xPos, (float) y, mcUsernameWidth + 6.5F, 12, 4, backgroundColor);
                Fonts.nunito14.drawStringWithShadow(mcUsername, xPos + 3f, (float)y + 4, new Color(190, 190, 190));

                width = (int) (xPos + mcUsernameWidth + 6.5F - x);

                y += 16;

                AvatarUtil.drawForumAvatar((float) x, (float) y, 12, 12, 4, 1f);

                String name = GothajClient.getInstance().getUser().getUsername();
                float nameWidth = Fonts.nunito14.getStringWidth(name);

                RoundedUtil.drawRound((float) x + 16, (float) y, nameWidth + 5, 12, 4, backgroundColor);
                Fonts.nunito14.drawStringWithShadow(name, (float) (x + 19f), (float) y + 4, new Color(240, 240, 240));

                String uid = "#" + GothajClient.getInstance().getUser().getUidFormatted();
                float uidWidth = Fonts.nunito14.getStringWidth(uid);

                RoundedUtil.drawRound((float) x + 25 + nameWidth, (float) y, uidWidth + 5, 12, 4, backgroundColor);
                Fonts.nunito14.drawStringWithShadow(uid, (float) (x + 27.5F + nameWidth), (float) y + 4,  new Color(190, 190, 190));
            }

            case "Best" -> {
                final String clientName = GothajClient.getInstance().getName();
                final String clientVersion = "v4.0.0";
                final int gray = new Color(170, 170, 170).getRGB();
                final boolean hudColorMode = Hud.wmBestColorMode.is("HUD Color");
                final Color hudColor = hudColorMode ? new Color(Hud.getColor(0)) : Color.WHITE;
                final float hueStep = 0.06f;

                float padX = 4f;
                float padTop = 3f;
                float textHeight = mc.fontRendererObj.FONT_HEIGHT;
                float lineGap = 2f;
                float lineHeight = 1.5f;
                float padBottom = 3f;

                float nameWidth = mc.fontRendererObj.getStringWidth(clientName);
                float versionWidth = mc.fontRendererObj.getStringWidth(clientVersion);
                float spaceWidth = mc.fontRendererObj.getCharWidth(' ');
                float boxWidth = nameWidth + spaceWidth + versionWidth + padX * 2f;
                float boxHeight = padTop + textHeight + lineGap + lineHeight + padBottom;

                RenderUtil.drawRect((float) x, (float) y, boxWidth, boxHeight, new Color(0, 0, 0, 150));

                float drawX = (float) x + padX;
                char[] nameChars = clientName.toCharArray();
                for (int i = 0; i < nameChars.length; i++) {
                    Color accent = hudColorMode ? hudColor : bestRainbow(i * hueStep);
                    mc.fontRendererObj.drawString(String.valueOf(nameChars[i]), drawX, (float) y + padTop, accent.getRGB(), true);
                    drawX += mc.fontRendererObj.getCharWidth(nameChars[i]);
                }

                mc.fontRendererObj.drawStringWithShadow(clientVersion, drawX + spaceWidth, (float) y + padTop, gray);

                float lineX = (float) x + padX;
                float lineY = (float) y + padTop + textHeight + lineGap;
                float lineW = boxWidth - padX * 2f;
                if (hudColorMode) {
                    RenderUtil.drawRect(lineX, lineY, lineW, lineHeight, hudColor);
                } else {
                    float totalSpread = nameChars.length * hueStep;
                    for (float lx = lineX; lx < lineX + lineW; ) {
                        float t = (lx - lineX) / lineW;
                        Color accent = bestRainbow(t * totalSpread);
                        float seg = Math.min(2f, lineX + lineW - lx);
                        RenderUtil.drawRect(lx, lineY, seg, lineHeight, accent);
                        lx += seg;
                    }
                }

                width = boxWidth;
                height = boxHeight;
            }

            case "Text" -> {
                String clientName = Hud.wmText.getProperty();

                Color color = Hud.wmOptText.getOptionState("Sync Color") ? new Color(Hud.getColor(5)) : new Color(Hud.getColorWatermark(clientName.length()));

                if (clientName.contains("$TIME")) {
                    LocalTime now = LocalTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    String timeStr = now.format(formatter);

                    clientName = clientName.replace("$TIME", timeStr);
                }

                if (clientName.contains("$DATE")) {
                    LocalDate now = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
                    String dateStr = now.format(formatter);

                    clientName = clientName.replace("$DATE", dateStr);
                }

                if (clientName.contains("$PING")) {
                    String serverStr = mc.getCurrentServerData().pingToServer + " ms";

                    clientName = clientName.replace("$PING", serverStr);
                }

                if (clientName.contains("$SERVER")) {
                    String serverStr = mc.getCurrentServerData().serverIP;

                    clientName = clientName.replace("$SERVER", serverStr);
                }

                if (clientName.contains("$IGN")) {
                    String ignStr = mc.thePlayer.getName();

                    clientName = clientName.replace("$IGN", ignStr);
                }

                if (clientName.contains("$USERNAME")) {
                    String userStr = GothajClient.getInstance().getUser().getUsername();

                    clientName = clientName.replace("$USERNAME", userStr);
                }

                if (Hud.wmOptText.getOptionState("Horizontal Color")) {
                    if (Hud.wmOptText.getOptionState("Custom Font")) {
                        FontUtil.drawGradientWatermark(Hud.wmFont.getProperty(), clientName, (float) x, (float) y, clientName.length(),false);
                    } else {
                        FontUtil.drawGradientWatermark(mc.fontRendererObj, clientName, (float) x, (float) y, clientName.length(), false);
                    }

                } else {
                    if (Hud.wmOptText.getOptionState("Custom Font")) {
                       //TODO SkiaDraw.text(Hud.wmFont.getProperty(), clientName, (float) x, (float) y, color);
                    } else {
                        mc.fontRendererObj.drawStringWithShadow(clientName, (float) x, (float) y, color.getRGB());
                    }
                }

                this.width = Hud.wmOptText.getOptionState("Custom Font") ? Hud.wmFont.getProperty().getStringWidth(clientName) : mc.fontRendererObj.getStringWidth(clientName);
                this.height = 10;
            }

            case "Other" -> {

                //TODO:: Complete this style
                RenderUtil.drawRect(x, y, 150, 20, Color.BLACK.getRGB());

            }
        }



        super.onRender(sr);
    }


    @Override
    public void onShader(ShaderOverlayEvent event) {

//        if (mc.currentScreen instanceof GuiChat) return;

        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;
        if (initPosition) {
            x = 8;
            y = 8;
        }

        float y = this.y;

        switch (Hud.wmMode.getProperty()) {
            case "Modern" -> {
                final String clientName = GothajClient.getInstance().getName();
                final String clientVersion = GothajClient.getInstance().getVersion();
                String nameVer = clientName + clientVersion;
                float nameVerWidth = Fonts.nunito16Bold.getStringWidth(nameVer);

                float xPos = (float)x;

                final Color backgroundColor = Color.BLACK;

                RoundedUtil.drawShaderRound(xPos, (float) y, nameVerWidth, 12, 4, backgroundColor);
                xPos += nameVerWidth + 4;

                final String ping = (mc.getCurrentServerData() == null ? 0 : mc.getCurrentServerData().pingToServer) + " ms";
                float pingWidth = Fonts.nunito14.getStringWidth(ping);
                RoundedUtil.drawShaderRound(xPos, (float) y, pingWidth + 6, 12, 4, backgroundColor);
                xPos += pingWidth + 6 + 4;

                String mcUsername = mc.getSession().getUsername();
                float mcUsernameWidth = Fonts.nunito14.getStringWidth(mcUsername);
                RoundedUtil.drawShaderRound(xPos, (float) y, mcUsernameWidth + 6.5F, 12, 4, backgroundColor);

                width = (int) (xPos + mcUsernameWidth + 6.5F - x);

                y += 16;

                AvatarUtil.drawForumAvatar((float) x, (float) y, 12, 12, 4, 1f);

                String name = GothajClient.getInstance().getUser().getUsername();
                float nameWidth = Fonts.nunito14.getStringWidth(name);

                RoundedUtil.drawShaderRound((float) x + 16, (float) y, nameWidth + 5, 12, 4, backgroundColor);

                String uid = "#" + GothajClient.getInstance().getUser().getUidFormatted();
                float uidWidth = Fonts.nunito14.getStringWidth(uid);

                RoundedUtil.drawShaderRound((float) x + 25 + nameWidth, (float) y, uidWidth + 5, 12, 4, backgroundColor);
            }
        }


        super.onShader(event);
    }


    private Color bestRainbow(float index) {
        float t = (float) ((System.currentTimeMillis() % 6000L) / 6000.0D);
        float hue = (index - t) % 1.0f;
        if (hue < 0) hue += 1.0f;
        return Color.getHSBColor(hue, 0.85f, 1.0f);
    }


}
