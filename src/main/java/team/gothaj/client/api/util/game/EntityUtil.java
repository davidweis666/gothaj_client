	package team.gothaj.client.api.util.game;

import lombok.val;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.api.util.player.SimulatedPlayer;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.module.client.Targets;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.combat.Velocity;
import team.gothaj.client.main.GothajClient;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static team.gothaj.client.api.util.Utils.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EntityUtil {

	public static int size = 0, switchIndex;

	public static TimerUtil timer = new TimerUtil();

	public static double getDistanceToEntityBoxFromPosition(double posX, double posY, double posZ, Entity entity) {
	    Vec3 eyes = new Vec3(posX, posY + mc.thePlayer.getEyeHeight(), posZ);
	    Vec3 pos = RotationUtil.getBestHitVec(entity, eyes);
	    double xDist = Math.abs(pos.xCoord - eyes.xCoord);
	    double yDist = Math.abs(pos.yCoord - eyes.yCoord);
	    double zDist = Math.abs(pos.zCoord - eyes.zCoord);
	    return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
	}

	/**
	 * Same as {@link #getDistanceToEntityBoxFromPosition} but shifts the entity hitbox by (offX, offY, offZ)
	 * (e.g. linear motion extrapolation for prediction).
	 */
	public static double getDistanceToEntityBoxFromPositionWithOffset(double posX, double posY, double posZ,
			Entity entity, double offX, double offY, double offZ) {
		Vec3 eyes = new Vec3(posX, posY + mc.thePlayer.getEyeHeight(), posZ);
		final float pad = 0.1f;
		AxisAlignedBB bb = entity.getEntityBoundingBox().expand(pad, pad, pad).offset(offX, offY, offZ);
		double ex = MathHelper.clamp(eyes.xCoord, bb.minX, bb.maxX);
		double ey = MathHelper.clamp(eyes.yCoord, bb.minY, bb.maxY);
		double ez = MathHelper.clamp(eyes.zCoord, bb.minZ, bb.maxZ);
		Vec3 pos = new Vec3(ex, ey - 0.4, ez);
		double xDist = Math.abs(pos.xCoord - eyes.xCoord);
		double yDist = Math.abs(pos.yCoord - eyes.yCoord);
		double zDist = Math.abs(pos.zCoord - eyes.zCoord);
		return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
	}
    
    public static double getDistanceToEntityPositionBoxFromPosition(double posX, double posY, double posZ, double posX2, double posY2, double posZ2, Entity entity) {
        Vec3 pos = RotationUtil.getBestHitVecPosition(entity, posX2, posY2, posZ2);
        Vec3 eyes = new Vec3(posX, posY + mc.thePlayer.getEyeHeight(), posZ);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
    }

	public static double getDistanceToEntityBox(Entity entity) {
		Vec3 eyes = Minecraft.getMinecraft().thePlayer.getPositionEyes(1f);
		Vec3 pos = RotationUtil.getBestHitVec(entity);
		double xDist = Math.abs(pos.xCoord - eyes.xCoord);
		double yDist = Math.abs(pos.yCoord - eyes.yCoord);
		double zDist = Math.abs(pos.zCoord - eyes.zCoord);
		return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
	}

	public static Vec3 getDirection(float yaw, float pitch) {
		var yawRad = Math.toRadians(yaw);
		var pitchRad = Math.toRadians(pitch);

		var x = -sin(yawRad) * cos(pitchRad);
		var y = -sin(pitchRad);
		var z = cos(yawRad) * cos(pitchRad);

		return new Vec3(x, y, z);
	}

	public static Double isInReach(Entity entity, double reach) {
		var eyePos = mc.thePlayer.getPositionEyes(1f);
		var bb = entity.getEntityBoundingBox().expand(
				entity.getCollisionBorderSize(), entity.getCollisionBorderSize(),
				entity.getCollisionBorderSize()
		);

		var dir = getDirection(RotationComponent.rotations[0], RotationComponent.rotations[1]);

		var end = eyePos.addVector(dir.xCoord * reach, dir.yCoord * reach, dir.zCoord * reach);

		var hit = bb.calculateIntercept(eyePos, end);
		if (hit == null)
			return null;

		return eyePos.distanceTo(hit.hitVec);
	}

    public static double getDistanceToEntityBox(Vec3 eyes, Entity entity) {
        Vec3 pos = RotationUtil.getBestHitVec(entity);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
    }
	
	public static double getDistanceToEntityPositionBox(Entity entity, double posX, double posY, double posZ) {
		Vec3 eyes = Minecraft.getMinecraft().thePlayer.getPositionEyes(1f);
		Vec3 pos = RotationUtil.getBestHitVecPosition(entity, posX, posY, posZ);
		double xDist = Math.abs(pos.xCoord - eyes.xCoord);
		double yDist = Math.abs(pos.yCoord - eyes.yCoord);
		double zDist = Math.abs(pos.zCoord - eyes.zCoord);
		return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
	}
	
	public static EntityLivingBase getTarget(float range) {
		KillAura ka = GothajClient.getInstance().getModuleManager().getAndCast(KillAura.class);
		
		EntityLivingBase target = null;
		ArrayList<EntityLivingBase> possibilities = new ArrayList<EntityLivingBase>();

        possibilities = mc.theWorld.loadedEntityList.stream()
				.filter(entity -> entity instanceof EntityLivingBase
						&& EntityUtil.getDistanceToEntityBox(entity) <= range)
				.map(entity -> (EntityLivingBase) entity).filter(EntityUtil::isValid)
				.sorted(Comparator.comparingDouble(entity -> switch (ka.sortMode.getProperty()) {
				case "Health" -> entity.getHealth();
				case "Fov" -> RotationUtil.yawDist(entity);
				case "HurtTime" -> entity.hurtTime;
				default -> EntityUtil.getDistanceToEntityBox(entity);
				})).collect(Collectors.toCollection(ArrayList::new));

		switch (ka.targetMode.getProperty()) {
		case "Priority" -> {
			target = possibilities.isEmpty() ? null : possibilities.getFirst();
		}
		case "Switch" -> {
			List<EntityLivingBase> switchList = possibilities.stream()
					.filter(e -> EntityUtil.getDistanceToEntityBox(e) <= range)
					.collect(Collectors.toCollection(ArrayList::new));

			if (!switchList.isEmpty()) {
				if (switchList.size() == 1) {
					target = switchList.getFirst();
				} else {
					if (switchIndex >= switchList.size()) {
						switchIndex = 0;
					}

                    target = switchList.get(switchIndex);

					if (ka.switchTimer.hasTimeReached(ka.switchDelay.getProperty().longValue())) {
						switchIndex = (switchIndex + 1) % switchList.size();
						ka.switchTimer.reset();

					}
				}
			} else {
                switchIndex = 0;
			}
		}
		}

		if (!isValid(target)) {
			return null;
		}
		return target;
	}

	public static boolean isTeammate(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityOtherPlayerMP) {
            String name = entityLivingBase.getDisplayName().getFormattedText();
            StringBuilder append = new StringBuilder().append("§");
            return name.startsWith(append.append(mc.thePlayer.getDisplayName().getFormattedText().charAt(1)).toString());
        }
        return false;
	}

	public static boolean isValid(EntityLivingBase entity) {
		KillAura ka = GothajClient.getInstance().getModuleManager().getAndCast(KillAura.class);
		
		if (entity == null || entity == mc.thePlayer || !entity.isEntityAlive() || entity.getHealth() == 0)
			return false;

		if (entity instanceof EntityArmorStand)
			return false;

		if (ka.checkWalls.getProperty() && !RayCastUtil.canEntityBeSeen(entity))
			return false;

		if (entity instanceof EntityPlayer pl && isBot(pl) && !Targets.combatTargets.getOptionState("Bots"))
			return false;

		if (entity instanceof EntityPlayer && !Targets.combatTargets.getOptionState("Players"))
			return false;


		if (entity instanceof IMob && !Targets.combatTargets.getOptionState("Monsters"))
			return false;

        Team entityTeam = entity.getTeam();
        Team playerTeam = mc.thePlayer.getTeam();
        boolean isTeammate = isTeammate(entity) || (entityTeam != null && playerTeam != null && entityTeam.isSameTeam(playerTeam));

        if (isTeammate && !Targets.combatTargets.getOptionState("Teammates")) {
            return false;
        }

		if (GothajClient.getInstance().getFriendManager().isFriend(entity.getName())) {
			return false;
		}

        if ((entity instanceof IAnimals || entity instanceof EntityAgeable)
                && !(entity instanceof IMob)
                && !Targets.combatTargets.getOptionState("Animals")) {
            return false;
        }


        return !entity.isInvisible() || Targets.combatTargets.getOptionState("Invisibles");
	}

	public static boolean isBot(EntityPlayer player) {
		if (Targets.botFilter.getOptionState("Tablist")) {
			return mc.getNetHandler().getPlayerInfoMap().stream()
					.noneMatch(info -> info.getGameProfile().getId().equals(player.getUniqueID()));
		}
		return false;
	}

    public static Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return getBlock(mc.thePlayer.posX + offsetX, mc.thePlayer.posY + offsetY, mc.thePlayer.posZ + offsetZ);
    }

    public static Block blockRelativeToPlayer(Vec3 target, final double offsetX, final double offsetY, final double offsetZ) {
        return getBlock(target.xCoord + offsetX, target.yCoord + offsetY, target.zCoord + offsetZ);
    }

    public static Block getBlockUnder(Vec3 target, int offset) {
        return blockRelativeToPlayer(target, 0, offset, 0);
    }

    public static Block getBlockUnder(int offset) {
        return blockRelativeToPlayer(0, offset, 0);
    }

    public static Block getBlock(final double x, final double y, final double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }
}
