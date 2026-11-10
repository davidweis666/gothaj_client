package com.cortex.legacy.render.gl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A compiled and linked GLSL program.
 *
 * <p>Sources are loaded through Minecraft's resource manager so shaders live alongside
 * the client's other assets and reload with them. Uniform locations are resolved lazily
 * and cached, since {@code glGetUniformLocation} is a synchronising driver call.
 */
public final class ShaderProgram {

    private final int id;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    private ShaderProgram(int id) {
        this.id = id;
    }

    /**
     * Compiles {@code assets/gothaj/shaders/cortex/<name>.vert} and {@code .frag} into a program.
     *
     * @throws IllegalStateException if either stage fails to compile or the program fails to link
     */
    public static ShaderProgram load(String name) {
        return load(name, name);
    }

    /**
     * Compiles a program from separately named stages, so effect passes can share one
     * fullscreen vertex shader.
     */
    public static ShaderProgram load(String vertexName, String fragmentName) {
        return fromSource(vertexName + "+" + fragmentName,
                read(vertexName + ".vert"), read(fragmentName + ".frag"));
    }

    /** Builds a program from GLSL held in memory, for effects assembled at runtime. */
    public static ShaderProgram fromSource(String label, String vertexSource, String fragmentSource) {
        GlContext.requireCurrent("compiling shader '" + label + "'");

        int vert = compile(GL20.GL_VERTEX_SHADER, vertexSource, label + " (vertex)");
        int frag = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource, label + " (fragment)");

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        // Shaders are reference-counted by the program; detaching and deleting here frees the
        // compiler-side objects as soon as linking is done.
        GL20.glDetachShader(program, vert);
        GL20.glDetachShader(program, frag);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 8192);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("Cortex: failed to link '" + label + "': " + log);
        }

        return new ShaderProgram(program);
    }

    private static int compile(int stage, String source, String debugName) {
        int shader = GL20.glCreateShader(stage);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 8192);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Cortex: failed to compile '" + debugName + "': " + log);
        }
        return shader;
    }

    private static String read(String fileName) {
        ResourceLocation location = new ResourceLocation("gothaj", "shaders/cortex/" + fileName);
        try (InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cortex: could not read shader resource " + location, e);
        }
    }

    public void bind() {
        GL20.glUseProgram(id);
    }

    /** Resolves a uniform location, for callers that need to issue a typed call themselves. */
    public int uniformLocation(String name) {
        return uniform(name);
    }

    private int uniform(String name) {
        Integer cached = uniformCache.get(name);
        if (cached != null) return cached;

        int location = GL20.glGetUniformLocation(id, name);
        uniformCache.put(name, location);
        return location;
    }

    public void set(String name, float v) {
        GL20.glUniform1f(uniform(name), v);
    }

    public void set(String name, int v) {
        GL20.glUniform1i(uniform(name), v);
    }

    public void set(String name, float x, float y) {
        GL20.glUniform2f(uniform(name), x, y);
    }

    public void set(String name, float x, float y, float z, float w) {
        GL20.glUniform4f(uniform(name), x, y, z, w);
    }

    /**
     * Uploads a 4x4 matrix from a buffer already in column-major order, which is both what
     * GL expects and what JOML's {@code get} produces — so no transpose is needed.
     */
    public void setMatrix(String name, FloatBuffer value) {
        GL20.glUniformMatrix4fv(uniform(name), false, value);
    }

    /** Uploads a packed ARGB colour as a straight-alpha RGBA vec4. */
    public void setColor(String name, int argb) {
        GL20.glUniform4f(uniform(name),
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }

    public void dispose() {
        GL20.glDeleteProgram(id);
        uniformCache.clear();
    }
}
