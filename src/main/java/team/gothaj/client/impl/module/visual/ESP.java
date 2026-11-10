package team.gothaj.client.impl.module.visual;

import net.minecraft.client.Minecraft;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IRCUtil;
import javafx.stage.FileChooser;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.animation.Animation;
import team.gothaj.client.api.animation.Easing;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.other.FileUtil;
import team.gothaj.client.api.util.render.AvatarUtil;
import team.gothaj.client.api.util.render.GLUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.gif.GifUtil;
import team.gothaj.client.impl.event.player.SneakSpoofEvent;
import team.gothaj.client.impl.event.render.RenderNameTagEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.client.Targets;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class ESP extends Module {

    private final Map<EntityPlayer, float[]> entityMap = new HashMap<>();
    private final Map<String, PlayerIRCData> ircCache = new HashMap<>();
    private final Map<EntityPlayer, double[]> lastRenderPos = new HashMap<>();
    private final Map<EntityPlayer, Animation> healthAnimations = new HashMap<>();

    private ResourceLocation selectedFillImg;
    private final GifUtil espGif = new GifUtil();

    private final ListProperty elements = new ListProperty("Elements", "Box", "Tag", "Health", "Armor");

    private final BooleanProperty cornered = new BooleanProperty("Cornered", false).setVisible(() -> elements.getOptionState("Box"));

    private final BooleanProperty fillBox = new BooleanProperty("Fill Box", false).setVisible(() -> elements.getOptionState("Box"));

    private final ModeProperty fillMode = new ModeProperty("Fill Mode", "Color", "Color", "Hud", "Team", "Image").setVisible(() -> elements.getOptionState("Box") && fillBox.getProperty());

    public final ActionProperty selectImage = new ActionProperty("Browse Image")
            .setListener(() -> {
                File searchImage = FileUtil.openFileChooser(
                        null,
                        new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.gif")
                );

                if (searchImage == null || !searchImage.exists()) {
                    selectedFillImg = null;
                    espGif.dispose();
                    return;
                }

                String fileName = searchImage.getName().toLowerCase();

                if (fileName.endsWith(".gif")) {
                    if (selectedFillImg != null) {
                        mc.getTextureManager().deleteTexture(selectedFillImg);
                        selectedFillImg = null;
                    }
                    espGif.load(searchImage, "esp_gif", null);
                } else {
                    espGif.dispose();
                    new Thread(() -> {
                        BufferedImage img;
                        try {
                            img = ImageIO.read(searchImage);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                            return;
                        }
                        if (img == null) return;

                        mc.addScheduledTask(() -> {
                            selectedFillImg = RenderUtil.uploadBufferedImageAsTexture(img, searchImage.getName());
                        });
                    }, "ESP-Image-Loader").start();
                }

            }).setVisible(() -> fillMode.is("Image"));


    private final BooleanProperty gradientFill = new BooleanProperty("Gradient Fill", false).setVisible(() -> elements.getOptionState("Box") && fillBox.getProperty() && !fillMode.is("Image"));

    private final ColorProperty fillColor = new ColorProperty("Primary Fill Color", new Color(20, 20, 20, 120)).setVisible(() -> elements.getOptionState("Box") && fillBox.getProperty() && fillMode.is("Color"));

    private final ColorProperty fillColorS = new ColorProperty("Secondary Fill Color", new Color(120, 120, 120, 120)).setVisible(() -> elements.getOptionState("Box") && fillMode.is("Color") && gradientFill.getProperty() && fillBox.getProperty());

    private final NumberProperty fillAlpha = new NumberProperty("Fill Alpha", 120, 1, 255, 1).setVisible(() -> elements.getOptionState("Box") && fillBox.getProperty());

    private final NumberProperty tagScale = new NumberProperty("Tag Scale", 1.0f, 0.5f, 2.5f, 0.05f).setVisible(() -> elements.getOptionState("Tag"));

    private final BooleanProperty segmentHealth = new BooleanProperty("Health Segments", false).setVisible(() -> elements.getOptionState("Health"));

    private final BooleanProperty dynamicHealth = new BooleanProperty("Pulse Health", false).setVisible(() -> elements.getOptionState("Health"));

    private final ModeProperty healthMode = new ModeProperty("Health Mode", "Health", "Health", "Hud", "Custom", "Team").setVisible(() -> elements.getOptionState("Health"));

    private final BooleanProperty gradientCustom = new BooleanProperty("Custom Gradient", false).setVisible(() -> elements.getOptionState("Health") && (healthMode.is("Hud") || healthMode.is("Custom")));

    public final ColorProperty healthPrimaryColor = new ColorProperty("Health Primary Color", Color.magenta).setVisible(() -> elements.getOptionState("Health") && healthMode.is("Custom"));

    public final ColorProperty healthSecondaryColor = new ColorProperty("Health Secondary Color", Color.pink).setVisible(() -> elements.getOptionState("Health") && healthMode.is("Custom") && gradientCustom.getProperty());

    private final BooleanProperty segmentArmor = new BooleanProperty("Armor Segments", false).setVisible(() -> elements.getOptionState("Armor"));


    public ESP() {
        super("ESP", "", ModuleCategory.VISUAL);
        registerProperties(elements,
                cornered, fillBox, fillMode, selectImage, gradientFill, fillColor, fillColorS, fillAlpha,
                tagScale,
                healthMode, segmentHealth, dynamicHealth, gradientCustom, healthPrimaryColor, healthSecondaryColor,
                segmentArmor);
    }

    @Override
    public String getSuffix() {
        return "2D";
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        renderPlayers(true);
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (fillMode.is("Image") && espGif.isLoaded()) {
            espGif.update();
        }
        renderPlayers(false);
    }

    private void renderPlayers(boolean isShader) {
        List<Map.Entry<EntityPlayer, float[]>> renderList = new ArrayList<>();
        for (EntityPlayer player : entityMap.keySet()) {
            if ((player != mc.thePlayer && (mc.gameSettings.thirdPersonView == 0 && player.getDistanceToEntity(mc.thePlayer) < 1.0F)) ||
                    !RenderUtil.canRenderBB(player.getEntityBoundingBox()))
                continue;

            renderList.add(new AbstractMap.SimpleEntry<>(player, entityMap.get(player)));
        }

        renderList.sort(Comparator.comparingDouble(entry ->
                mc.thePlayer.getDistanceToEntity(entry.getKey())));

        for (int i = renderList.size() - 1; i >= 0; i--) {
            Map.Entry<EntityPlayer, float[]> entry = renderList.get(i);
            EntityPlayer player = entry.getKey();
            final float[] positions = entry.getValue();
            final float x = positions[0];
            final float y = positions[1];
            final float x2 = positions[2];
            final float y2 = positions[3];

            final float health = player.getHealth();
            final float maxHealth = player.getMaxHealth();
            final float healthPercentage = health / maxHealth;

            if (elements.getOptionState("Tag")) {
                renderTag(player, x, y, x2, y2, isShader);
            }
            if (elements.getOptionState("Armor")) {
                renderArmor(player, x, y, x2, y2, isShader);
            }
            if (elements.getOptionState("Health")) {
                renderHealth(player, x, y, x2, y2, healthPercentage, isShader);
            }
            if (elements.getOptionState("Box")) {
                renderBox(player, x, y, x2, y2, isShader);
            }
        }
    }


    private void renderTag(EntityPlayer player, float x, float y, float x2, float y2, boolean isShader) {
        final IFontRenderer fontRenderer = Fonts.nunito14;
        float scale = (float) tagScale.getProperty().doubleValue();

        float xMid = x + ((x2 - x) / 2);

        GlStateManager.pushMatrix();
        GlStateManager.translate(xMid, y, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-xMid, -y, 0);

        final String name = (IrcPlayer.getByIngameName(player.getGameProfile().getName()) != null ?
                IRCUtil.getIrcPrefix(player.getGameProfile().getName()) + player.getGameProfile().getName() :
                player.getGameProfile().getName()) + EnumChatFormatting.WHITE;

        final float xDif = x2 - x;
        final float middle = x + (xDif / 2);
        final float textHeight = fontRenderer.getHeight();

        float renderY = y - textHeight - 3;

        final float nameWidth = fontRenderer.getStringWidth(name);
        final String healthString = String.valueOf((int) Math.round(player.getHealth() + player.getAbsorptionAmount()));
        final float healthWidth = fontRenderer.getStringWidth(healthString) + Fonts.icons11.getStringWidth("h") + 1.5f;
        final String distanceString = Math.round(mc.thePlayer.getDistanceToEntity(player)) + "m";
        final float distanceWidth = fontRenderer.getStringWidth(distanceString);

        float tagWidth = (distanceWidth + 8f) + 3f + (nameWidth + 8f) + (healthWidth + 8f) + 3f;
        float tagLeft = middle - tagWidth / 2f;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();

        if (isShader) {
            RoundedUtil.drawShaderRound(tagLeft, renderY - 2, distanceWidth + 8f, textHeight + 4, 3, Color.BLACK);
            RoundedUtil.drawShaderRound(tagLeft + (distanceWidth + 8f) + 3f, renderY - 2, nameWidth + 8f, textHeight + 4, 3, Color.BLACK);
            RoundedUtil.drawShaderRound(tagLeft + (distanceWidth + 8f) + 3f + (nameWidth + 8f) + 3f, renderY - 2, healthWidth + 8f, textHeight + 4, 3, Color.BLACK);
        } else {
            RoundedUtil.drawRound(tagLeft, renderY - 2, distanceWidth + 8f, textHeight + 4, 3, new Color(20, 20, 20, 130));
            RoundedUtil.drawRound(tagLeft + (distanceWidth + 8f) + 3f, renderY - 2, nameWidth + 8f, textHeight + 4, 3, new Color(20, 20, 20, 130));
            RoundedUtil.drawRound(tagLeft + (distanceWidth + 8f) + 3f + (nameWidth + 8f) + 3f, renderY - 2, healthWidth + 8f, textHeight + 4, 3, new Color(20, 20, 20, 130));
        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        RenderUtil.resetColor();

        if (!isShader) {
            fontRenderer.drawString(distanceString, tagLeft + 4f, renderY + 0.5F, new Color(210, 210, 210).darker());
            fontRenderer.drawString(name, tagLeft + (distanceWidth + 8f) + 7f, renderY + 0.5F, ColorUtil.getTeamColor(player));

            float healthBoxLeft = tagLeft + (distanceWidth + 8f) + 3f + (nameWidth + 8f) + 3f;
            float healthBoxWidth = healthWidth + 8f;
            float healthBoxTop = renderY - 2;
            float healthBoxHeight = textHeight + 4;

            float healthBoxCenterX = healthBoxLeft + (healthBoxWidth / 2f);
            float healthBoxCenterY = healthBoxTop + (healthBoxHeight / 2f);

            float healthTextWidth = fontRenderer.getStringWidth(healthString);
            float iconWidth = Fonts.icons11.getStringWidth("h");
            float totalContentWidth = healthTextWidth + 1.5f + iconWidth;

            float healthStartX = healthBoxCenterX - (totalContentWidth / 2f);

            float healthTextY = healthBoxCenterY - (fontRenderer.getHeight() / 2f) + 0.5F;
            float iconTextY = healthBoxCenterY - (Fonts.icons11.getHeight() / 2f) + 0.5F;


            fontRenderer.drawString(healthString, healthStartX, healthTextY, ColorUtil.getColorFromPercentage(player.getHealth() / player.getMaxHealth()));
            Fonts.icons11.drawString("h", healthStartX + healthTextWidth + 1.5f, iconTextY, ColorUtil.getColorFromPercentage(player.getHealth() / player.getMaxHealth()));
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();

        PlayerIRCData ircData = ircCache.get(player.getGameProfile().getName());

        if (ircData == null) {
            IrcPlayer ircPlayerObj = IrcPlayer.getByIngameName(player.getGameProfile().getName());
            String ircName = ircPlayerObj != null ? ircPlayerObj.getIrcNick() : null;
            String uidString = null;
            if (ircName != null) {
                String normalUid = AvatarUtil.getUID(ircName);
                if (normalUid != null) {
                    uidString = normalUid;
                }
            }
            ircData = new PlayerIRCData(ircName, uidString);
            ircCache.put(player.getGameProfile().getName(), ircData);

        }

        float nameRectLeft = tagLeft + (distanceWidth + 8f) + 3f;
        float nameRectWidth = nameWidth + 8f;

        IrcPlayer ircPlayer = IrcPlayer.getByIngameName(player.getGameProfile().getName());

        if (ircData.uidString != null && ircPlayer != null && (!ircPlayer.isVanished() || ircPlayer.getIngameName().equals(mc.thePlayer.getGameProfile().getName())) && ircPlayer.getIrcNick().equals(ircData.ircName)) {

            float avatarBox = 12, gap = 3, padding = 8;
            float ircNameWidth = fontRenderer.getStringWidth(ircData.ircName);
            float uidWidth = fontRenderer.getStringWidth(ircData.uidString);

            float nameCenter = nameRectLeft + (nameRectWidth / 2);
            float ircNameBoxLeft = nameCenter - 5;

            float ircY = renderY - textHeight - 8;

            float avatarLeft = ircNameBoxLeft - avatarBox - gap - ircNameWidth / 2;

            if (isShader) {
                RoundedUtil.drawShaderRound(avatarLeft, ircY - 2, avatarBox, textHeight + 4, 3, Color.BLACK);
                RoundedUtil.drawShaderRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + padding, textHeight + 4, 3, Color.BLACK);
                RoundedUtil.drawShaderRound(ircNameBoxLeft + ircNameWidth / 2 + padding + gap, ircY - 2, uidWidth + padding, textHeight + 4, 3, Color.BLACK);
            } else {
                // RoundedUtil.drawRound(avatarLeft, ircY - 2, avatarBox, textHeight + 4, 3, new Color(20, 20, 20, 140));
                RoundedUtil.drawRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + padding, textHeight + 4, 3, new Color(20, 20, 20, 130));
                RoundedUtil.drawRound(ircNameBoxLeft + ircNameWidth / 2 + padding + gap, ircY - 2, uidWidth + padding, textHeight + 4, 3, new Color(20, 20, 20, 130));
            }


            if (!isShader) {

                AvatarUtil.drawForumAvatar(ircData.uidString, avatarLeft, ircY - 2, avatarBox, textHeight + 4, 3, 1);

                fontRenderer.drawCenteredString(ircData.ircName, ircNameBoxLeft + (padding / 2), ircY + 0.5f, new Color(210, 210, 210));
                fontRenderer.drawString(ircData.uidString, ircNameBoxLeft + ircNameWidth / 2 + padding + gap + (padding / 2), ircY + 0.5f, new Color(210, 210, 210).darker());
            }

        } else if (GothajClient.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) {
            float ircNameWidth = fontRenderer.getStringWidth("Friend");

            float nameCenter = nameRectLeft + (nameRectWidth / 2);
            float ircNameBoxLeft = nameCenter - 5;

            float ircY = renderY - textHeight - 8;

            if (isShader) {
                RoundedUtil.drawShaderRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + 8, textHeight + 4, 3, Color.BLACK);
            } else {
                RoundedUtil.drawRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + 8, textHeight + 4, 3, new Color(20, 20, 20, 130));
            }

            if (!isShader) {
                fontRenderer.drawCenteredString("Friend", ircNameBoxLeft + 4, ircY + 0.5f, new Color(0, 206, 211));
            }

        } else if (EntityUtil.isTeammate(player) && !Targets.combatTargets.getOptionState("Teammates")) {
            float ircNameWidth = fontRenderer.getStringWidth("Teammate");

            float nameCenter = nameRectLeft + (nameRectWidth / 2);
            float ircNameBoxLeft = nameCenter - 5;

            float ircY = renderY - textHeight - 8;

            if (isShader) {
                RoundedUtil.drawShaderRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + 8, textHeight + 4, 3, Color.BLACK);
            } else {
                RoundedUtil.drawRound(ircNameBoxLeft - ircNameWidth / 2, ircY - 2, ircNameWidth + 8, textHeight + 4, 3, new Color(20, 20, 20, 130));
            }

            if (!isShader) {
                fontRenderer.drawCenteredString("Teammate", ircNameBoxLeft + 4, ircY + 0.5f, -1);
            }

        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        RenderUtil.resetColor();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderArmor(EntityPlayer player, float x, float y, float x2, float y2, boolean isShader) {
        final float armorPercentage = player.getTotalArmorValue() / 20.0F;
        final float totalWidth = x2 - x;
        final float armorBarWidth = totalWidth * armorPercentage;
        final float endOfArmorBar = x + armorBarWidth;

        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);
        glBegin(GL_QUADS);
        glVertex2f(x, y2 + 0.5F);
        glVertex2f(x, y2 + 2.5F);
        glVertex2f(x2, y2 + 2.5F);
        glVertex2f(x2, y2 + 0.5F);

        if (armorPercentage > 0) {
            RenderUtil.color(isShader ? Color.BLACK.getRGB() : 0xff4798D1);
            glVertex2f(x + 0.5F, y2 + 1);
            glVertex2f(x + 0.5F, y2 + 2);
            glVertex2f(x + armorBarWidth - 0.5F, y2 + 2);
            glVertex2f(x + armorBarWidth - 0.5F, y2 + 1);
        }

        if (segmentArmor.getProperty() && !isShader) {
            RenderUtil.color(Color.BLACK.getRGB());

            float separatorSpacing = totalWidth / 10f;

            for (int i = 1; i < 10; i++) {
                float segX = x + (separatorSpacing * i);

                if (segX < endOfArmorBar) {
                    glVertex2f(segX, y2 + 1);
                    glVertex2f(segX, y2 + 2);
                    glVertex2f(segX + 0.5F, y2 + 2);
                    glVertex2f(segX + 0.5F, y2 + 1);
                }
            }
        }

        glEnd();
    }



    private void renderHealth(EntityPlayer player, float x, float y, float x2, float y2, float healthPercentage, boolean isShader) {
        float healthBarLeft = x - 3F;
        float healthBarRight = x - 1F;

        final float health = player.getHealth();
        final float maxHealth = player.getMaxHealth();

        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);
        glBegin(GL_QUADS);
        glVertex2f(healthBarLeft, y);
        glVertex2f(healthBarLeft, y2);
        glVertex2f(healthBarRight, y2);
        glVertex2f(healthBarRight, y);

        healthBarLeft += 0.5F;
        healthBarRight -= 0.5F;

        final float heightDif = y - y2;

        final float animatedPercent = getAnimatedHealth(player, healthPercentage);
        final float healthBarHeight = heightDif * animatedPercent;

        final boolean pulsing = dynamicHealth.getProperty();
        final float topOfHealthBar = y2 + 0.5F + healthBarHeight;

        if (!gradientCustom.getProperty()) {
            final int color = isShader ? Color.BLACK.getRGB() : getHealthColor(player, healthPercentage, false);
            RenderUtil.color(pulsing ? ColorUtil.fadeBetween(color, healthMode.is("Hud") ? Hud.getColor(7) : ColorUtil.darker(new Color(color), 0.36F).getRGB()) : color);

            glVertex2f(healthBarLeft, topOfHealthBar);
            glVertex2f(healthBarLeft, y2 - 0.5F);
            glVertex2f(healthBarRight, y2 - 0.5F);
            glVertex2f(healthBarRight, topOfHealthBar);
        } else {
            glEnd();

            final boolean needScissor = health < maxHealth;
            if (needScissor) {
                glEnable(GL_SCISSOR_TEST);
                RenderUtil.startScissorBox((int) healthBarLeft, (int) (y2 + healthBarHeight), 2, (int) y2);
            }

            int startColor = isShader ? Color.BLACK.getRGB() : getHealthColor(player, healthPercentage, false);
            final int endColor = isShader ? Color.BLACK.getRGB() : getHealthColor(player, healthPercentage, true);

            glShadeModel(GL_SMOOTH);
            glBegin(GL_QUADS);

            startColor = pulsing ? ColorUtil.fadeBetween(startColor, ColorUtil.darker(new Color(startColor), 0.36F).getRGB()) : startColor;
            RenderUtil.color(startColor);

            glVertex2f(healthBarLeft, topOfHealthBar);
            RenderUtil.color(pulsing ? ColorUtil.fadeBetween(endColor, ColorUtil.darker(new Color(endColor), 0.36F).getRGB()) : endColor);
            glVertex2f(healthBarLeft, y2 - 0.5F);
            glVertex2f(healthBarRight, y2 - 0.5F);
            RenderUtil.color(startColor);
            glVertex2f(healthBarRight, topOfHealthBar);

            glEnd();
            glShadeModel(GL_FLAT);

            if (needScissor) glDisable(GL_SCISSOR_TEST);

            glBegin(GL_QUADS);
        }

        final float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
            final float absorptionPercentage = Math.min(1.0F, absorption / 20.0F);
            final int absorptionColor = isShader ? Color.BLACK.getRGB() : new Color(0x4800FF).getRGB();
            final float absorptionHeight = heightDif * absorptionPercentage;
            final float topOfAbsorptionBar = y2 + 0.5F + absorptionHeight;

            RenderUtil.color(pulsing ? ColorUtil.fadeBetween(absorptionColor, ColorUtil.darker(new Color(absorptionColor), 0.36F).getRGB()) : absorptionColor);

            glVertex2f(healthBarLeft, topOfAbsorptionBar);
            glVertex2f(healthBarLeft, y2 - 0.5F);
            glVertex2f(healthBarRight, y2 - 0.5F);
            glVertex2f(healthBarRight, topOfAbsorptionBar);
        }

        if (segmentHealth.getProperty() && !isShader) {
            RenderUtil.color(Color.BLACK.getRGB());

            float barTotalHeight = Math.abs( y - y2);
            float separatorSpacing = barTotalHeight / 10f;

            for (int i = 1; i < 10; i++) {
                float segY = y + (separatorSpacing * i);
                if (segY > topOfHealthBar) {
                    glVertex2f(healthBarLeft, segY);
                    glVertex2f(healthBarLeft, segY + 0.5F);
                    glVertex2f(healthBarRight, segY + 0.5F);
                    glVertex2f(healthBarRight, segY);
                }
            }
        }

        glEnd();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderBox(EntityPlayer player, float x, float y, float x2, float y2, boolean isShader) {

        Color teamColor = new Color(ColorUtil.getTeamColor(player));

        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);
        glPushMatrix();

        if (!isShader) {
            float innerX = x + 1.0F;
            float innerY = y + 1.0F;
            float innerX2 = x2 - 1.0F;
            float innerY2 = y2 - 1.0F;
            float innerW = innerX2 - innerX;
            float innerH = innerY2 - innerY;

            if (fillBox.getProperty()) {
                if (fillMode.is("Image")) {
                    ResourceLocation textureToDraw = null;
                    if (espGif.isLoaded()) {
                        textureToDraw = espGif.getCurrentTexture();
                    } else if (selectedFillImg != null) {
                        textureToDraw = selectedFillImg;
                    }
                    if (textureToDraw != null) {
                        RenderUtil.drawImage(textureToDraw, innerX, innerY, innerW + 1.2F, innerH + 1.2F, fillAlpha.getProperty(), false);
                    }
                } else {
                    int alpha = fillAlpha.getProperty().intValue();
                    Color color1Raw = fillMode.is("Color") ? fillColor.getProperty()
                            : fillMode.is("Team") ? teamColor
                            : new Color(Hud.primaryColor.getProperty().getRGB() & 0xFFFFFF);
                    Color color2Raw = fillMode.is("Color") ? fillColorS.getProperty()
                            : fillMode.is("Team") ? teamColor.darker().darker()
                            : new Color(Hud.secondaryColor.getProperty().getRGB() & 0xFFFFFF);

                    Color color1 = new Color(color1Raw.getRed(), color1Raw.getGreen(), color1Raw.getBlue(), alpha);
                    Color color2 = !gradientFill.getProperty() ? color1 : new Color(color2Raw.getRed(), color2Raw.getGreen(), color2Raw.getBlue(), alpha);
                    Color colorMid = ColorUtil.interpolateColor(color1Raw, color2Raw, 0.5f);
                    colorMid = !gradientFill.getProperty() ? color1 : new Color(colorMid.getRed(), colorMid.getGreen(), colorMid.getBlue(), alpha);

                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.enableAlpha();
                    GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
                    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                    GlStateManager.shadeModel(GL11.GL_SMOOTH);

                    glBegin(GL11.GL_QUADS);
                    RenderUtil.color4f(color1);
                    glVertex2f(innerX, innerY);
                    RenderUtil.color4f(colorMid);
                    glVertex2f(innerX, innerY2);
                    RenderUtil.color4f(color2);
                    glVertex2f(innerX2, innerY2);
                    RenderUtil.color4f(colorMid);
                    glVertex2f(innerX2, innerY);
                    RenderUtil.resetColor();
                    glEnd();

                    GlStateManager.shadeModel(GL11.GL_FLAT);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                    GlStateManager.disableAlpha();
                }
            }
        }

        RenderUtil.resetColor();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) 0x96);

        glBegin(GL_QUADS);

        if (cornered.getProperty()) {
            float length = (y2 - y) / 4.5F;
            length = Math.max(4.0F, length);


            glVertex2f(x, y);
            glVertex2f(x, y + length);
            glVertex2f(x + 1.5F, y + length);
            glVertex2f(x + 1.5F, y);

            glVertex2f(x, y);
            glVertex2f(x, y + 1.5F);
            glVertex2f(x + length, y + 1.5F);
            glVertex2f(x + length, y);

            glVertex2f(x2 - 1.5F, y);
            glVertex2f(x2 - 1.5F, y + length);
            glVertex2f(x2, y + length);
            glVertex2f(x2, y);

            glVertex2f(x2 - length, y);
            glVertex2f(x2 - length, y + 1.5F);
            glVertex2f(x2, y + 1.5F);
            glVertex2f(x2, y);

            glVertex2f(x, y2 - length);
            glVertex2f(x, y2);
            glVertex2f(x + 1.5F, y2);
            glVertex2f(x + 1.5F, y2 - length);

            glVertex2f(x, y2 - 1.5F);
            glVertex2f(x, y2);
            glVertex2f(x + length, y2);
            glVertex2f(x + length, y2 - 1.5F);

            glVertex2f(x2 - 1.5F, y2 - length);
            glVertex2f(x2 - 1.5F, y2);
            glVertex2f(x2, y2);
            glVertex2f(x2, y2 - length);

            glVertex2f(x2 - length, y2 - 1.5F);
            glVertex2f(x2 - length, y2);
            glVertex2f(x2, y2);
            glVertex2f(x2 - 4F, y2);

            RenderUtil.color4f(isShader ? Color.BLACK : Color.WHITE);
            float innerLength = length - 0.5F;

            glVertex2f(x + 0.5F, y + 0.5F);
            glVertex2f(x + 0.5F, y + innerLength);
            glVertex2f(x + 1F, y + innerLength);
            glVertex2f(x + 1F, y + 0.5F);

            glVertex2f(x + 0.5F, y + 0.5F);
            glVertex2f(x + 0.5F, y + 1F);
            glVertex2f(x + innerLength, y + 1F);
            glVertex2f(x + innerLength, y + 0.5F);

            glVertex2f(x2 - 1F, y + 0.5F);
            glVertex2f(x2 - 1F, y + innerLength);
            glVertex2f(x2 - 0.5F, y + innerLength);
            glVertex2f(x2 - 0.5F, y + 0.5F);

            glVertex2f(x2 - innerLength, y + 0.5F);
            glVertex2f(x2 - innerLength, y + 1F);
            glVertex2f(x2 - 0.5F, y + 1F);
            glVertex2f(x2 - 0.5F, y + 0.5F);

            glVertex2f(x + 0.5F, y2 - innerLength);
            glVertex2f(x + 0.5F, y2 - 0.5F);
            glVertex2f(x + 1F, y2 - 0.5F);
            glVertex2f(x + 1F, y2 - innerLength);

            glVertex2f(x + 0.5F, y2 - 1F);
            glVertex2f(x + 0.5F, y2 - 0.5F);
            glVertex2f(x + innerLength, y2 - 0.5F);
            glVertex2f(x + innerLength, y2 - 1F);

            glVertex2f(x2 - 1F, y2 - innerLength);
            glVertex2f(x2 - 1F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - innerLength);

            glVertex2f(x2 - innerLength, y2 - 1F);
            glVertex2f(x2 - innerLength, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 1F);

        } else {
            glVertex2f(x, y);
            glVertex2f(x, y2);
            glVertex2f(x + 1.5F, y2);
            glVertex2f(x + 1.5F, y);

            glVertex2f(x2 - 1.5F, y);
            glVertex2f(x2 - 1.5F, y2);
            glVertex2f(x2, y2);
            glVertex2f(x2, y);

            glVertex2f(x + 1.5F, y);
            glVertex2f(x + 1.5F, y + 1.5F);
            glVertex2f(x2 - 1.5F, y + 1.5F);
            glVertex2f(x2 - 1.5F, y);

            glVertex2f(x + 1.5F, y2 - 1.5F);
            glVertex2f(x + 1.5F, y2);
            glVertex2f(x2 - 1.5F, y2);
            glVertex2f(x2 - 1.5F, y2 - 1.5F);

            RenderUtil.color4f(isShader ? Color.BLACK : Color.WHITE);

            glVertex2f(x + 0.5F, y + 0.5F);
            glVertex2f(x + 0.5F, y2 - 0.5F);
            glVertex2f(x + 1, y2 - 0.5F);
            glVertex2f(x + 1, y + 0.5F);

            glVertex2f(x2 - 1, y + 0.5F);
            glVertex2f(x2 - 1, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y + 0.5F);

            glVertex2f(x + 0.5F, y + 0.5F);
            glVertex2f(x + 0.5F, y + 1);
            glVertex2f(x2 - 0.5F, y + 1);
            glVertex2f(x2 - 0.5F, y + 0.5F);

            glVertex2f(x + 0.5F, y2 - 1);
            glVertex2f(x + 0.5F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 0.5F);
            glVertex2f(x2 - 0.5F, y2 - 1);
        }

        glEnd();

        glPopAttrib();
        glPopMatrix();
    }


    @Listen
    public void onRenderDimension(RenderDimensionEvent event) {
        float partialTicks = event.getPartialTicks();
        int scaleFactor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();

        if (!entityMap.isEmpty()) entityMap.clear();
        if (!lastRenderPos.isEmpty()) lastRenderPos.clear();

        List<EntityPlayer> players = new ArrayList<>(mc.theWorld.playerEntities);

        if (!players.contains(mc.thePlayer) && mc.gameSettings.thirdPersonView > 0) {
            players.add(mc.thePlayer);
        } else if (mc.gameSettings.thirdPersonView == 0) {
            players.remove(mc.thePlayer);
        }

        if (players.isEmpty()) return;

        for (EntityPlayer player : players) {
            lastRenderPos.put(player, new double[]{
                    player.lastTickPosX, player.posX,
                    player.lastTickPosY, player.posY,
                    player.lastTickPosZ, player.posZ
            });
        }

        for (final EntityPlayer player : players) {

            if (!isValid(player)) continue;

            mc.entityRenderer.setupCameraTransform(event.getPartialTicks(), 0);

            double[] cached = lastRenderPos.get(player);
            if (cached == null) continue;

            final double posX = (RenderUtil.interpolate(cached[0], cached[1], partialTicks) -
                    mc.getRenderManager().viewerPosX);
            final double posY = (RenderUtil.interpolate(cached[2], cached[3], partialTicks) -
                    mc.getRenderManager().viewerPosY);
            final double posZ = (RenderUtil.interpolate(cached[4], cached[5], partialTicks) -
                    mc.getRenderManager().viewerPosZ);

            final double halfWidth = player.width / 2.0D;
            final var eventSpoof = new SneakSpoofEvent();
            if (player == Minecraft.getMinecraft().thePlayer) GothajClient.getInstance().getEventBus().fire(eventSpoof);
            final AxisAlignedBB bb = new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth,
                    posX + halfWidth, posY + player.height + (player.isSneaking() && !eventSpoof.isCancelled() ? -0.2D : 0.1D), posZ + halfWidth).expand(0.1, 0.1, 0.1);

            final double[][] vectors = {{bb.minX, bb.minY, bb.minZ},
                    {bb.minX, bb.maxY, bb.minZ},
                    {bb.minX, bb.maxY, bb.maxZ},
                    {bb.minX, bb.minY, bb.maxZ},
                    {bb.maxX, bb.minY, bb.minZ},
                    {bb.maxX, bb.maxY, bb.minZ},
                    {bb.maxX, bb.maxY, bb.maxZ},
                    {bb.maxX, bb.minY, bb.maxZ}};

            float[] projection;
            final float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};

            for (final double[] vec : vectors) {
                projection = GLUtil.project2D((float) vec[0], (float) vec[1], (float) vec[2], scaleFactor);
                if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                    final float pX = projection[0];
                    final float pY = projection[1];
                    position[0] = Math.min(position[0], pX);
                    position[1] = Math.min(position[1], pY);
                    position[2] = Math.max(position[2], pX);
                    position[3] = Math.max(position[3], pY);
                }
            }

            entityMap.put(player, position);
        }
    }

    @Listen
    public void onRenderNameTag(RenderNameTagEvent event) {
        event.setCancelled(elements.getOptionState("Tag") && entityMap.containsKey(event.getEntity()));
    }

    public static boolean isValid(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer || entity.deathTime > 0 || !entity.isEntityAlive() || entity.getHealth() == 0) return false;

        if (entity instanceof EntityArmorStand) return false;

        String name = entity.getDisplayName().getUnformattedText();
        if (name.contains("Shop") || name.contains("Keeper")) return false;

        if (entity instanceof EntityPlayer pl) {
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0) return true;
            if (!Targets.visualTargets.getOptionState("Players")) return false;
            if (EntityUtil.isBot(pl) && !Targets.visualTargets.getOptionState("Bots")) return false;
        }

        if (entity instanceof IMob && !Targets.visualTargets.getOptionState("Monsters")) return false;

        Team entityTeam = entity.getTeam();
        Team playerTeam = mc.thePlayer.getTeam();
        boolean isTeammate = EntityUtil.isTeammate(entity) || (entityTeam != null && playerTeam != null && entityTeam.isSameTeam(playerTeam));

        if (isTeammate && !Targets.visualTargets.getOptionState("Teammates")) {
            return false;
        }

        if ((entity instanceof IAnimals || entity instanceof EntityAgeable) && !Targets.visualTargets.getOptionState("Animals")) return false;

        return !entity.isInvisible() || Targets.visualTargets.getOptionState("Invisibles");
    }

    private float getAnimatedHealth(EntityPlayer player, float healthPercentage) {
        Animation animation = healthAnimations.computeIfAbsent(player, k ->
                new Animation(Easing.EASE_IN_OUT_QUAD, 300));

        animation.run(healthPercentage);

        return (float) Math.max(0, Math.min(1, animation.getValue()));
    }


    private int getHealthColor(EntityPlayer player, float healthPercentage, boolean secondary) {

        switch (healthMode.getProperty()) {
            case "Health" -> {
                if (secondary) {
                    return ColorUtil.darker(new Color(ColorUtil.getColorFromPercentage(healthPercentage)), 0.36F).getRGB();
                }
                return ColorUtil.getColorFromPercentage(healthPercentage);
            }
            case "Custom" -> {
                if (secondary) {
                    return healthSecondaryColor.getProperty().getRGB();
                }
                return healthPrimaryColor.getProperty().getRGB();
            }
            case "Team" -> {
                if (secondary) {
                    return ColorUtil.darker(new Color(ColorUtil.getTeamColor(player)), 0.36F).getRGB();
                }
                return new Color(ColorUtil.getTeamColor(player)).getRGB();
            }
            case "Hud" -> {
                if (secondary) {
                    return Hud.secondaryColor.getProperty().getRGB();
                }
                return Hud.primaryColor.getProperty().getRGB();
            }
        }
        return -1;
    }

    private record PlayerIRCData(String ircName, String uidString) {
    }
}
