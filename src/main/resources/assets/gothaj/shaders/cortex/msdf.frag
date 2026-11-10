#version 330 core

// Multi-channel signed distance field text.
//
// Each channel stores the distance to a differently coloured subset of the glyph's edges.
// Taking the median of the three recovers a distance that stays correct where two edges
// meet, which is what keeps corners sharp instead of rounding them off the way a
// single-channel field does.

in vec2 v_UV;
in vec4 v_Color;
in vec4 v_Color2;
in float v_GradT;

uniform sampler2D u_Atlas;
uniform vec2 u_AtlasSize;
uniform float u_PxRange;   // width of the distance ramp, in atlas texels

out vec4 fragColor;

float median(vec3 rgb) {
    return max(min(rgb.r, rgb.g), min(max(rgb.r, rgb.g), rgb.b));
}

// Mirrors every 2 units, so a gradient that scrolls past either end folds back instead of
// jumping. Over the 0..1 a static gradient uses, this is the identity.
float gradientBlend(float t) {
    return abs(mod(t + 1.0, 2.0) - 1.0);
}

void main() {
    vec3 sampled = texture(u_Atlas, v_UV).rgb;
    float distance = median(sampled) - 0.5;

    // The decoded value spans u_PxRange atlas texels, so scaling it by how many screen
    // pixels one texel covers puts the distance in screen pixels — which is what keeps the
    // edge exactly one pixel wide at any size. Taking the smaller axis stays crisp when the
    // text is scaled unevenly. The floor of 1.0 stops very small text from vanishing.
    vec2 pixelsPerTexel = vec2(1.0) / fwidth(v_UV * u_AtlasSize);
    float screenPxRange = max(u_PxRange * min(pixelsPerTexel.x, pixelsPerTexel.y), 1.0);

    float alpha = clamp(distance * screenPxRange + 0.5, 0.0, 1.0);
    if (alpha <= 0.0) discard;

    vec4 color = mix(v_Color, v_Color2, gradientBlend(v_GradT));
    fragColor = vec4(color.rgb, color.a * alpha);
}
