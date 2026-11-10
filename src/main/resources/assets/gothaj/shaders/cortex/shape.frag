#version 330 core

// One shader for every Cortex primitive. The shape's identity arrives per-vertex and
// selects a signed-distance function below, which is what lets an entire HUD of mixed
// rectangles, circles, lines and triangles collapse into a single draw call.

in vec2 v_Local;
in vec2 v_HalfSize;
in vec4 v_Radii;
in vec4 v_Meta;      // kind, thickness, glowRadius, gradient position
in vec4 v_Color;
in vec4 v_Color2;

out vec4 fragColor;

const int KIND_BOX = 0;
const int KIND_ELLIPSE = 1;
const int KIND_TRIANGLE = 2;

/** How sharply a glow fades. Higher concentrates it nearer the shape. */
const float GLOW_DECAY = 3.0;

// Radii arrive as (top-left, top-right, bottom-right, bottom-left) in a space where +y
// points down, so each fragment picks the radius belonging to its own quadrant.
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

// Gradient-corrected approximation: exact for circles, and within a fraction of a pixel
// for the eccentricities a UI actually uses.
float sdEllipse(vec2 p, vec2 ab) {
    vec2 safe = max(ab, vec2(1e-6));
    vec2 n = p / safe;
    float k1 = length(n);
    float k2 = length(n / safe);
    return (k1 - 1.0) / max(k2, 1e-6);
}

// Mirrors the gradient position every 2 units. Over the 0..1 a static gradient uses this
// is the identity, and an animated one that slides past either end folds back instead of
// jumping — which is what makes the scroll seamless in both directions.
float gradientBlend(float t) {
    return abs(mod(t + 1.0, 2.0) - 1.0);
}

float sdTriangle(vec2 p, vec2 p0, vec2 p1, vec2 p2) {
    vec2 e0 = p1 - p0, e1 = p2 - p1, e2 = p0 - p2;
    vec2 v0 = p - p0, v1 = p - p1, v2 = p - p2;

    vec2 pq0 = v0 - e0 * clamp(dot(v0, e0) / dot(e0, e0), 0.0, 1.0);
    vec2 pq1 = v1 - e1 * clamp(dot(v1, e1) / dot(e1, e1), 0.0, 1.0);
    vec2 pq2 = v2 - e2 * clamp(dot(v2, e2) / dot(e2, e2), 0.0, 1.0);

    float s = sign(e0.x * e2.y - e0.y * e2.x);
    vec2 d = min(min(vec2(dot(pq0, pq0), s * (v0.x * e0.y - v0.y * e0.x)),
                     vec2(dot(pq1, pq1), s * (v1.x * e1.y - v1.y * e1.x))),
                     vec2(dot(pq2, pq2), s * (v2.x * e2.y - v2.y * e2.x)));

    return -sqrt(d.x) * sign(d.y);
}

void main() {
    int kind = int(v_Meta.x + 0.5);
    float thickness = v_Meta.y;
    float glowRadius = v_Meta.z;

    float d;
    if (kind == KIND_ELLIPSE) {
        d = sdEllipse(v_Local, v_HalfSize);
    } else if (kind == KIND_TRIANGLE) {
        d = sdTriangle(v_Local, v_HalfSize, v_Radii.xy, v_Radii.zw);
    } else {
        d = sdRoundedBox(v_Local, v_HalfSize, v_Radii);
    }

    // A signed distance field has unit gradient, so fwidth(d) is exactly one screen pixel
    // measured in the same units as d. Deriving the filter width from d itself — never from
    // the outline-folded distance below — is what keeps edges smooth: abs() reverses the
    // gradient across the middle of a stroke, and fwidth of that spikes to roughly twice
    // the stroke width, smearing both of its edges.
    float px = max(fwidth(d), 1e-5);

    float alpha;

    if (thickness < 0.0) {
        // Glow: falls off outside the boundary only. Masking the interior keeps a
        // translucent shape drawn on top from picking up a bright core underneath it.
        float outside = smoothstep(-px, px, d);
        float t = clamp(max(d, 0.0) / max(glowRadius, 1e-4), 0.0, 1.0);

        // An exponential curve, rebased so it reaches exactly zero at the glow's edge
        // rather than merely approaching it. A plain exp() is still around 5% opaque where
        // the geometry runs out, and because that geometry is a rectangle, the leftover
        // shows up as a faint box framing the glow.
        float tail = exp(-GLOW_DECAY);
        float falloff = (exp(-GLOW_DECAY * t) - tail) / (1.0 - tail);

        alpha = outside * falloff;
    } else if (thickness > 0.0) {
        // Outline: the band straddling the boundary. Hairlines are widened to a single
        // pixel and faded by however much they were widened, so a sub-pixel stroke reads
        // as a faint continuous line instead of breaking up.
        float half = thickness * 0.5;
        float coverage = min(half / (px * 0.5), 1.0);
        float band = abs(d) - max(half, px * 0.5);

        alpha = (1.0 - smoothstep(-px * 0.5, px * 0.5, band)) * coverage;
    } else {
        alpha = 1.0 - smoothstep(-px * 0.5, px * 0.5, d);
    }

    if (alpha <= 0.0) discard;

    vec4 color = mix(v_Color, v_Color2, gradientBlend(v_Meta.w));
    fragColor = vec4(color.rgb, color.a * alpha);
}
