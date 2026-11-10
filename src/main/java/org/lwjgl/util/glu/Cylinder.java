package org.lwjgl.util.glu;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.GLU_INSIDE;

/**
 * Cylinder utility class for LWJGL3 and Java 24
 * Ported from LWJGL2 GLU utilities
 */
public class Cylinder extends Quadric {
    
    public void draw(float baseRadius, float topRadius, float height, int slices, int stacks) {
        float nsign = (orientation == GLU_INSIDE) ? -1.0F : 1.0F;
        
        float da = ((float) Math.PI * 2F) / (float) slices;
        float dr = (topRadius - baseRadius) / (float) stacks;
        float dz = height / (float) stacks;
        float nz = (baseRadius - topRadius) / height;
        
        if (drawStyle == GLU_POINT) {
            glBegin(GL_POINTS);
            
            for (int i = 0; i < slices; ++i) {
                float x = this.cos((float) i * da);
                float y = this.sin((float) i * da);
                this.normal3f(x * nsign, y * nsign, nz * nsign);
                float z = 0.0F;
                float r = baseRadius;
                
                for (int j = 0; j <= stacks; ++j) {
                    glVertex3f(x * r, y * r, z);
                    z += dz;
                    r += dr;
                }
            }
            
            glEnd();
        } else if (drawStyle == GLU_LINE || drawStyle == GLU_SILHOUETTE) {
            if (drawStyle == GLU_LINE) {
                float z = 0.0F;
                float r = baseRadius;
                
                for (int j = 0; j <= stacks; ++j) {
                    glBegin(GL_LINE_LOOP);
                    
                    for (int i = 0; i < slices; ++i) {
                        float x = this.cos((float) i * da);
                        float y = this.sin((float) i * da);
                        this.normal3f(x * nsign, y * nsign, nz * nsign);
                        glVertex3f(x * r, y * r, z);
                    }
                    
                    glEnd();
                    z += dz;
                    r += dr;
                }
            } else if (baseRadius != 0.0F) {
                glBegin(GL_LINE_LOOP);
                
                for (int i = 0; i < slices; ++i) {
                    float x = this.cos((float) i * da);
                    float y = this.sin((float) i * da);
                    this.normal3f(x * nsign, y * nsign, nz * nsign);
                    glVertex3f(x * baseRadius, y * baseRadius, 0.0F);
                }
                
                glEnd();
                glBegin(GL_LINE_LOOP);
                
                for (int i = 0; i < slices; ++i) {
                    float x = this.cos((float) i * da);
                    float y = this.sin((float) i * da);
                    this.normal3f(x * nsign, y * nsign, nz * nsign);
                    glVertex3f(x * topRadius, y * topRadius, height);
                }
                
                glEnd();
            }
            
            glBegin(GL_LINES);
            
            for (int i = 0; i < slices; ++i) {
                float x = this.cos((float) i * da);
                float y = this.sin((float) i * da);
                this.normal3f(x * nsign, y * nsign, nz * nsign);
                glVertex3f(x * baseRadius, y * baseRadius, 0.0F);
                glVertex3f(x * topRadius, y * topRadius, height);
            }
            
            glEnd();
        } else if (drawStyle == GLU_FILL) {
            float ds = 1.0F / (float) slices;
            float dt = 1.0F / (float) stacks;
            float t = 0.0F;
            float z = 0.0F;
            float r = baseRadius;
            
            for (int j = 0; j < stacks; ++j) {
                float s = 0.0F;
                glBegin(GL_TRIANGLE_STRIP);
                
                for (int i = 0; i <= slices; ++i) {
                    float x, y;
                    if (i == slices) {
                        x = this.sin(0.0F);
                        y = this.cos(0.0F);
                    } else {
                        x = this.sin((float) i * da);
                        y = this.cos((float) i * da);
                    }
                    
                    if (nsign == 1.0F) {
                        this.normal3f(x * nsign, y * nsign, nz * nsign);
                        this.TXTR_COORD(s, t);
                        glVertex3f(x * r, y * r, z);
                        this.normal3f(x * nsign, y * nsign, nz * nsign);
                        this.TXTR_COORD(s, t + dt);
                        glVertex3f(x * (r + dr), y * (r + dr), z + dz);
                    } else {
                        this.normal3f(x * nsign, y * nsign, nz * nsign);
                        this.TXTR_COORD(s, t);
                        glVertex3f(x * r, y * r, z);
                        this.normal3f(x * nsign, y * nsign, nz * nsign);
                        this.TXTR_COORD(s, t + dt);
                        glVertex3f(x * (r + dr), y * (r + dr), z + dz);
                    }
                    
                    s += ds;
                }
                
                glEnd();
                r += dr;
                t += dt;
                z += dz;
            }
        }
    }
}
