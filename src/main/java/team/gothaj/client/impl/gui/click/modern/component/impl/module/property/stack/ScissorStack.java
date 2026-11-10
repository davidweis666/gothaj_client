package team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ScissorStack {
    private static final Deque<Rectangle> stack = new ArrayDeque<>();

    private static Rectangle toRect(float x0, float y0, float x1, float y1, ScaledResolution sr) {
        int scale = sr.getScaleFactor();
        int sx = Math.round(x0 * scale);
        int sy = Math.round((sr.getScaledHeight() - y1) * scale); // invert Y
        int sw = Math.round((x1 - x0) * scale);
        int sh = Math.round((y1 - y0) * scale);
        if (sw < 0) { sx += sw; sw = -sw; }
        if (sh < 0) { sy += sh; sh = -sh; }
        return new Rectangle(sx, sy, Math.max(0, sw), Math.max(0, sh));
    }

    private static Rectangle intersect(Rectangle a, Rectangle b) {
        if (a == null) return b;
        if (b == null) return a;
        Rectangle r = a.intersection(b);
        return r.isEmpty() ? new Rectangle(0, 0, 0, 0) : r;
    }

    public static void push(float x0, float y0, float x1, float y1, ScaledResolution sr) {
        Rectangle next = toRect(x0, y0, x1, y1, sr);
        Rectangle applied = stack.isEmpty() ? next : intersect(stack.peek(), next);
        stack.push(applied);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(applied.x, applied.y, applied.width, applied.height);
    }

    public static void pop() {
        if (stack.isEmpty()) return;
        stack.pop();
        if (stack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            Rectangle r = stack.peek();
            GL11.glScissor(r.x, r.y, r.width, r.height);
        }
    }
}
