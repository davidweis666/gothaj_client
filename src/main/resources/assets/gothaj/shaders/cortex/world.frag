#version 330 core

// Flat colour, optionally modulated by a sprite. Deliberately unlit and unfogged:
// this draws overlays — ESP boxes, tracers, markers — which exist to be seen through
// the world's own atmosphere rather than to sit inside it.

in vec2 v_UV;
in vec4 v_Color;

uniform sampler2D u_Texture;

// Set for billboards drawn from a sprite; untextured geometry leaves it false rather
// than binding a white 1x1, which would cost a texture swap between every batch.
uniform bool u_Textured;

out vec4 fragColor;

void main() {
    vec4 color = v_Color;

    if (u_Textured) {
        color *= texture(u_Texture, v_UV);
    }

    // Fully transparent fragments are dropped rather than blended. With depth writes
    // enabled for occluded geometry, blending them would stamp the depth buffer with
    // an invisible quad and punch a hole in whatever is drawn behind it later.
    if (color.a <= 0.0) discard;

    fragColor = color;
}
