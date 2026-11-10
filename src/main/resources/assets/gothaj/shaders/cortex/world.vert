#version 330 core

// Geometry drawn in the world, during the dimension pass rather than the overlay.
//
// Positions arrive camera-relative: the caller has already subtracted the render
// manager's viewer position in double precision, because a float world coordinate
// out at the far edge of a Minecraft map has metre-scale error and boxes visibly
// wobble as the player walks. u_ViewProj is Minecraft's own projection times its
// modelview, read straight back out of GL, so this geometry lands in exactly the
// same space the game's own world rendering does.

layout (location = 0) in vec3 a_Pos;
layout (location = 1) in vec2 a_UV;
layout (location = 2) in vec4 a_Color;

uniform mat4 u_ViewProj;

out vec2 v_UV;
out vec4 v_Color;

void main() {
    v_UV = a_UV;
    v_Color = a_Color;

    gl_Position = u_ViewProj * vec4(a_Pos, 1.0);
}
