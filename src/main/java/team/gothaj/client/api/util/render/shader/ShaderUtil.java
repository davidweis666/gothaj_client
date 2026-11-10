package team.gothaj.client.api.util.render.shader;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.other.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtil implements Utils {

    private final int shaderProgram;

    public ShaderUtil(String fragmentShaderLoc, String vertexShaderLoc) {
        int program = glCreateProgram();
        try {

            int fragmentShaderID = switch (fragmentShaderLoc) {
                case "bgGlass" -> createShader(new ByteArrayInputStream(bgGlass.getBytes()), GL_FRAGMENT_SHADER);
                case "bgFlow" -> createShader(new ByteArrayInputStream(bgFlow.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseDownGlow" -> createShader(new ByteArrayInputStream(kawaseDownGlow.getBytes()), GL_FRAGMENT_SHADER);
                case "bloom" -> createShader(new ByteArrayInputStream(bloom.getBytes()), GL_FRAGMENT_SHADER);
                case "blur" -> createShader(new ByteArrayInputStream(gaussianBlur.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseUpGlow" -> createShader(new ByteArrayInputStream(kawaseUpGlow.getBytes()), GL_FRAGMENT_SHADER);
                case "chams" -> createShader(new ByteArrayInputStream(chams.getBytes()), GL_FRAGMENT_SHADER);
                case "outline" -> createShader(new ByteArrayInputStream(outline.getBytes()), GL_FRAGMENT_SHADER);
                case "glow" -> createShader(new ByteArrayInputStream(glowShader.getBytes()), GL_FRAGMENT_SHADER);
                case "roundRectOutline" -> createShader(new ByteArrayInputStream(roundRectOutline.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseUpBloom" -> createShader(new ByteArrayInputStream(kawaseUpBloom.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseDownBloom" -> createShader(new ByteArrayInputStream(kawaseDownBloom.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseUp" -> createShader(new ByteArrayInputStream(kawaseUp.getBytes()), GL_FRAGMENT_SHADER);
                case "kawaseDown" -> createShader(new ByteArrayInputStream(kawaseDown.getBytes()), GL_FRAGMENT_SHADER);
                case "gradientFont" -> createShader(new ByteArrayInputStream(gradientFont.getBytes()), GL_FRAGMENT_SHADER);
                case "gradient" -> createShader(new ByteArrayInputStream(gradient.getBytes()), GL_FRAGMENT_SHADER);
                case "roundedRect" -> createShader(new ByteArrayInputStream(roundedRect.getBytes()), GL_FRAGMENT_SHADER);
                case "roundedRectOptional" -> createShader(new ByteArrayInputStream(roundedRectOptional.getBytes()), GL_FRAGMENT_SHADER);
                case "roundRectGradient" -> createShader(new ByteArrayInputStream(roundedRectGradient.getBytes()), GL_FRAGMENT_SHADER);
                case "roundRectGradientOptional" -> createShader(new ByteArrayInputStream(roundedRectGradientOptional.getBytes()), GL_FRAGMENT_SHADER);
                case "roundedRectTexture" -> createShader(new ByteArrayInputStream(roundedRectTexture.getBytes()), GL_FRAGMENT_SHADER);
                case "gradientMask" -> createShader(new ByteArrayInputStream(gradientMask.getBytes()), GL_FRAGMENT_SHADER);
                default -> createShader(mc.getResourceManager().getResource(new ResourceLocation("gothaj", fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
            };

            glAttachShader(program, fragmentShaderID);

            int vertexShaderID = createShader(
                    mc.getResourceManager()
                            .getResource(new ResourceLocation("gothaj", vertexShaderLoc))
                            .getInputStream(),
                    GL_VERTEX_SHADER
            );
            glAttachShader(program, vertexShaderID);

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == 0) {
            throw new IllegalStateException("Shader failed to link! " + fragmentShaderLoc);
        }
        this.shaderProgram = program;
    }

    public ShaderUtil(String fragmentShaderLoc) {
        this(fragmentShaderLoc, "render/shader/vertex.vert");
    }

    public void init() {
        glUseProgram(shaderProgram);
    }

    public void unload() {
        glUseProgram(0);
    }

    public int getUniform(String name) {
        return glGetUniformLocation(shaderProgram, name);
    }

    public void setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(shaderProgram, name);
        switch (args.length) {
            case 1:
                glUniform1f(loc, args[0]);
                break;
            case 2:
                glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(shaderProgram, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    public static void drawQuads(float width, float height) {
        drawQuads(0.0f, 0.0f, width, height);
    }

    public static void drawFixedQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        drawQuads((float) (mc.displayWidth / sr.getScaleFactor()), (float) mc.displayHeight / sr.getScaleFactor());
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, FileUtil.readInputStream(inputStream));
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.out.println(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }

        return shader;
    }

    private final String bgFlow = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            
            uniform vec2 resolution;
            uniform float time;
            uniform vec2 mouse;
            uniform vec3 color1;
            uniform vec3 color2;
            uniform float alpha;
            
            const int   complexity      = 30;
            const float fluid_speed     = 3.0;
            const float color_intensity = 0.22;
            
            void main()
            {
                vec2 p = (2.0 * gl_FragCoord.xy - resolution) / max(resolution.x, resolution.y);
            
                for(int i = 1; i < complexity; i++) {
                    vec2 newp = p;
                    newp.x += 0.5 / float(i) * sin(float(i) * p.y + time / fluid_speed + 0.9 * float(i)) + 100.0;
                    newp.y += 0.5 / float(i) * sin(float(i) * p.x + time / fluid_speed + 0.5 * float(i + 10)) - 100.0;
                    p = newp;
                }
            
                float t = 0.5 + 0.5 * sin(p.x + p.y + time * 0.25);
                vec3 col = mix(color1, color2, t);
            
                gl_FragColor = vec4(col, alpha);
            }
            """;

    private final String bgGlass =
            """
                    #version 120
                    
                    uniform float time;
                    uniform vec2 mouse;
                    uniform vec2 resolution;
                    uniform float alpha;
                    uniform vec3 color1;
                    
                    #define iResolution resolution
                    #define fragCoord gl_FragCoord
                    #define fragColor gl_FragColor
                    #define mainImage main
                    #define iTime time
                    
                    #define power 1.0
                    #define zoomOut 3.0
                    #define rot 1.0
                    #define iter 10.0
                    #define huePower 5.0
                    #define glow 0.5
                    #define distortScale 0.5
                    #define distortPower 1.0
                    #define Speed 0.02
                    #define WaveSpeed 0.2
                    #define Brightness 0.3
                    
                    void mainImage( void )
                    {
                        vec2 uv = ( fragCoord.xy / iResolution.xy ) + mouse / 10.0;
                        vec2 XYScale = vec2(1.,1.);
                        vec2 XYMove = vec2(0.0,0.0);
                    
                        uv *= zoomOut;
                        uv.xy = uv.xy * XYScale;
                        uv.xy = uv.xy + XYMove;
                        vec3 finalCol = vec3(0,0,0);
                        float halfDistort = distortScale / 0.5;
                        float distortsc2 = distortScale / distortScale + halfDistort;
                    
                        for(float i = 1.0; i < iter; i++){
                            uv.x += distortPower / i * sin(i * distortScale * uv.y - iTime * Speed);
                            uv.y += distortPower / i * sin(i * distortsc2 * uv.x + iTime * Speed);
                        }
                        vec3 col = vec3(vec3(glow,glow,glow)/sin(iTime*WaveSpeed-length(uv.yx) - uv.y));
                        finalCol = vec3(col*col);
                        
                        vec3 Color = color1 * Brightness;
                    
                        fragColor = vec4(finalCol.rgb * Color, alpha) * power;
                    }""";



    /**
     * Source: Tenacity 5.0 Shaders
     */

    private final String kawaseDownGlow = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform vec2 offset, halfpixel, iResolution;
            uniform vec4 color;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, gl_TexCoord[0].st);
                sum.rgb *= sum.a;
                sum *= 4.0;
                vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);
                smp1.rgb *= smp1.a;
                sum += smp1;
                vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);
                smp2.rgb *= smp2.a;
                sum += smp2;
                vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                smp3.rgb *= smp3.a;
                sum += smp3;
                vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
                smp4.rgb *= smp4.a;
                sum += smp4;
                vec4 result = sum / 12.0;
                gl_FragColor = vec4(color.rgb / result.a, result.a);
            }""";
    private final String kawaseUpGlow = """
                 #version 120
            
            uniform sampler2D inTexture, textureToCheck;
               uniform vec2 halfpixel, offset, iResolution;
               uniform bool check;
               uniform float lastPass;
               uniform float exposure;
            
               void main() {
                   if(check && texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;
                   vec2 uv = vec2(gl_FragCoord.xy / iResolution);
            
                   vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
                   sum.rgb *= sum.a;
                   vec4 smpl1 =  texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset);
                   smpl1.rgb *= smpl1.a;
                   sum += smpl1 * 2.0;
                   vec4 smp2 = texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
                   smp2.rgb *= smp2.a;
                   sum += smp2;
                   vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset);
                   smp3.rgb *= smp3.a;
                   sum += smp3 * 2.0;
                   vec4 smp4 = texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
                   smp4.rgb *= smp4.a;
                   sum += smp4;
                   vec4 smp5 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                   smp5.rgb *= smp5.a;
                   sum += smp5 * 2.0;
                   vec4 smp6 = texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
                   smp6.rgb *= smp6.a;
                   sum += smp6;
                   vec4 smp7 = texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset);
                   smp7.rgb *= smp7.a;
                   sum += smp7 * 2.0;
                   vec4 result = sum / 12.0;
                   gl_FragColor = vec4(result.rgb / result.a, mix(result.a, 1.0 - exp(-result.a * exposure), step(0.0, lastPass)));
               }
            """;
    private final String glowShader = """
            #version 120
            
            uniform sampler2D textureIn, textureToCheck;
            uniform vec2 texelSize, direction;
            uniform vec3 color;
            uniform bool avoidTexture;
            uniform float exposure, radius;
            uniform float weights[256];
            
            #define offset direction * texelSize
            
            void main() {
                if (direction.y == 1 && avoidTexture) {
                    if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;
                }
                vec4 innerColor = texture2D(textureIn, gl_TexCoord[0].st);
                innerColor.rgb *= innerColor.a;
                innerColor *= weights[0];
                for (float r = 1.0; r <= radius; r++) {
                    vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);
                    vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);
            
                    colorCurrent1.rgb *= colorCurrent1.a;
                    colorCurrent2.rgb *= colorCurrent2.a;
            
                    innerColor += (colorCurrent1 + colorCurrent2) * weights[int(r)];
                }
            
                gl_FragColor = vec4(innerColor.rgb / innerColor.a, mix(innerColor.a, 1.0 - exp(-innerColor.a * exposure), step(0.0, direction.y)));
            }
            """;
    private final String roundRectOutline = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color, outlineColor;
            uniform float radius, outlineThickness;
            
            float roundedSDF(vec2 centerPos, vec2 size, float radius) {
                return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
            }
            
            void main() {
                float distance = roundedSDF(gl_FragCoord.xy - location - (rectSize * .5), (rectSize * .5) + (outlineThickness *.5) - 1.0, radius);
            
                float blendAmount = smoothstep(0., 2., abs(distance) - (outlineThickness * .5));
            
                vec4 insideColor = (distance < 0.) ? color : vec4(outlineColor.rgb,  0.0);
                gl_FragColor = mix(outlineColor, insideColor, blendAmount);
            
            }
            """;
    private final String kawaseUpBloom = """
            #version 120
            
            uniform sampler2D inTexture, textureToCheck;
            uniform vec2 halfpixel, offset, iResolution;
            uniform int check;
            
            void main() {
              //  if(check && texture2D(textureToCheck, gl_TexCoord[0].st).a > 0.0) discard;
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
            
                vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
                sum.rgb *= sum.a;
                vec4 smpl1 =  texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset);
                smpl1.rgb *= smpl1.a;
                sum += smpl1 * 2.0;
                vec4 smp2 = texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
                smp2.rgb *= smp2.a;
                sum += smp2;
                vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset);
                smp3.rgb *= smp3.a;
                sum += smp3 * 2.0;
                vec4 smp4 = texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
                smp4.rgb *= smp4.a;
                sum += smp4;
                vec4 smp5 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                smp5.rgb *= smp5.a;
                sum += smp5 * 2.0;
                vec4 smp6 = texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
                smp6.rgb *= smp6.a;
                sum += smp6;
                vec4 smp7 = texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset);
                smp7.rgb *= smp7.a;
                sum += smp7 * 2.0;
                vec4 result = sum / 12.0;
                gl_FragColor = vec4(result.rgb / result.a, mix(result.a, result.a * (1.0 - texture2D(textureToCheck, gl_TexCoord[0].st).a),check));
            }
            """;
    private final String kawaseDownBloom = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform vec2 offset, halfpixel, iResolution;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, gl_TexCoord[0].st);
                sum.rgb *= sum.a;
                sum *= 4.0;
                vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);
                smp1.rgb *= smp1.a;
                sum += smp1;
                vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);
                smp2.rgb *= smp2.a;
                sum += smp2;
                vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                smp3.rgb *= smp3.a;
                sum += smp3;
                vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
                smp4.rgb *= smp4.a;
                sum += smp4;
                vec4 result = sum / 8.0;
                gl_FragColor = vec4(result.rgb / result.a, result.a);
            }
            """;
    private final String kawaseUp = """
            #version 120
            
            uniform sampler2D inTexture, textureToCheck;
            uniform vec2 halfpixel, offset, iResolution;
            uniform int check;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
                sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
                sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
                sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;
            
                gl_FragColor = vec4(sum.rgb /12.0, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, check));
            }
            """;
    private final String kawaseDown = """
            #version 120
            
            uniform sampler2D inTexture;
            uniform vec2 offset, halfpixel, iResolution;
            
            void main() {
                vec2 uv = vec2(gl_FragCoord.xy / iResolution);
                vec4 sum = texture2D(inTexture, gl_TexCoord[0].st) * 4.0;
                sum += texture2D(inTexture, uv - halfpixel.xy * offset);
                sum += texture2D(inTexture, uv + halfpixel.xy * offset);
                sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
                sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);
                gl_FragColor = vec4(sum.rgb * .125, 1.0);
            }
            """;

    private final String gradientFont = """
            // #version 120
              uniform sampler2D font;          // glyph atlas (alpha in A or R)
              uniform vec2 location;           // text rect top-left in screen pixels
              uniform vec2 rectSize;           // text rect size in pixels (width, height)
              uniform vec4 leftColor;          // gradient start (RGBA)
              uniform vec4 rightColor;         // gradient end (RGBA)
              uniform float alphaCutoff;       // e.g. 0.01 to discard near-transparent texels
            
              void main() {
                  float a = texture2D(font, gl_TexCoord[0].st).a;     // sample glyph alpha
                  if (a <= alphaCutoff) discard;                      // mask strictly to text
            
                  float t = clamp((gl_FragCoord.x - location.x) / rectSize.x, 0.0, 1.0);
                  vec4 grad = mix(leftColor, rightColor, t);
            
                  // Output premultiplied by glyph coverage to confine the gradient to glyphs
                  gl_FragColor = vec4(grad.rgb, grad.a) * a;
              }
            
            """;

    private final String gradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform sampler2D tex;
            uniform vec4 color1, color2, color3, color4;
            #define NOISE .5/255.0
            
            vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
                vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
                //Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 coords = (gl_FragCoord.xy - location) / rectSize;
                gl_FragColor = createGradient(coords, color1, color2, color3, color4);
            }
            """;
    private final String roundedRectGradient = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color1, color2, color3, color4;
            uniform float radius;
            
            #define NOISE .5/255.0
            
            float roundSDF(vec2 p, vec2 b, float r) {
                return length(max(abs(p) - b , 0.0)) - r;
            }
            
            vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
                vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
                //Dithering the color
                // from https://shader-tutorial.dev/advanced/color-banding-dithering/
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 st = gl_TexCoord[0].st;
                vec2 halfSize = rectSize * .5;
                // use the bottom leftColor as the alpha
                float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - radius - 1., radius)));
                vec4 gradient = createGradient(st, color1, color2, color3, color4);
                gl_FragColor = vec4(gradient.rgb, gradient.a * smoothedAlpha);
            }
            """;

    private final String roundedRectGradientOptional = """
            #version 120
            
               uniform vec2 location, rectSize;
               uniform vec4 color1, color2, color3, color4;
               uniform vec4 radius; // x: top-left, y: top-right, z: bottom-left, w: bottom-right
            
               #define NOISE .5/255.0
            
               float roundedBoxSDF(vec2 centerPos, vec2 size, vec4 cornerRadius) {
                   vec2 q = abs(centerPos);
                   float r;
            
                   if (centerPos.x > 0.0) {
                       if (centerPos.y > 0.0) {
                           r = cornerRadius.w; // bottom-right
                       } else {
                           r = cornerRadius.y; // top-right
                       }
                   } else {
                       if (centerPos.y > 0.0) {
                           r = cornerRadius.z; // bottom-left
                       } else {
                           r = cornerRadius.x; // top-left
                       }
                   }
            
                   return length(max(q - size + r, 0.0)) - r;
               }
            
               vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
                   vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
                   color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                   return color;
               }
            
               void main() {
                   vec2 st = gl_TexCoord[0].st;
                   vec2 halfSize = rectSize * .5;
                   vec2 fragPos = st * rectSize;
            
                   vec2 centerPos = halfSize - fragPos;
            
                   float dist = roundedBoxSDF(centerPos, halfSize - 1.0, radius);
            
                   float smoothedAlpha = (1.0 - smoothstep(0.0, 2.0, dist));
                   vec4 gradient = createGradient(st, color1, color2, color3, color4);
            
                   // Only multiply smoothedAlpha, don't multiply gradient.a again
                   gl_FragColor = vec4(gradient.rgb, smoothedAlpha * gradient.a);
               }
            
        """;



    private String roundedRectTexture = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform sampler2D textureIn;\n" +
            "uniform float radius, alpha;\n" +
            "\n" +
            "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
            "    return length(max(abs(centerPos) -size, 0.)) - radius;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    float distance = roundedBoxSDF((rectSize * .5) - (gl_TexCoord[0].st * rectSize), (rectSize * .5) - radius - 1., radius);\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 2.0, distance)) * alpha;\n" +
            "    gl_FragColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, smoothedAlpha);\n" +
            "}";

    public static String gradientMask = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform sampler2D tex;
            uniform vec3 color1, color2, color3, color4;
            uniform float alpha;
            
            #define NOISE .5/255.0
            
            vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){
                vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);
                color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453));
                return color;
            }
            
            void main() {
                vec2 coords = (gl_FragCoord.xy - location) / rectSize;
                float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;
                gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4), texColorAlpha * alpha);
            }""";



    private final String roundedRect = """
            #version 120
            
            uniform vec2 location, rectSize;
            uniform vec4 color;
            uniform float radius;
            uniform bool blur;
            
            float roundSDF(vec2 p, vec2 b, float r) {
                return length(max(abs(p) - b, 0.0)) - r;
            }
            
            void main() {
                vec2 rectHalf = rectSize * 0.5;
                vec2 fragPos = gl_TexCoord[0].st * rectSize;
                float dist = roundSDF(rectHalf - fragPos, rectHalf - radius - 1.0, radius);
            
                float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, dist)) * color.a;
            
                if (smoothedAlpha < 0.01) discard;
            
                gl_FragColor = vec4(color.rgb, smoothedAlpha);
            }
            """;

    private final String roundedRectOptional = """
        #version 120
        
        uniform vec2 location, rectSize;
        uniform vec4 color;
        uniform vec4 radius; // x: top-left, y: top-right, z: bottom-left, w: bottom-right
        uniform bool blur;
        
        float roundSDF(vec2 p, vec2 b, float r) {
            return length(max(abs(p) - b, 0.0)) - r;
        }
        
        float roundedBoxSDF(vec2 centerPos, vec2 size, vec4 cornerRadius) {
            // Select the appropriate corner radius
            vec2 q = abs(centerPos);
            float r;
            
            // Determine which quadrant we're in and use the corresponding radius
            if (centerPos.x > 0.0) {
                if (centerPos.y > 0.0) {
                    r = cornerRadius.w; // bottom-right
                } else {
                    r = cornerRadius.y; // top-right
                }
            } else {
                if (centerPos.y > 0.0) {
                    r = cornerRadius.z; // bottom-left
                } else {
                    r = cornerRadius.x; // top-left
                }
            }
            
            return length(max(q - size + r, 0.0)) - r;
        }
        
        void main() {
            vec2 rectHalf = rectSize * 0.5;
            vec2 fragPos = gl_TexCoord[0].st * rectSize;
            
            // Center the position
            vec2 centerPos = rectHalf - fragPos;
            
            float dist = roundedBoxSDF(centerPos, rectHalf - 1.0, radius);
        
            float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, dist)) * color.a;
        
            if (smoothedAlpha < 0.01) discard;
        
            gl_FragColor = vec4(color.rgb, smoothedAlpha);
        }
        """;


    private final String gaussianBlur = """
            #version 120
            
            uniform sampler2D textureIn;
            uniform vec2 texelSize;
            uniform vec2 direction;
            uniform float radius;
            uniform float weights[128];
            
            #define offset (texelSize * direction)
            
            void main() {
                vec2 uv = gl_TexCoord[0].st;
                vec3 color = texture2D(textureIn, uv).rgb * weights[0];
            
                for (int i = 1; i < 128; ++i) {
                    if (i > int(radius)) break;
            
                    vec2 delta = float(i) * offset;
                    color += texture2D(textureIn, uv + delta).rgb * weights[i];
                    color += texture2D(textureIn, uv - delta).rgb * weights[i];
                }
            
                gl_FragColor = vec4(color, 1.0);
            }
            """;

    private final String bloom = """
             #version 120
    
                 uniform sampler2D inTexture;
                 uniform sampler2D textureToCheck;
                 uniform vec2 texelSize;
                 uniform vec2 direction;
                 uniform float radius;
                 uniform float weights[256];
                 uniform vec3 color;
    
                 void main() {
                     vec2 uv = gl_TexCoord[0].st;
    
                     if (direction.y > 0.0 && texture2D(textureToCheck, uv).a != 0.0) {
                         discard;
                     }
    
                     float alpha = texture2D(inTexture, uv).a * weights[0];
                     float weightSum = weights[0];
    
                     for (int i = 1; i <= int(radius); ++i) {
                         vec2 offset = texelSize * direction * float(i);
                         float w = weights[i];
    
                         alpha += texture2D(inTexture, clamp(uv + offset, vec2(0.0), vec2(1.0))).a * w;
                         alpha += texture2D(inTexture, clamp(uv - offset, vec2(0.0), vec2(1.0))).a * w;
                         weightSum += 2.0 * w;
                     }
    
                     alpha = (alpha / weightSum) * 0.8;
                     gl_FragColor = vec4(color, alpha);
                 }
    """;


    private final String chams =
            """
                    #version 120
                    
                    uniform sampler2D textureIn;
                    uniform vec4 color;
                    void main() {
                        float alpha = texture2D(textureIn, gl_TexCoord[0].st).a;
                        gl_FragColor = vec4(color.rgb, color.a * mix(0.0, alpha, step(0.0, alpha)));
                    }
                    """;

    private final String outline = """
            #version 120
            
               uniform vec2 texelSize, direction;
               uniform sampler2D textureIn;
               uniform float radius;
            
               #define offset direction * texelSize
            
               void main() {
                   vec4 center = texture2D(textureIn, gl_TexCoord[0].st);
                   center.rgb *= center.a;
                   if (center.a > 0) discard;
                   for (float r = 1.0; r <= radius; r++) {
                       vec4 alphaCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);
                       vec4 alphaCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);
                       alphaCurrent1.rgb *= alphaCurrent1.a;
                       alphaCurrent2.rgb *= alphaCurrent2.a;
                       center += alphaCurrent1 + alphaCurrent2;
                   }
            
                   gl_FragColor = vec4(center.rgb / center.a, center.a);
            
               }
            
            """;

}