#version 330 core

// Shared by images and MSDF text — both are textured quads and differ only in how the
// fragment stage reads the sampled texel.

layout (location = 0) in vec2 a_Pos;     // logical px
layout (location = 1) in vec2 a_UV;
layout (location = 2) in float a_GradT;  // position along the gradient axis
layout (location = 3) in vec4 a_Color;
layout (location = 4) in vec4 a_Color2;  // equals a_Color when there is no gradient

uniform vec2 u_Viewport;

out vec2 v_UV;
out vec4 v_Color;
out vec4 v_Color2;
out float v_GradT;

void main() {
    gl_Position = vec4(
        (a_Pos.x / u_Viewport.x) * 2.0 - 1.0,
        1.0 - (a_Pos.y / u_Viewport.y) * 2.0,
        0.0, 1.0);

    v_UV = a_UV;

    // The stops are resolved per fragment rather than here: an animated gradient folds its
    // position back on itself as it scrolls, and that fold has to happen after
    // interpolation to stay smooth across the quad.
    v_Color = a_Color;
    v_Color2 = a_Color2;
    v_GradT = a_GradT;
}
