#version 330 core

layout (location = 0) in vec2 a_Pos;       // screen position, logical px, already rotated
layout (location = 1) in vec2 a_Local;     // unrotated offset from the shape's centre
layout (location = 2) in vec2 a_HalfSize;  // half extent, ellipse radii, or triangle point 0
layout (location = 3) in vec4 a_Radii;     // corner radii, or triangle points 1 and 2
layout (location = 4) in vec4 a_Meta;      // kind, thickness, glowRadius, gradient position
layout (location = 5) in vec4 a_Color;
layout (location = 6) in vec4 a_Color2;    // gradient end; equals a_Color for a flat fill

uniform vec2 u_Viewport;                   // logical px

out vec2 v_Local;
out vec2 v_HalfSize;
out vec4 v_Radii;
out vec4 v_Meta;
out vec4 v_Color;
out vec4 v_Color2;

void main() {
    // Logical pixels to clip space, with y running downward to match GUI coordinates.
    gl_Position = vec4(
        (a_Pos.x / u_Viewport.x) * 2.0 - 1.0,
        1.0 - (a_Pos.y / u_Viewport.y) * 2.0,
        0.0, 1.0);

    v_Local = a_Local;
    v_HalfSize = a_HalfSize;
    v_Radii = a_Radii;
    v_Meta = a_Meta;
    v_Color = a_Color;
    v_Color2 = a_Color2;
}
