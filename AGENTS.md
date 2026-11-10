# Cortex — agent instructions

Scope: `src/main/java/com/cortex/legacy/` and `src/main/resources/assets/gothaj/shaders/cortex/`.
Applies when using Cortex to draw, or when editing the engine itself.

Cortex is a batched, shader-driven renderer for Gothaj (Minecraft 1.8.9, LWJGL 3, OpenGL 3.3+).
It replaces the immediate-mode helpers in `RenderUtil` / `ShaderUtil`.

Two passes, separately bracketed and independently disabled on failure:

| Pass | Event | Space | Entry point |
|---|---|---|---|
| **Overlay (2D)** | `RenderOverlayEvent` | Logical GUI px, origin top-left, y down | `Cortex.*` |
| **World (3D)** | `RenderDimensionEvent` | Absolute world coordinates | `Cortex.world().*` |

---

## Hard rules

1. **Draw in the pass that owns the space.** 2D from a `RenderOverlayEvent` handler, 3D from a
   `RenderDimensionEvent` handler. Draw calls made outside their bracket are silently ignored —
   this is the first thing to check when nothing appears.
2. **Never call `Cortex.init()`, `beginFrame()`, `endFrame()`, `beginWorld()` or `endWorld()`.**
   Already wired — `init()` in `GothajClient.init()`, the overlay bracket in
   `GuiIngame.renderGameOverlay`, the world bracket in `EntityRenderer.renderWorldPass`.
3. **Emit `glow(...)` before the shape it wraps.** Glow covers only the region outside the
   boundary.
4. **A custom effect must declare its own uniforms in GLSL.** The engine cannot know their names.
5. **Hoist `Region` / `Corners` / `Gradient` / `TextStyle` / `Color` to `static final`.** The
   engine allocates nothing per frame; rebuilding value types in the render loop is the only way
   that gets lost.
6. **Registration (`fonts()`, `shaders()`) is safe from any thread. Drawing is not.** GL work is
   deferred to first use on the render thread.
7. **Do not add `RenderUtil` / `ShaderUtil` / `GlStateManager` calls inside the engine.** It owns
   its GL state; mixing reintroduces cache-desync bugs.

---

## File map

| Path | Holds |
|---|---|
| `Cortex.java` | Static facade: lifecycle, all draw methods, batch control |
| `draw/` | `Region`, `Corners`, `Gradient` — immutable value types |
| `render/batch/` | `ShapeBatcher` (SDF primitives), `QuadBatcher` (images + glyphs), `WorldBatcher` (3D tris + lines) |
| `world/` | `WorldDraw` (3D facade), `Box` (world AABB), `DepthMode` |
| `render/gl/` | `ShaderProgram`, `RenderTarget`, `RenderTargetPool`, `GlStateSnapshot`, `GlContext` |
| `effect/blur/` | `KawaseBlur` — dual-Kawase down/up chain |
| `effect/shader/` | `CustomShaders` (user GLSL + wrapper header), `UniformBlock` |
| `text/` | `FontManager`, `TextStyle`, `CortexFonts` (register fonts here) |
| `text/font/` | `BakedFont`, `FontBaker`, `Glyph` |
| `text/msdf/` | `MsdfGenerator` — msdfgen reimplementation |
| `image/` | `Image`, `AnimatedImage`, `ImageLoader` (PNG + GIF) |
| `assets/gothaj/shaders/cortex/` | GLSL 330 core: `shape`, `blit`, `textured`, `image`, `msdf`, `fullscreen`, `kawase_down`, `kawase_up`, `world` |

---

## API

Coordinates are **logical (GUI-scaled) pixels**, origin top-left, y down.
Colours are `java.awt.Color`; every method also accepts a packed ARGB `int` for interop with
`Hud.getColor(...)` / `ColorUtil.setAlpha(...)`.

### Shapes — batched into one draw call

```java
Cortex.rect(Region, Color)                                  // also (x, y, w, h, Color)
Cortex.roundedRect(Region, Corners, Color)
Cortex.outlineRect(Region, float thickness, Color)
Cortex.outlineRoundedRect(Region, Corners, float thickness, Color)
Cortex.circle(float cx, float cy, float radius, Color)
Cortex.outlineCircle(float cx, float cy, float radius, float thickness, Color)
Cortex.ellipse(float cx, float cy, float rx, float ry, Color)
Cortex.line(float x0, float y0, float x1, float y1, float thickness, Color)
Cortex.triangle(float x0, float y0, float x1, float y1, float x2, float y2, Color)
Cortex.outlineTriangle(..., float thickness, Color)
```

### Corners

```java
Corners.NONE, Corners.all(r), Corners.top(r), Corners.bottom(r), Corners.of(tl, tr, br, bl)
```

Per-corner radii render correctly. Clamped to half the shorter side automatically.

### Gradients

```java
Gradient.horizontal(Color, Color)          // or (int, int)
Gradient.vertical(Color, Color)
    .animate(float cyclesPerSecond)        // scrolls; driven by the frame clock

Cortex.gradientRect(Region, Corners, Gradient)
Cortex.gradientOutline(Region, Corners, float thickness, Gradient)
Cortex.gradientCircle(float cx, float cy, float radius, Gradient)
Cortex.gradientLine(float x0, float y0, float x1, float y1, float thickness, Gradient)
Cortex.gradientImage(Image, Region, Gradient)
TextStyle.gradient(Gradient)               // masks the whole run
```

### Glow

```java
Cortex.glow(Region, Corners, float radius, Color);        // FIRST
Cortex.roundedRect(Region, Corners, Color);               // THEN the shape
Cortex.glowCircle(float cx, float cy, float radius, float glowRadius, Color);
```

### Backdrop blur

```java
Cortex.blur(Region, Corners, float radius);
Cortex.blur(Region, Corners, float radius, Color tint);
```

`tint` alpha is a **mix amount** (how far the backdrop moves toward that colour), not opacity.
Radius is in logical pixels, scaled to the framebuffer internally.

### Text

```java
// Register once, in CortexFonts.loadFonts()
Cortex.fonts().registerSystem("name", "Arial");          // installed family
Cortex.fonts().register("name", "fonts/Inter.ttf");      // asset or file path

TextStyle.of("name", 14, Color.WHITE)
    .shadow()                          // or .shadow(Color, float offset)
    .align(TextStyle.Align.LEFT)       // LEFT | CENTER | RIGHT
    .baseline(TextStyle.Baseline.TOP)  // TOP | MIDDLE | ALPHABETIC
    .letterSpacing(0.5f)
    .gradient(g);

Cortex.text(float x, float y, String, TextStyle);
Cortex.text(float x, float y, String, Color, boolean shadow);   // vanilla font, slower
float w = Cortex.textWidth(String, TextStyle);
float h = Cortex.textHeight(TextStyle);
```

Baking an ASCII range takes a few hundred milliseconds and runs on a background thread. Until it
completes, `text(..., TextStyle)` falls back to Minecraft's bitmap font automatically. Do not block
on it, and do not assume a font is ready in the frame after registering.

### Images

```java
Cortex.image("gothaj/img/logo.png", Region);             // path forms below
Cortex.image("gothaj/img/logo.png", Region, Color tint);
Cortex.animation("gothaj/img/spin.gif", Region);         // frame chosen from the clock
Cortex.image(Image, Region, float u0, float v0, float u1, float v1, int argb);  // sprite sheet
```

Accepted paths: `gothaj/img/x.png` (as on disk), `img/x.png` (client-relative),
`namespace:path`, or an absolute file path. Decoded and cached on first use; failures cached too.

### Custom GLSL

```java
Cortex.shaders().register("name", body);                 // any thread; compiles on first draw
Cortex.shaders().registerFromResource("name", "file.frag");
Cortex.shader(Region, Corners, "name", UniformBlock);    // UniformBlock may be null
```

Body supplies only `cortexEffect`; the engine wraps version, varyings and the rounded-corner mask:

```glsl
uniform vec4 u_Accent;                    // REQUIRED: declare your own uniforms

vec4 cortexEffect(vec2 uv, vec2 local) {
    vec3 base = texture(u_Texture, uv).rgb;
    return vec4(mix(base, u_Accent.rgb, 0.5), 1.0);
}
```

`uv` samples the captured backdrop; `local` is the offset from the region centre in logical px.
Provided by the wrapper: `u_Texture`, `u_Time`, `u_Region`, `u_HalfSize`, `u_Radii`, `u_Resolution`.

---

## World pass (3D)

Drawn among the blocks, from a `RenderDimensionEvent` handler. Coordinates are **absolute world
coordinates** — the numbers `entity.posX` and `BlockPos` already carry. The engine subtracts the
camera in double precision before handing floats to the GPU, so never pre-subtract `viewerPos`
the way `RenderUtil` requires.

```java
// Boxes — ESP, ChestESP, block highlights
Cortex.world().box(Box, int fillArgb, int outlineArgb);   // fill + outline, one call
Cortex.world().box(Box, int argb);                        // fill only
Cortex.world().boxOutline(Box, int argb);                 // edges only
Cortex.world().progressBox(Box, float progress, int fill, int outline);   // Breaker-style

// Lines
Cortex.world().line(x0, y0, z0, x1, y1, z1, int argb);
Cortex.world().tracer(x, y, z, int argb);                 // from the camera to a point
Cortex.world().tracer(Box, int argb);                     // to the base centre of a box

// Billboards — the primitive a particle is made of
Cortex.world().billboard(x, y, z, float size, int argb);
Cortex.world().billboard(x, y, z, float size, Image, int tint);
Cortex.world().billboard(x, y, z, float size, Image, u0, v0, u1, v1, int tint);

// Batch state — each change submits what is queued, so group draws by these
Cortex.world().depth(DepthMode.THROUGH_WALLS);            // default; or OCCLUDED
Cortex.world().lineWidth(2f);
```

Building a `Box`:

```java
Box.ofEntity(entity, partialTicks)   // interpolated — does NOT lag behind a moving model
Box.ofBlock(blockPos)                // the unit cube
Box.ofSize(x, y, z, sx, sy, sz)      // corner + extent
Box.of(axisAlignedBB)
box.inflate(0.05).offset(0, 0.1, 0)
```

**Batching.** Draws merge across `(depth mode, texture, line width)`; changing any of the three
submits. Within one batch, triangles are always submitted before lines, so a box's outline lands
on top of its own fill regardless of call order. A whole ESP of mixed fills and outlines is two
draw calls.

**The engine draws particles; it does not simulate them.** Spawning, moving and expiring belongs
to the module that owns the behaviour, which draws each survivor once a frame via `billboard`.

**Translucency is not sorted.** Overlay geometry does not write depth, so two overlapping
translucent boxes blend in submission order rather than by distance. Invisible for ESP volumes;
worth knowing before building anything that leans on correct see-through layering.

---

## Canonical module

```java
public class Example extends Module {

    private static final Region PANEL  = Region.of(10, 10, 170, 90);
    private static final Corners ROUND = Corners.all(10);
    private static final Color TINT    = new Color(0, 0, 0, 96);
    private static final TextStyle LABEL =
            TextStyle.of("cortex-demo", 14, Color.WHITE).shadow();

    public Example() {
        super("Example", "Draws a panel", ModuleCategory.VISUAL);
    }

    @Listen
    public void onRender(RenderOverlayEvent event) {
        Cortex.blur(PANEL, ROUND, 14f, TINT);
        Cortex.outlineRoundedRect(PANEL, ROUND, 1f, new Color(255, 255, 255, 64));
        Cortex.text(20, 18, "Hello", LABEL);
    }

    @Listen
    public void onRenderWorld(RenderDimensionEvent event) {
        // One depth() up front, not one per entity — each change costs a draw call.
        Cortex.world().depth(DepthMode.THROUGH_WALLS).lineWidth(1.5f);

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            Box box = Box.ofEntity(player, event.getPartialTicks()).inflate(0.05);
            Cortex.world().box(box, FILL, OUTLINE);
        }
    }
}
```

---

## Failure modes

| Symptom | Cause | Fix |
|---|---|---|
| Effect never appears | GLSL compile error, usually an undeclared uniform | Declare it in the body; check log for `effect '<name>' failed to compile` |
| Faint rectangle around a glow | Falloff not reaching zero at the quad edge | Falloff must be rebased to hit 0 at `glowRadius` (see `shape.frag`) |
| Blurred panel shows the wrong part of the screen | Blur UV vertical orientation flipped | `vTop > vBottom`; framebuffer rows count upward, GUI coords downward |
| Nothing draws at all | Drawing outside the pass's own event, or engine disabled after an error | Check log for `Cortex: disabled after failing to …` / `world pass disabled after failing to …` |
| 3D geometry in the wrong place, or drifting far from spawn | `viewerPos` pre-subtracted by the caller | Pass absolute world coordinates; the engine subtracts the camera itself, in doubles |
| ESP box lags behind a moving player | `getEntityBoundingBox()` used raw | `Box.ofEntity(entity, partialTicks)` — the model is drawn interpolated, the box must be too |
| 3D outlines vanish behind the fill | Fill and outline in different batches, fill submitted last | Same batch submits triangles before lines; a `depth()` / `lineWidth()` call between them splits it |
| JVM aborts with "No context is current" | GL call from a non-render thread | Registration must not touch GL; see `GlContext.requireCurrent` |
| Depth/blend wrong in the game *after* the HUD | GL state not restored | `GlStateSnapshot` must capture and restore; never "set defaults" |
| Text renders inverted or as blocks | MSDF sign convention or mipmapped atlas | Positive inside; atlas must be uploaded with `mipmap = false` |

---

## Invariants — preserve when editing the engine

These were each a real bug. Do not regress them.

1. **Zero allocation per frame.** No `new`, boxing, or enhanced-for (allocates an iterator) on any
   draw path. `RenderTargetPool` uses a linear-scanned list precisely to avoid boxing a `Long` key.
2. **GL state is captured and restored, never reset to defaults.** `GlStateManager` caches state and
   short-circuits redundant calls, so a "repair" call that agrees with its stale cache does nothing.
   After restoring, the texture and alpha-test caches are pushed back through `GlStateManager`
   because Minecraft's font renderer may have changed them mid-frame.
3. **Registration never touches GL.** Modules are enabled while configs load on a background
   thread. `ShaderProgram.fromSource` and `Image.upload` are guarded by `GlContext.requireCurrent`.
4. **The MSDF atlas is never mipmapped.** Averaging texels between levels destroys the distance
   field. Content images *are* mipmapped, and need alpha bleed to avoid dark halos.
5. **Effects read a captured copy, never the framebuffer they draw into.** Sampling a texture
   attached to the bound draw framebuffer is undefined.
6. **Effect passes run with blending disabled.** Each pass replaces its whole target, which comes
   from a pool and still holds stale pixels.
7. **The shape shader takes `fwidth(d)`, never `fwidth` of the outline-folded distance.** `abs()`
   reverses the gradient across a stroke and spikes the filter width.
8. **Vertex attribute locations must match the batchers.** `ShapeBatcher` binds 0–6,
   `QuadBatcher` binds 0–4, `WorldBatcher` binds 0–2.
9. **The blur chain stops at half resolution.** A final full-res pass is the most expensive in the
   chain and adds nothing visible. Consumers sample by normalised coordinate.
10. **World geometry is camera-relative, subtracted in doubles.** A float world coordinate at the
    far edge of a map has metre-scale error; boxes visibly wobble as the player walks. The
    subtraction happens on the CPU, in `double`, before anything becomes a `float`.
11. **The world pass reads Minecraft's own matrices back out of GL.** `GL_MODELVIEW_MATRIX` times
    `GL_PROJECTION_MATRIX`, never a camera reconstructed from player yaw/pitch — a private copy
    drifts out of agreement with the game the moment anything touches the camera.
12. **The two passes fail independently.** `failed` disables the overlay, `worldFailed` the world.
    A driver that cannot compile `world.frag` must not take the HUD down with it.

---

## Verifying changes

```bash
./gradlew.bat compileJava
```

Compilation alone is weak here — most of this engine fails at runtime, not compile time. For
anything touching shaders, fonts, images, or allocation, verify against a **real GL context** in a
throwaway harness (headless GLFW window + `GL.createCapabilities()`), driven by a Gradle init
script so `build.gradle` stays untouched. Worth checking:

- every `.vert`/`.frag` pair compiles and links, including the runtime-assembled effect wrapper
- attribute locations match the batchers
- a font bakes and its glyphs decode correctly (dump the atlas and a rendered string to PNG)
- a GIF decodes to the right frame count and delays (dump a contact sheet)
- draw paths allocate zero bytes (`ThreadMXBean.getThreadAllocatedBytes`, with a control that
  deliberately allocates to prove the instrument is sensitive)

Never claim visual correctness from a compile. State plainly what was and was not verified.

### Verifying the world pass in-game

Enable the **`CortexWorldTest`** module (Client category). It draws `WorldDebugScene` — one of
every 3D primitive, anchored where the module was switched on — plus a HUD legend naming each
colour. Walk around the anchor; a camera basis that is subtly wrong looks correct head-on.

The legend's first two lines are the diagnostic, and they separate two failures that look
identical on screen:

| Line | Reading | Means |
|---|---|---|
| `listener 0 frames` (red) | The module's `RenderDimensionEvent` handler never ran | Not subscribed, or the event is not firing — nothing to do with the engine |
| `engine FAILED …` (red) | The pass switched itself off | The message is the root cause, usually a GLSL compile or a missing resource |
| `engine pass never opened` | `beginWorld()` is not being reached | The `EntityRenderer` bracket is gone or unreachable |
| `engine 0 calls, 0 verts` | Pass ran, no geometry arrived | Draws are happening outside the bracket |
| `engine N calls, M verts` | Geometry submitted | The pipeline works; anything invisible now is matrix, state or colour |

Then check in this order, because a failure early on makes everything after it meaningless:

1. **Axis marker** — red +X, green +Y, blue +Z. Green must point at the sky. If the axes are
   swapped or mirrored, `u_ViewProj` is wrong (`projection × modelview`, in that order) and
   nothing else in the row is worth reading.
2. **Boxes** sit flush on block boundaries and do not swim as you walk. Swimming means the
   camera subtraction lost precision — it must happen in `double`, before the cast to `float`.
3. **Billboards** face you from every angle, including from directly above. A billboard that
   shears as you circle it means the camera basis came from the wrong matrix rows.
4. **Depth pair** (green, two boxes) — put terrain between yourself and them; the left
   through-walls box survives, the right occluded one does not.
5. **Line widths** 1/2/4/8 are visibly different.
6. **Nothing is left broken afterwards** — turn the module off and confirm the world, hand and
   HUD still render normally. State left behind by the pass shows up here.
