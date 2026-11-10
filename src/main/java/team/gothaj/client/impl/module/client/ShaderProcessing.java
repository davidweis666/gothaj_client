package team.gothaj.client.impl.module.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.StencilUtil;
import team.gothaj.client.api.util.render.shader.ShaderUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.KawaseBloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.api.util.render.shader.impl.KawaseBlur;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;

import static net.minecraft.client.gui.GuiScreen.MAIN_SHADER;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glPopMatrix;

public class ShaderProcessing extends Module {


    public static Framebuffer frameBuffer = new Framebuffer(1, 1, false);

    public static Framebuffer guiFrameBuffer = new Framebuffer(1, 1, false);
    
    public static final BooleanProperty blur = new BooleanProperty("Blur", true);

    public static final ModeProperty blurMode = new ModeProperty("Blur Type", "Gaussian", "Gaussian", "Kawase").setVisible(blur::getProperty);

    public final BooleanProperty blurBackground = new BooleanProperty("Blur Background", false).setVisible(blur::getProperty);

    public final NumberProperty blurStrength = new NumberProperty("Blur Strength", 7, 1, 30, 1).setVisible(() -> blurMode.is("Gaussian") && blur.getProperty());

    public final NumberProperty blurIterations = new NumberProperty("Blur Iterations", 5, 1, 5, 1).setVisible(() -> blurMode.is("Kawase") && blur.getProperty());

    private final NumberProperty blurOffset = new NumberProperty("Blur Offset", 5, 1, 30, 1).setVisible(() -> blurMode.is("Kawase") && blur.getProperty());

    public static final BooleanProperty bloom = new BooleanProperty("Shadow", true);

    public static final ModeProperty bloomMode = new ModeProperty("Shadow Type", "Gaussian", "Gaussian", "Kawase").setVisible(bloom::getProperty).addChangeListener((property, oldValue, newValue) -> {
        if (!oldValue.equalsIgnoreCase(newValue) && newValue.equalsIgnoreCase("Kawase") && blurMode.is("Gaussian")) {
            GothajClient.getInstance().getNotificationManager().add("ShaderProcessing", "Using kawase shadow with gaussian blur is not recommended!", Notification.Type.INFO);
        }
        return newValue;
    });

    public final NumberProperty bloomOffset = new NumberProperty("Shadow Offset", 1, 1, 10, 1).setVisible(bloom::getProperty);

    public final NumberProperty bloomStrength = new NumberProperty("Shadow Radius", 10, 1, 30, 1).setVisible(() -> bloom.getProperty() && bloomMode.is("Gaussian"));

    public final NumberProperty bloomIterations = new NumberProperty("Shadow Iterations", 5, 1, 5, 1).setVisible(() -> bloomMode.is("Kawase") && bloom.getProperty());

    private final BooleanProperty bloomMatchHud = new BooleanProperty("Shadow Match HUD", false).setVisible(bloom::getProperty);

    private final BooleanProperty gothajShader = new BooleanProperty("Gothaj Shader", true);

    private final NumberProperty gothajShaderOpacity = new NumberProperty("Gothaj Shader Alpha", 100, 75, 255, 1);


    public ShaderProcessing() {
        super("ShaderProcessing", "Renders shadow and blur in your widgets", ModuleCategory.CLIENT);
        registerProperties(blur, blurMode, blurBackground, blurStrength, blurIterations, blurOffset,
                bloom, bloomMode, bloomOffset, bloomIterations, bloomStrength, bloomMatchHud,
                gothajShader, gothajShaderOpacity);
    }

    @Override
    public void setToggled(boolean toggle) {
        super.setToggled(toggle);
    }

    public void drawShaders(final ScaledResolution scaledResolution) {
        if (!this.isToggled()) return;

        if (blur.getProperty()) {

            switch (blurMode.getProperty()) {
                case "Kawase" -> {
                    frameBuffer = RenderUtil.createFrameBuffer(frameBuffer, false);
                    frameBuffer.framebufferClear();
                    frameBuffer.bindFramebuffer(false);
                    GothajClient.getInstance().getEventBus().fire(new ShaderOverlayEvent(scaledResolution, ShaderOverlayEvent.Type.BLUR));
                    frameBuffer.unbindFramebuffer();

                    if (frameBuffer.framebufferTexture > 0) {
                        KawaseBlur.renderBlur(frameBuffer.framebufferTexture, blurIterations.getProperty().intValue(), blurOffset.getProperty().intValue());
                    }
                }

                case "Gaussian" -> {
                    Blur.startBlur();
                    GothajClient.getInstance().getEventBus().fire(new ShaderOverlayEvent(scaledResolution, ShaderOverlayEvent.Type.BLUR));
                    Blur.endBlur(blurStrength.getProperty().intValue(), 1);
                }
            }
        }

        if (bloom.getProperty()) {

            frameBuffer = RenderUtil.createFrameBuffer(frameBuffer, false);
            frameBuffer.framebufferClear();
            frameBuffer.bindFramebuffer(false);
            GothajClient.getInstance().getEventBus().fire(new ShaderOverlayEvent(scaledResolution, ShaderOverlayEvent.Type.BLOOM));
            frameBuffer.unbindFramebuffer();

            if (frameBuffer.framebufferTexture > 0) {
                if (bloomMode.is("Kawase")) {
                    KawaseBloom.renderBlur(frameBuffer.framebufferTexture, bloomIterations.getProperty().intValue(), bloomOffset.getProperty().intValue(), new Color(bloomMatchHud.getProperty() ? Hud.getColor(5) : Color.BLACK.getRGB()));
                } else {
                    Bloom.renderBloom(frameBuffer.framebufferTexture, bloomStrength.getProperty().intValue(), bloomOffset.getProperty().intValue(), bloomMatchHud.getProperty() ? Hud.getColor(5) : Color.BLACK.getRGB());
                }
            }
            //RenderUtil.resetColor();

        }

        if (gothajShader.getProperty()) {
            StencilUtil.init(() -> {
                GothajClient.getInstance().getEventBus().fire(new ShaderOverlayEvent(scaledResolution, ShaderOverlayEvent.Type.GOTHAJ));
                RenderUtil.rect(-10, -10, -5, -5, Color.BLACK);
            });

            glPushMatrix();
            glDisable(GL_CULL_FACE);
            glDisable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            MAIN_SHADER.init();
            MAIN_SHADER.setUniformf("alpha", gothajShaderOpacity.getProperty().intValue() / 255F);
            MAIN_SHADER.setUniformf("time", (System.currentTimeMillis() - RenderUtil.initTime) / 1500.0F);
            MAIN_SHADER.setUniformf("resolution", Minecraft.getMinecraft().displayWidth / 8F, Minecraft.getMinecraft().displayHeight / 8F);

            ShaderUtil.drawFixedQuads();

            MAIN_SHADER.unload();

            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glPopMatrix();

            StencilUtil.uninit();
        }
    }
}
