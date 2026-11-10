package org.lwjgl.util.glu;

import lombok.Getter;
import lombok.Setter;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.GLU_OUTSIDE;
import static org.lwjgl.util.glu.GLU.GLU_SMOOTH;

/**
 * Quadric utility class for LWJGL3 and Java 24
 * Ported from LWJGL2 GLU utilities
 */
@Setter
@Getter
public class Quadric {

    public static final int GLU_POINT = 100010;
    public static final int GLU_LINE = 100011;
    public static final int GLU_FILL = 100012;
    public static final int GLU_SILHOUETTE = 100013;

    protected int drawStyle = GLU_FILL;
    protected int orientation = GLU_OUTSIDE;
    protected boolean textureFlag = false;
    protected int normals = GLU_SMOOTH;

    protected void normal3f(float x, float y, float z) {
        float mag = (float) Math.sqrt(x * x + y * y + z * z);
        if (mag > 1.0E-5F) {
            x /= mag;
            y /= mag;
            z /= mag;
        }
        glNormal3f(x, y, z);
    }

    protected void TXTR_COORD(float x, float y) {
        if (this.textureFlag) {
            glTexCoord2f(x, y);
        }
    }


    protected float sin(float r) {
        return (float) Math.sin(r);
    }

    protected float cos(float r) {
        return (float) Math.cos(r);
    }
}
