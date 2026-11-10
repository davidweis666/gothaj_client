#version 330 core

in vec2 v_UV;
in vec4 v_Color;
in vec4 v_Color2;
in float v_GradT;

uniform sampler2D u_Texture;

out vec4 fragColor;

// Mirrors every 2 units, so a gradient that scrolls past either end folds back instead of
// jumping. Over the 0..1 a static gradient uses, this is the identity.
float gradientBlend(float t) {
    return abs(mod(t + 1.0, 2.0) - 1.0);
}

void main() {
    // The vertex colour doubles as a tint, so a white image can be recoloured and a
    // gradient can be laid over it without a second pass.
    vec4 tint = mix(v_Color, v_Color2, gradientBlend(v_GradT));
    fragColor = texture(u_Texture, v_UV) * tint;
}
