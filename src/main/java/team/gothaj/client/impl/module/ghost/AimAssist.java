package team.gothaj.client.impl.module.ghost;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.eventbus.Listen;

public class AimAssist extends Module {

    private final NumberProperty range = new NumberProperty("Range", 5, 3, 10, 1);

    private final NumberProperty speed = new NumberProperty("Speed", 5, 1, 10, 1);

    private final BooleanProperty weaponOnly = new BooleanProperty("Weapon only", false);

    private final BooleanProperty teams = new BooleanProperty("Teams", false);

    private final BooleanProperty vertical = new BooleanProperty("Aim Vertically", false);

    private final BooleanProperty gcd = new BooleanProperty("GCD", false);

    public AimAssist() {
        super("AimAssist", "Smoothly assists your aim toward nearby players", ModuleCategory.GHOST);
        registerProperties(range, speed, weaponOnly, teams, vertical, gcd);
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null || !mc.inGameHasFocus) return;

        if (weaponOnly.getProperty() && !holdingSword()) return;

        Entity target = getClosestTarget(range.getProperty());
        if (target != null && Mouse.isButtonDown(0)) {
            faceEntitySmooth(target, speed.getProperty().intValue());
        }
    }

    private Entity getClosestTarget(double range) {
        Entity closest = null;
        double closestDistance = range;

        for (Object object : mc.theWorld.playerEntities) {
            if (!(object instanceof EntityPlayer player)) continue;
            if (player == mc.thePlayer || player.isInvisible()) continue;
            if (teams.getProperty() && isTeammate(player)) continue;

            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private void faceEntitySmooth(Entity entity, int speed) {
        double diffX = entity.posX - mc.thePlayer.posX;
        double diffZ = entity.posZ - mc.thePlayer.posZ;
        double diffY = (entity.posY + entity.getEyeHeight()) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());

        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));

        float yaw = updateRotation(mc.thePlayer.rotationYaw, targetYaw, (float) (speed - Math.random() * 0.5));
        float pitch = updateRotation(mc.thePlayer.rotationPitch, targetPitch, (float) (speed / 2 - Math.random() * 0.5));

        if (gcd.getProperty()) {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float g = f * f * f * 1.2F;
            float yawDelta = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
            float pitchDelta = pitch - mc.thePlayer.rotationPitch;
            yaw = mc.thePlayer.rotationYaw + (yawDelta - (yawDelta % g));
            pitch = mc.thePlayer.rotationPitch + (pitchDelta - (pitchDelta % g));
        }

        mc.thePlayer.rotationYaw = yaw;

        if (vertical.getProperty()) {
            mc.thePlayer.rotationPitch = pitch;
        }
    }

    private float updateRotation(float current, float target, float speed) {
        float delta = MathHelper.wrapAngleTo180_float(target - current);
        if (delta > speed) delta = speed;
        if (delta < -speed) delta = -speed;
        return current + delta;
    }

    private boolean holdingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private boolean isTeammate(EntityPlayer player) {
        Scoreboard board = mc.theWorld.getScoreboard();
        ScorePlayerTeam own = board.getPlayersTeam(mc.thePlayer.getName());
        ScorePlayerTeam other = board.getPlayersTeam(player.getName());
        return own != null && other != null && own == other;
    }
}
