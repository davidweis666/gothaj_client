package team.gothaj.client.api.util.render.states;

import org.lwjgl.opengl.GL30;

import java.util.Stack;

public class GlStates {
    private static final Stack<GlState> GL_STATES = new Stack<>();

    public static void push() {
        GlStates.GL_STATES.push(new GlState(getGLVersion()).push());
    }

    public static void pop() {
        if (!GlStates.GL_STATES.empty()) {
            GlStates.GL_STATES.pop().pop();
        }
    }

    public static int getGLVersion() {
        int[] major = new int[1];
        int[] minor = new int[1];
        GL30.glGetIntegerv(GL30.GL_MAJOR_VERSION, major);
        GL30.glGetIntegerv(GL30.GL_MINOR_VERSION, minor);
        return major[0] * 100 + minor[0] * 10;
    }
}
