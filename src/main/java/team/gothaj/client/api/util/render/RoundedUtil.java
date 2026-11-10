package team.gothaj.client.api.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.render.shader.ShaderUtil;
import team.gothaj.client.impl.module.client.ShaderProcessing;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtil implements Utils {
    public static ShaderUtil roundedShader = new ShaderUtil("roundedRect");
    public static ShaderUtil roundedOptionalShader = new ShaderUtil("roundedRectOptional");
    public static ShaderUtil roundedOutlineShader = new ShaderUtil("roundRectOutline");
    public static ShaderUtil gradientMaskShader = new ShaderUtil("gradientMask");
    private static final ShaderUtil roundedTexturedShader = new ShaderUtil("roundedRectTexture");
    private static final ShaderUtil roundedGradientShader = new ShaderUtil("roundRectGradient");
    private static final ShaderUtil roundedGradientShaderOptional = new ShaderUtil("roundRectGradientOptional");

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, Color color) {
        drawShaderRound(x, y, width, height, radius, false, color);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        RenderUtil.resetColor();
        GLUtil.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.setAlphaLimit(0);
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2);

        roundedShader.unload();
        GLUtil.endBlend();
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {

        if (ShaderProcessing.bloomMode.is("Gaussian") || mc.currentScreen != null) {
            x += 0.25f;
            y += 0.25f;
            width -= 0.5f;
            height -= 0.5f;
        }

        drawRound(x, y, width, height, radius, blur, color);
    }


    public static void drawRound(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, Color color) {
        drawRound(x, y, width, height, bottomRightRadius, bottomLeftRadius, topRightRadius, topLeftRadius, false, color);
    }

    public static void drawShaderRound(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, Color color) {
        if (ShaderProcessing.bloomMode.is("Gaussian") || mc.currentScreen != null) {
            x += 0.3f;
            y += 0.27f;
            width -= 0.6f;
            height -= 0.54f;
        }

        drawRound(x, y, width, height, bottomRightRadius, bottomLeftRadius, topRightRadius, topLeftRadius, false, color);
    }

    public static void drawRound(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, boolean blur, Color color) {
        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedOptionalShader.init();

        setupRoundedRectUniforms(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius, roundedOptionalShader);
        roundedOptionalShader.setUniformi("blur", blur ? 1 : 0);
        roundedOptionalShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2);

        roundedOptionalShader.unload();
        GLUtil.endBlend();
    }

    public static void drawGradientRound(float x, float y, float width, float height, float bottomLeftRadius, float bottomRightRadius, float topLeftRadius, float topRightRadius, Color left, Color right) {
        drawGradientRound(x, y, width, height, bottomLeftRadius, bottomRightRadius, topLeftRadius, topRightRadius, left, left, right, right);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, int left, int right) {
        drawGradientRound(x, y, width, height, bottomLeftRadius, bottomRightRadius, topLeftRadius, topRightRadius, new Color(left), new Color(left), new Color(right), new Color(right));
    }

    public static void drawGradientRound(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        RenderUtil.resetColor();
        GLUtil.startBlend();
        RenderUtil.setAlphaLimit(0);
        roundedGradientShaderOptional.init();

        setupRoundedRectUniforms(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius, roundedGradientShaderOptional);

        // Bottom left
        roundedGradientShaderOptional.setUniformf("color1", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
        // Top Left
        roundedGradientShaderOptional.setUniformf("color2", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
        // Bottom Right
        roundedGradientShaderOptional.setUniformf("color3", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);
        // Top Right
        roundedGradientShaderOptional.setUniformf("color4", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);

        ShaderUtil.drawQuads(x, y, width, height);

        roundedGradientShaderOptional.unload();
        GLUtil.endBlend();
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientLine(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientLine(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawShaderGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        x += 0.2f;
        y += 0.2f;
        width -= 0.4f;
        height -= 0.4f;

        drawGradientRound(x, y, width, height, radius, bottomLeft, topLeft, bottomRight, topRight);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        RenderUtil.resetColor();
        GLUtil.startBlend();
        RenderUtil.setAlphaLimit(0);
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);

        //Top left
        roundedGradientShader.setUniformf("color1", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
        // Bottom Left
        roundedGradientShader.setUniformf("color2", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
        //Top Right
        roundedGradientShader.setUniformf("color3", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
        //Bottom Right
        roundedGradientShader.setUniformf("color4", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);

        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2);

        roundedGradientShader.unload();
        GLUtil.endBlend();
    }

    public static void applyGradientMask(float x, float y, float width, float height, float alpha, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight, Runnable content) {
        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();

        gradientMaskShader.init();

        gradientMaskShader.setUniformf("location", x * factor, (Minecraft.getMinecraft().displayHeight - (height * factor)) - (y * factor));
        gradientMaskShader.setUniformf("rectSize", width * factor, height * factor);
        gradientMaskShader.setUniformf("alpha", alpha);
        gradientMaskShader.setUniformi("tex", 0);

        // Bottom Left
        gradientMaskShader.setUniformf("color1", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f);
        // Top left
        gradientMaskShader.setUniformf("color2", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f);
        // Bottom Right
        gradientMaskShader.setUniformf("color3", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f);
        // Top Right
        gradientMaskShader.setUniformf("color4", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f);

        content.run();

        gradientMaskShader.unload();

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1, 1, 1, 1);
    }


    public static void drawGradientLine(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        RenderUtil.setAlphaLimit(0);
        RenderUtil.resetColor();
        GLUtil.startBlend();
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);

        //Top left
        roundedGradientShader.setUniformf("color1", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
        // Bottom Left
        roundedGradientShader.setUniformf("color2", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
        //Top Right
        roundedGradientShader.setUniformf("color3", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
        //Bottom Right
        roundedGradientShader.setUniformf("color4", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);

        ShaderUtil.drawQuads(x, y, width, height);
        roundedGradientShader.unload();
        GLUtil.endBlend();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        RenderUtil.resetColor();
        GLUtil.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(mc);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);

        ShaderUtil.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtil.endBlend();
    }

    public static void drawRoundTextured(float x, float y, float width, float height, float radius, float alpha) {
        RenderUtil.resetColor();
        RenderUtil.setAlphaLimit(0);
        GLUtil.startBlend();
        roundedTexturedShader.init();
        roundedTexturedShader.setUniformi("textureIn", 0);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedTexturedShader);
        roundedTexturedShader.setUniformf("alpha", alpha);
        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedTexturedShader.unload();
        GLUtil.endBlend();
    }


    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtil shader) {
        ScaledResolution sr = new ScaledResolution(mc);
        shader.setUniformf("location", x * sr.getScaleFactor(), (mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        shader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        shader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, ShaderUtil shader) {
        ScaledResolution sr = new ScaledResolution(mc);
        shader.setUniformf("location", x * sr.getScaleFactor(), (mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        shader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        shader.setUniformf("radius", topLeftRadius * sr.getScaleFactor(), topRightRadius * sr.getScaleFactor(), bottomLeftRadius * sr.getScaleFactor(), bottomRightRadius * sr.getScaleFactor());
    }
}
