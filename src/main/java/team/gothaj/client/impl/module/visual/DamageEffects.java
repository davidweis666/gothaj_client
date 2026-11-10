package team.gothaj.client.impl.module.visual;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.Timer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.FontProperty;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DamageEffects extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", "Damage", "Damage", "Superhero", "Custom");
    public final InputProperty custom = new InputProperty("Custom (split by space)").setVisible(() -> mode.is("Custom"));

    public final BooleanProperty customFont = new BooleanProperty("Custom Font", false);
    public final FontProperty font = new FontProperty("Font", Fonts.nunito18).setVisible(customFont::getProperty);

    private final List<Effects> hits = new ArrayList<>();
    private float lastHealth;
    private EntityLivingBase lastTarget = null;

    private final String[] SUPERHERO = {
            "POW!",
            "BOOM!",
            "HIT!",
            "SMASH!"
    };

    public DamageEffects() {
        super("DamageEffects", "", ModuleCategory.VISUAL);
        registerProperties(mode, custom, customFont, font);
    }

    @Listen
    public void onUpdate(MotionEvent e) {
        if (KillAura.target == null) {
            this.lastHealth = 20;
            lastTarget = null;
            return;
        }

        if (this.lastTarget == null || KillAura.target != this.lastTarget) {
            this.lastTarget = KillAura.target;
            this.lastHealth = KillAura.target.getHealth();
            return;
        }

        if (KillAura.target.getHealth() != this.lastHealth) {
            if (KillAura.target.getHealth() < this.lastHealth) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                this.hits.add(new Effects(
                        KillAura.target.getPosition().add(
                                random.nextDouble(0.5, 1.5),
                                random.nextDouble(1, 1.5),
                                random.nextDouble(0.5, 1.5)
                        ),
                        this.lastHealth - KillAura.target.getHealth()
                ));
            }
            this.lastHealth = KillAura.target.getHealth();
        }
    }

    @Listen
    public void onRender(RenderDimensionEvent event) {
        GlStateManager.pushMatrix();
        try {
            hits.removeIf(d -> {
                if (d.isFinished()) {
                    return true;
                }
                d.onRender();
                return false;
            });
        } catch (Exception ignored) {
        }
        GlStateManager.popMatrix();
    }

    class Effects {
        protected static final Minecraft mc = Minecraft.getMinecraft();
        private final long startTime = System.currentTimeMillis();
        private final BlockPos pos;
        private final double healthVal;
        private final String hitText;

        private static final long MAX_TIME = 1337;
        private static final double SCALE_FACTOR = 0.01666666753590107;

        public Effects(BlockPos pos, double healthVal) {
            this.pos = pos;
            this.healthVal = healthVal;

            this.hitText = switch (mode.getProperty()) {
                case "Superhero" -> SUPERHERO[MathUtil.getRandomNumber(0, SUPERHERO.length - 1)];
                case "Custom" -> getRandomCustomText();
                default -> new DecimalFormat("#.#").format(this.healthVal);
            };
        }

        public void onRender() {
            RenderManager renderManager = mc.getRenderManager();
            Timer timer = mc.timer;

            double x = this.pos.getX() - renderManager.viewerPosX;
            double y = this.pos.getY() - renderManager.viewerPosY;
            double z = this.pos.getZ() - renderManager.viewerPosZ;

            float viewMultiplier = (mc.gameSettings.thirdPersonView == 2) ? -1.0f : 1.0f;
            double size = 1.2;

            GL11.glPushMatrix();

            mc.entityRenderer.setupCameraTransform(timer.renderPartialTicks, 0);
            GL11.glTranslated(x, y, z);
            GL11.glNormal3f(0.0f, 1.0f, 0.0f);
            GL11.glRotatef(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(renderManager.playerViewX, viewMultiplier, 0.0f, 0.0f);
            GL11.glScaled(-SCALE_FACTOR * size, -SCALE_FACTOR * size, SCALE_FACTOR * size);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            long timeLeft = (this.startTime + MAX_TIME) - System.currentTimeMillis();
            float sizePercentage = 1.0f;
            float yPercentage = 0.0f;

            if (timeLeft < 75) {
                sizePercentage = Math.min((float) timeLeft / 75F, 1F);
                yPercentage = Math.min((float) timeLeft / 75F, 1F);
            } else {
                sizePercentage = Math.min((float) (System.currentTimeMillis() - this.startTime) / 300F, 1F);
                yPercentage = Math.min((float) (System.currentTimeMillis() - this.startTime) / 600F, 1F);
            }

            GlStateManager.scale(2 * sizePercentage, 2 * sizePercentage, 2 * sizePercentage);
            Gui.drawRect(-100, -100, 100, 100, new Color(255, 0, 0, 0).getRGB());

            Color render = new Color(0, 255, 0);
            if (this.healthVal < 3 && this.healthVal > 1) {
                render = new Color(255, 255, 0);
            } else if (this.healthVal <= 1) {
                render = new Color(255, 0, 0);
            }

            if (customFont.getProperty()) {
              //TODO  SkiaDraw.text(font.getProperty(), hitText, 0, -(yPercentage * 4), render.getRGB());
            } else {
                mc.fontRendererObj.drawStringWithShadow(hitText, 0, -(yPercentage * 4), render.getRGB());
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }

        private String getRandomCustomText() {
            String customInput = custom.getProperty();
            if (customInput == null || customInput.isBlank()) {
                return "HIT!";
            }
            String[] customTexts = customInput.split("\\s+");
            if (customTexts.length == 0) {
                return "HIT!";
            }
            int randomIndex = ThreadLocalRandom.current().nextInt(customTexts.length);
            return customTexts[randomIndex];
        }

        public boolean isFinished() {
            return System.currentTimeMillis() - this.startTime >= MAX_TIME;
        }
    }
}
