#version 330 core

// Draws one region-sized quad from gl_VertexID as a 4-vertex triangle strip, so
// compositing a backdrop panel needs no vertex buffer.

uniform vec4 u_Region;      // x, y, w, h in logical px
uniform vec2 u_Viewport;    // logical px
uniform vec4 u_UvRect;      // u0, v0, u1, v1 of this region inside the source texture

out vec2 v_Local;           // offset from the region's centre, for the rounded-rect mask
out vec2 v_SourceUV;        // where to read the captured backdrop

void main() {
    vec2 corner = vec2(gl_VertexID == 1 || gl_VertexID == 3 ? 1.0 : 0.0,
                       gl_VertexID >= 2 ? 1.0 : 0.0);

    vec2 pos = u_Region.xy + corner * u_Region.zw;

    v_Local = pos - (u_Region.xy + u_Region.zw * 0.5);

    // The caller supplies the source rectangle already flipped, because the backdrop is
    // read out of a framebuffer whose origin is bottom-left while these coordinates run
    // top-down. Letting it map the corners also means the source can be a small crop
    // around this panel rather than the whole screen.
    v_SourceUV = vec2(mix(u_UvRect.x, u_UvRect.z, corner.x),
                      mix(u_UvRect.y, u_UvRect.w, corner.y));

    gl_Position = vec4((pos.x / u_Viewport.x) * 2.0 - 1.0,
                       1.0 - (pos.y / u_Viewport.y) * 2.0,
                       0.0, 1.0);
}
