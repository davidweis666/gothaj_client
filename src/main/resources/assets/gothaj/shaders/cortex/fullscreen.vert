#version 330 core

// Covers the viewport with a single oversized triangle generated from gl_VertexID, so
// effect passes need no vertex buffer at all — only an empty VAO has to be bound.

out vec2 v_UV;

void main() {
    vec2 p = vec2(gl_VertexID == 1 ? 3.0 : -1.0,
                  gl_VertexID == 2 ? 3.0 : -1.0);

    v_UV = (p + 1.0) * 0.5;
    gl_Position = vec4(p, 0.0, 1.0);
}
