#version 330 core

// Dual-Kawase downsample: a 5-tap diamond that halves resolution each pass. Chaining
// these is far cheaper than a wide Gaussian for the large radii a frosted-glass panel
// wants, because most of the work happens at a fraction of the pixel count.

in vec2 v_UV;

uniform sampler2D u_Texture;
uniform vec2 u_HalfPixel;   // 0.5 / source resolution
uniform float u_Offset;

out vec4 fragColor;

void main() {
    vec2 o = u_HalfPixel * u_Offset;

    vec4 sum = texture(u_Texture, v_UV) * 4.0;
    sum += texture(u_Texture, v_UV - o);
    sum += texture(u_Texture, v_UV + o);
    sum += texture(u_Texture, v_UV + vec2(o.x, -o.y));
    sum += texture(u_Texture, v_UV - vec2(o.x, -o.y));

    fragColor = sum / 8.0;
}
