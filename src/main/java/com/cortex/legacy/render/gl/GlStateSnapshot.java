package com.cortex.legacy.render.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Records the GL state Cortex is about to change, so it can be put back exactly.
 *
 * <h2>Why not just set sensible defaults</h2>
 * Minecraft's {@code GlStateManager} keeps its own copy of the GL state and skips any call
 * that would be redundant against it. Once the engine changes something behind its back
 * that cache is wrong, and the obvious repair — calling {@code GlStateManager.enableDepth()}
 * on the way out — does nothing at all, because the cache still says depth is enabled and
 * the call short-circuits. Depth then stays off for everything the game draws next.
 *
 * <p>Restoring the real state to what it was avoids that trap entirely: the cache was
 * consistent with reality before the engine ran, so making reality match again makes the
 * cache correct by construction. Nothing has to be guessed, and no state the engine
 * happens to touch in future can be forgotten here without the same protection.
 */
public final class GlStateSnapshot {

    private boolean blend;
    private boolean depthTest;
    private boolean cullFace;
    private boolean scissor;
    private boolean alphaTest;
    private boolean depthMask;
    private boolean lineSmooth;

    private int depthFunc;
    private float lineWidth;

    private int blendSrcRgb;
    private int blendDstRgb;
    private int blendSrcAlpha;
    private int blendDstAlpha;

    private int activeTexture;
    private int boundTexture;

    private int program;
    private int vertexArray;
    private int arrayBuffer;
    private int elementBuffer;
    private int framebuffer;

    private final int[] viewport = new int[4];

    /** Reads the current state. Called once per frame, not per draw. */
    public void capture() {
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        // Touched by the world pass, which draws occluded geometry and antialiased lines.
        // Line smoothing in particular is not routed through GlStateManager at all, so
        // leaving it on would silently change how the game's own lines rasterise.
        lineSmooth = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
        depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);

        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        // The engine only ever binds textures on unit 0, so unit 0 is what has to be
        // recorded — and a texture binding is only readable from the active unit, so the
        // unit has to be selected to read it and put back afterwards. Reading the binding
        // under whatever unit happened to be active would record the wrong texture and
        // restore it to the wrong place, clobbering unit 0 and leaving the other unit
        // holding one of ours.
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(activeTexture);

        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
    }

    /** Puts everything back the way {@link #capture()} found it. */
    public void restore() {
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        // Unit 0 is the only one the engine binds to, so it is the only one to put back —
        // and it has to be the active unit while that happens. Whichever unit the game had
        // selected is then restored, since that is part of the state too.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTexture);
        GL13.glActiveTexture(activeTexture);

        setEnabled(GL11.GL_BLEND, blend);
        setEnabled(GL11.GL_DEPTH_TEST, depthTest);
        setEnabled(GL11.GL_CULL_FACE, cullFace);
        setEnabled(GL11.GL_SCISSOR_TEST, scissor);
        setEnabled(GL11.GL_ALPHA_TEST, alphaTest);
        setEnabled(GL11.GL_LINE_SMOOTH, lineSmooth);

        GL11.glDepthFunc(depthFunc);
        GL11.glLineWidth(lineWidth);
        GL11.glDepthMask(depthMask);
        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GL11.glColorMask(true, true, true, true);
    }

    /** The texture that was bound on unit 0. */
    public int boundTexture() {
        return boundTexture;
    }

    /** Whether alpha testing was on. */
    public boolean alphaTest() {
        return alphaTest;
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }
}
