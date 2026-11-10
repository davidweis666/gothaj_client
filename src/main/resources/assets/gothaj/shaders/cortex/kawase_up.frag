#version 330 core

// Dual-Kawase upsample: an 8-tap tent that doubles resolution each pass, undoing the
// downsample chain while spreading the blur further.

in vec2 v_UV;

uniform sampler2D u_Texture;
uniform vec2 u_HalfPixel;   // 0.5 / source resolution
uniform float u_Offset;

out vec4 fragColor;

void main() {
    vec2 o = u_HalfPixel * u_Offset;

    vec4 sum = texture(u_Texture, v_UV + vec2(-o.x * 2.0, 0.0));
    sum += texture(u_Texture, v_UV + vec2(-o.x, o.y)) * 2.0;
    sum += texture(u_Texture, v_UV + vec2(0.0, o.y * 2.0));
    sum += texture(u_Texture, v_UV + vec2(o.x, o.y)) * 2.0;
    sum += texture(u_Texture, v_UV + vec2(o.x * 2.0, 0.0));
    sum += texture(u_Texture, v_UV + vec2(o.x, -o.y)) * 2.0;
    sum += texture(u_Texture, v_UV + vec2(0.0, -o.y * 2.0));
    sum += texture(u_Texture, v_UV + vec2(-o.x, -o.y)) * 2.0;

    fragColor = sum / 12.0;
}
