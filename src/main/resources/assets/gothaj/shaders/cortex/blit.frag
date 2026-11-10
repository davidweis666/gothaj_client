#version 330 core

// Composites a blurred copy of the screen back over the frame, masked to a rounded
// rectangle and optionally tinted.

in vec2 v_Local;
in vec2 v_SourceUV;

uniform sampler2D u_Texture;
uniform vec2 u_HalfSize;
uniform vec4 u_Radii;       // top-left, top-right, bottom-right, bottom-left
uniform vec4 u_Tint;        // rgb tint, alpha = how far to pull the backdrop toward it

out vec4 fragColor;

float pickRadius(vec2 p, vec4 r) {
    float top = p.x < 0.0 ? r.x : r.y;
    float bottom = p.x < 0.0 ? r.w : r.z;
    return p.y < 0.0 ? top : bottom;
}

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    float rad = min(pickRadius(p, r), min(b.x, b.y));
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - rad;
}

void main() {
    float d = sdRoundedBox(v_Local, u_HalfSize, u_Radii);
    float edge = max(fwidth(d), 1e-4);
    float mask = 1.0 - smoothstep(-edge, edge, d);

    if (mask <= 0.0) discard;

    vec3 rgb = texture(u_Texture, v_SourceUV).rgb;
    rgb = mix(rgb, u_Tint.rgb, u_Tint.a);

    fragColor = vec4(rgb, mask);
}
