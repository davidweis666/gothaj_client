package team.gothaj.client.api.util.render.shader.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.StencilUtil;
import team.gothaj.client.api.util.render.shader.ShaderUtil;

import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.glUniform1fv;

public class Blur implements Utils{
    private static final ShaderUtil GAUSSIAN_BLUR_SHADER = new ShaderUtil("blur");
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static final int MAX_RADIUS = 128;
    private static final int CACHE_LIMIT = 16;
    private static final Map<Float, FloatBuffer> gaussianWeightCache = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Float, FloatBuffer> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    public static FloatBuffer getGaussianWeights(float radius) {
        radius = Math.min(radius, MAX_RADIUS);

        return gaussianWeightCache.computeIfAbsent(radius, r -> {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(MAX_RADIUS);
            float sigma = r / 2f;
            float sum = 0f;

            for (int i = 0; i < MAX_RADIUS; i++) {
                float weight = MathUtil.calculateGaussianValue(i, sigma);
                if (weight < 0.001f) break;
                buffer.put(weight);
                sum += (i == 0) ? weight : 2 * weight;
            }

            buffer.rewind();
            for (int i = 0; i < buffer.limit(); i++) {
                buffer.put(i, buffer.get(i) / sum);
            }

            buffer.rewind();
            return buffer;
        });
    }

    private static void setupUniforms(float dirX, float dirY, float radius) {
        ScaledResolution sr = new ScaledResolution(mc);

        GAUSSIAN_BLUR_SHADER.setUniformi("textureIn", 0);
        GAUSSIAN_BLUR_SHADER.setUniformf("texelSize", 1f / sr.getScaledWidth(), 1f / sr.getScaledHeight());
        GAUSSIAN_BLUR_SHADER.setUniformf("direction", dirX, dirY);
        GAUSSIAN_BLUR_SHADER.setUniformf("radius", radius);

        glUniform1fv(GAUSSIAN_BLUR_SHADER.getUniform("weights"), getGaussianWeights(radius));
    }

    public static void startBlur() {
        StencilUtil.initStencilToWrite();
    }

    public static void endBlur(float radius, float compression) {
        if (radius <= 0.0f || compression <= 0.0f) {
            StencilUtil.uninit();
            return;
        }

        StencilUtil.readStencilBuffer(1);
        framebuffer = RenderUtil.createFrameBuffer(framebuffer);

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        applyBlurPass(compression, 0.0f, radius, mc.getFramebuffer().framebufferTexture);
        framebuffer.unbindFramebuffer();

        mc.getFramebuffer().bindFramebuffer(false);
        applyBlurPass(0.0f, compression, radius, framebuffer.framebufferTexture);

        StencilUtil.uninit();
        RenderUtil.resetColor();
        GlStateManager.bindTexture(0);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void applyBlurPass(float dirX, float dirY, float radius, int texture) {
        GAUSSIAN_BLUR_SHADER.init();
        setupUniforms(dirX, dirY, radius);
        RenderUtil.bindTexture(texture);
        ShaderUtil.drawQuads();
        GAUSSIAN_BLUR_SHADER.unload();
    }
}