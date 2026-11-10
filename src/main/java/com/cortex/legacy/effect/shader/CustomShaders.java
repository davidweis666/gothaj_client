package com.cortex.legacy.effect.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.util.Logger;
import org.lwjgl.opengl.GL20;
import com.cortex.legacy.render.gl.ShaderProgram;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiles and caches user-supplied GLSL effects.
 *
 * <h2>Writing one</h2>
 * An effect supplies only a body — the engine wraps it with the boilerplate, so there is no
 * {@code #version} line, no varying declarations and no chance of the rounded-corner mask
 * being forgotten. <b>Anything passed in through a {@link UniformBlock} must be declared by
 * the body itself</b>, since the engine cannot know its name or type:
 *
 * <pre>{@code
 * uniform vec4 u_Accent;          // matches UniformBlock.setColor("u_Accent", ...)
 *
 * vec4 cortexEffect(vec2 uv, vec2 local) {
 *     vec3 base = texture(u_Texture, uv).rgb;
 *     float wave = sin(local.y * 0.2 + u_Time * 3.0) * 0.5 + 0.5;
 *     return vec4(mix(base, u_Accent.rgb, wave), 1.0);
 * }
 * }</pre>
 *
 * <p>{@code uv} samples the frame behind the region and {@code local} is the offset from
 * the region's centre in logical pixels. Declared for you: {@code u_Texture}, {@code u_Time},
 * {@code u_Region}, {@code u_HalfSize}, {@code u_Radii} and {@code u_Resolution}.
 *
 * <p>A body that fails to compile is reported once and then does nothing — a panel that
 * simply never appears is usually a compile error in the log, most often a uniform that was
 * passed in but never declared.
 */
public final class CustomShaders {

    /**
     * Everything an effect body can rely on, plus the {@code main} that applies the
     * rounded-rect mask afterwards. Keeping the mask here rather than in user code means an
     * effect cannot accidentally spill outside its own panel.
     */
    private static final String HEADER = """
            #version 330 core

            in vec2 v_Local;
            in vec2 v_SourceUV;

            uniform sampler2D u_Texture;
            uniform vec2 u_HalfSize;
            uniform vec4 u_Radii;
            uniform vec4 u_Region;
            uniform vec2 u_Resolution;
            uniform float u_Time;

            out vec4 fragColor;

            float pickRadius(vec2 p, vec4 r) {
                float top = p.x < 0.0 ? r.x : r.y;
                float bottom = p.x < 0.0 ? r.w : r.z;
                return p.y < 0.0 ? top : bottom;
            }

            float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
                float rad = min(pickRadius(p, r), min(b.x, b.y));
                vec2 q = abs(p) - b + rad;
                return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - rad;
            }

            vec4 cortexEffect(vec2 uv, vec2 local);

            void main() {
                float d = sdRoundedBox(v_Local, u_HalfSize, u_Radii);
                float edge = max(fwidth(d), 1e-4);
                float mask = 1.0 - smoothstep(-edge, edge, d);
                if (mask <= 0.0) discard;

                vec4 result = cortexEffect(v_SourceUV, v_Local);
                fragColor = vec4(result.rgb, result.a * mask);
            }

            """;

    /** An effect that has been registered but not yet compiled. */
    private record Source(String body, String resource) {
    }

    private final Map<String, ShaderProgram> programs = new ConcurrentHashMap<>();
    private final Map<String, Source> pending = new ConcurrentHashMap<>();
    private final Map<String, Boolean> failed = new ConcurrentHashMap<>();

    /**
     * Registers an effect from inline GLSL.
     *
     * <p>Only records the source — compiling happens the first time the effect is drawn.
     * That is what makes this callable from anywhere: modules are commonly enabled while a
     * config loads on a background thread, and a GL call from a thread that holds no
     * context does not throw, it kills the process.
     */
    public void register(String name, String effectBody) {
        if (isKnown(name)) return;
        pending.put(name, new Source(effectBody, null));
    }

    /**
     * Registers an effect whose body lives in {@code assets/gothaj/shaders/cortex/<path>}.
     *
     * <p>Reading and compiling are both deferred to first use, for the same reason as
     * {@link #register}.
     */
    public void registerFromResource(String name, String path) {
        if (isKnown(name)) return;
        pending.put(name, new Source(null, path));
    }

    private boolean isKnown(String name) {
        return programs.containsKey(name) || pending.containsKey(name) || failed.containsKey(name);
    }

    /**
     * Returns the compiled effect, building it on first use.
     *
     * <p>Must be called on the render thread — it is the point at which the effect's GL
     * resources are created.
     */
    public ShaderProgram get(String name) {
        ShaderProgram program = programs.get(name);
        if (program != null) return program;

        Source source = pending.remove(name);
        if (source == null) return null;

        try {
            String body = source.body() != null
                    ? source.body()
                    : read("shaders/cortex/" + source.resource());

            program = ShaderProgram.fromSource(name, readVertexShader(), HEADER + body);
            programs.put(name, program);
            return program;
        } catch (Exception e) {
            failed.put(name, Boolean.TRUE);

            // Loud on purpose: the only other symptom is a panel that never draws, which
            // looks like a positioning mistake rather than a shader that never built.
            Logger.log("Cortex: effect '" + name + "' failed to compile and will not draw");
            System.err.println("Cortex: could not compile effect '" + name + "': " + e.getMessage());
            return null;
        }
    }

    /** Effects reuse the region-quad vertex shader that backdrop panels are drawn with. */
    private static String readVertexShader() {
        return read("shaders/cortex/blit.vert");
    }

    private static String read(String path) {
        ResourceLocation location = new ResourceLocation("gothaj", path);
        try (InputStream in = Minecraft.getMinecraft().getResourceManager()
                .getResource(location).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cortex: could not read " + location, e);
        }
    }

    /** Uploads a block's values into the currently bound program. */
    public static void apply(ShaderProgram program, UniformBlock block) {
        if (block == null) return;

        // Indexed rather than enhanced-for: the latter allocates an iterator per effect.
        List<UniformBlock.Entry> entries = block.entries();

        for (int i = 0; i < entries.size(); i++) {
            UniformBlock.Entry entry = entries.get(i);

            switch (entry.kind()) {
                case FLOAT1 -> program.set(entry.name(), entry.values()[0]);
                case FLOAT2 -> program.set(entry.name(), entry.values()[0], entry.values()[1]);
                case FLOAT3 -> GL20.glUniform3f(program.uniformLocation(entry.name()),
                        entry.values()[0], entry.values()[1], entry.values()[2]);
                case FLOAT4 -> program.set(entry.name(), entry.values()[0], entry.values()[1],
                        entry.values()[2], entry.values()[3]);
                case INT1 -> program.set(entry.name(), entry.intValue());
            }
        }
    }

    public void dispose() {
        for (ShaderProgram program : programs.values()) {
            program.dispose();
        }
        programs.clear();
        pending.clear();
        failed.clear();
    }
}
