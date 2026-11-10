package team.gothaj.client.api.util.render.shader.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.shader.ShaderUtil;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUniform1fv;

public class Bloom implements Utils {
    private static final ShaderUtil bloomShader = new ShaderUtil("bloom");
    private static Framebuffer bloomFramebuffer = new Framebuffer(1, 1, false);
    private static final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
    private static final int MAX_RADIUS = 128;
    private static final int CACHE_LIMIT = 16;

    private static final Map<Integer, float[]> gaussianCache = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, float[]> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    public static void renderBloom(int sourceTexture, int radius, int color) {
        renderBloom(sourceTexture, radius, 1, color);
    }

    public static void renderBloom(int sourceTexture, int radius, int offset, int color) {
        if (radius < 0) return;

        bloomFramebuffer = RenderUtil.createFrameBuffer(bloomFramebuffer);

        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0.0f);
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        bloomFramebuffer.framebufferClear();
        bloomFramebuffer.bindFramebuffer(true);
        bloomShader.init();
        setUniforms(radius, color, offset, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
        ShaderUtil.drawQuads();

        mc.getFramebuffer().bindFramebuffer(true);
        setUniforms(radius, color,0, offset);
        GL13.glActiveTexture(GL13.GL_TEXTURE16);
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, bloomFramebuffer.framebufferTexture);
        ShaderUtil.drawQuads();

        bloomShader.unload();
        GlStateManager.bindTexture(0);
    }

    private static void setUniforms(int radius, int color, int dirX, int dirY) {
        float[] weights = getGaussianWeights(radius);
        weightBuffer.clear();
        for (float w : weights) {
            weightBuffer.put(w);
        }
        weightBuffer.flip();

        ScaledResolution sr = new ScaledResolution(mc);

        Color c = new Color(color);

        float red = c.getRed() / 255f;
        float green = c.getGreen() / 255f;
        float blue = c.getBlue() / 255f;

        bloomShader.setUniformi("inTexture", 0);
        bloomShader.setUniformi("textureToCheck", 16);
        bloomShader.setUniformf("texelSize", 1f / sr.getScaledWidth(), 1f / sr.getScaledHeight());
        bloomShader.setUniformf("radius", radius);
        bloomShader.setUniformf("direction", dirX, dirY);
        bloomShader.setUniformf("color", red, green, blue);
        glUniform1fv(bloomShader.getUniform("weights"), weightBuffer);
    }

    private static float[] getGaussianWeights(int radius) {
        radius = Math.min(radius, MAX_RADIUS - 1);
        return gaussianCache.computeIfAbsent(radius, r -> {
            float[] weights = new float[256];
            float sigma = r / 2f;
            float twoSigmaSq = 2 * sigma * sigma;
            float sum = 0f;

            for (int i = 0; i <= r; i++) {
                float weight = (float) Math.exp(-(i * i) / twoSigmaSq);
                weights[i] = weight;
                sum += (i == 0) ? weight : 2 * weight;
            }

            float inv = 1.0f / sum;
            for (int i = 0; i <= r; i++) {
                weights[i] *= inv;
            }
            return weights;
        });
    }
}