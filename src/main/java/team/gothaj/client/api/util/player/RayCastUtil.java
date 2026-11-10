package team.gothaj.client.api.util.player;

import com.google.common.base.Predicates;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.Utils;

import java.util.List;

/**
 * @author Kawase
 * @since 30.08.2025
 */
public class RayCastUtil implements Utils {
    // methods for block ray casting.
    public static boolean lookingAtBlock(BlockPos blockPos, float yaw, float pitch, EnumFacing enumFacing,
                                         boolean strict) {
        MovingObjectPosition movingObjectPosition = rayCast(new float[] { yaw, pitch },
                mc.playerController.getBlockReachDistance(), 2);

        if (movingObjectPosition == null)
            return false;
        if (movingObjectPosition.getBlockPos() == null)
            return false;

        if (strict) {
            return movingObjectPosition.getBlockPos().equalsBlockPos(blockPos)
                    && movingObjectPosition.sideHit == enumFacing;
        } else {
            return movingObjectPosition.getBlockPos().equalsBlockPos(blockPos);
        }
    }

    // methods for entity ray casting.

    public static boolean canHit(EntityLivingBase target) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mc.objectMouseOver.entityHit == target) {
            return true;
        }

        return false;
    }

    public static boolean canHitCustom(final float[] rotation, EntityLivingBase target, float range) {
        MovingObjectPosition result = rayCast(rotation, range, 0);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return result.entityHit == target;
        }

        return false;
    }

    public static boolean canHit(final float[] rotation, EntityLivingBase target, float range) {
        MovingObjectPosition result = mc.thePlayer.rayTraceCustom(range, rotation[0], rotation[1]);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return result.entityHit == target;
        }

        return false;
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = mc.thePlayer.getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }
    
    public static Entity rayTraceFromTarget(Entity target, double range, float[] rotations) {
		Vec3 vec3 = target.getPositionEyes(1.0f);

		Vec3 vec31 = target.getVectorForRotation(rotations[1], rotations[0]);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range);

		Entity pointedEntity = null;

		float f = 1.0F;
		List<?> list = Minecraft.getMinecraft().theWorld.getEntitiesInAABBexcluding(
				Minecraft.getMinecraft().getRenderViewEntity(),
				Minecraft.getMinecraft().getRenderViewEntity().getEntityBoundingBox()
						.addCoord(vec31.xCoord * range, vec31.yCoord * range, vec31.zCoord * range).expand(f, f, f),
				Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
		double d2 = range;

		for (Object o : list) {
			Entity entity1 = (Entity) o;
			float f1 = entity1.getCollisionBorderSize();
			AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
			MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

			if (axisalignedbb.isVecInside(vec3)) {
				if (d2 >= 0.0D) {
					pointedEntity = entity1;
					d2 = 0.0D;
				}
			} else if (movingobjectposition != null) {
				double d3 = vec3.distanceTo(movingobjectposition.hitVec);

				if (d3 < d2 || d2 == 0.0D) {
					if (entity1 == Minecraft.getMinecraft().getRenderViewEntity().ridingEntity) {
						if (d2 == 0.0D) {
							pointedEntity = entity1;
						}
					} else {
						pointedEntity = entity1;
						d2 = d3;
					}
				}
			}
		}

		return pointedEntity;
	}


    public static MovingObjectPosition rayCast(final float[] rots, final double range, final double hitBoxExpand) {
        MovingObjectPosition objectMouseOver = null;
        final Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;
            double d0 = range;
            objectMouseOver = entity.rayTraceCustom(d0, rots[0], rots[1]);
            double d2 = d0;
            final Vec3 vec3 = entity.getPositionEyes(1);
            boolean flag = false;
            if (mc.playerController.extendedReach()) {
                d0 = 6.0;
                d2 = 6.0;
            } else {
                if (d0 > 3.0) {
                    flag = true;
                }
            }
            if (objectMouseOver != null) {
                d2 = objectMouseOver.hitVec.distanceTo(vec3);
            }
            final Vec3 vec4 = entity.getLookCustom(rots[0], rots[1]);
            final Vec3 vec5 = vec3.addVector(vec4.xCoord * d0, vec4.yCoord * d0, vec4.zCoord * d0);
            Entity pointedEntity = null;
            Vec3 vec6 = null;
            final float f = 1.0f;
            final List list = mc.theWorld
                    .getEntitiesInAABBexcluding(
                            entity, entity.getEntityBoundingBox()
                                    .addCoord(vec4.xCoord * d0, vec4.yCoord * d0, vec4.zCoord * d0).expand(f, f, f),
                            Predicates.and(EntitySelectors.NOT_SPECTATING));
            double d3 = d2;
            final AxisAlignedBB realBB = null;
            for (int i = 0; i < list.size(); ++i) {
                final Entity entity2 = (Entity) list.get(i);
                final float f2 = (float) (entity2.getCollisionBorderSize() + hitBoxExpand);
                final AxisAlignedBB axisalignedbb = entity2.getEntityBoundingBox().expand(f2, f2, f2);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec5);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (d3 >= 0.0) {
                        pointedEntity = entity2;
                        vec6 = ((movingobjectposition == null) ? vec3 : movingobjectposition.hitVec);
                        d3 = 0.0;
                    }
                } else if (movingobjectposition != null) {
                    final double d4 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d4 < d3 || d3 == 0.0) {

                        if (entity2 == entity.ridingEntity) {
                            if (d3 == 0.0) {
                                pointedEntity = entity2;
                                vec6 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity2;
                            vec6 = movingobjectposition.hitVec;
                            d3 = d4;
                        }
                    }
                }
            }
            if (pointedEntity != null && flag && vec3.distanceTo(vec6) > range) {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec6, null,
                        new BlockPos(vec6));
            }
            if (pointedEntity != null && (d3 < d2 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec6);
            }
        }
        return objectMouseOver;
    }

    public static Entity rayTrace(double range, float[] rotations) {
		if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
			return mc.objectMouseOver.entityHit;
		}

		MovingObjectPosition objectMouseOver = null;
        final Entity entity = mc.getRenderViewEntity();
        
        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;
            double d0 = range;
            objectMouseOver = entity.rayTraceCustom(d0, rotations[0], rotations[1]);
            double d2 = d0;
            final Vec3 vec3 = entity.getPositionEyes(1);
            boolean flag = false;
            if (mc.playerController.extendedReach()) {
                d0 = 6.0;
                d2 = 6.0;
            } else {
                if (d0 > 3.0) {
                    flag = true;
                }
            }
            if (objectMouseOver != null) {
                d2 = objectMouseOver.hitVec.distanceTo(vec3);
            }
            final Vec3 vec4 = entity.getLookCustom(rotations[0], rotations[1]);
            final Vec3 vec5 = vec3.addVector(vec4.xCoord * d0, vec4.yCoord * d0, vec4.zCoord * d0);
            Entity pointedEntity = null;
            Vec3 vec6 = null;
            final float f = 1.0f;
            final List list = mc.theWorld
                    .getEntitiesInAABBexcluding(
                            entity, entity.getEntityBoundingBox()
                                    .addCoord(vec4.xCoord * d0, vec4.yCoord * d0, vec4.zCoord * d0).expand(f, f, f),
                            Predicates.and(EntitySelectors.NOT_SPECTATING));
            double d3 = d2;
            final AxisAlignedBB realBB = null;
            for (int i = 0; i < list.size(); ++i) {
                final Entity entity2 = (Entity) list.get(i);
                final float f2 = (float) (entity2.getCollisionBorderSize());
                final AxisAlignedBB axisalignedbb = entity2.getEntityBoundingBox().expand(f2, f2, f2);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec5);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (d3 >= 0.0) {
                        pointedEntity = entity2;
                        vec6 = ((movingobjectposition == null) ? vec3 : movingobjectposition.hitVec);
                        d3 = 0.0;
                    }
                } else if (movingobjectposition != null) {
                    final double d4 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d4 < d3 || d3 == 0.0) {

                        if (entity2 == entity.ridingEntity) {
                            if (d3 == 0.0) {
                                pointedEntity = entity2;
                                vec6 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity2;
                            vec6 = movingobjectposition.hitVec;
                            d3 = d4;
                        }
                    }
                }
            }
            if (pointedEntity != null && flag && vec3.distanceTo(vec6) > range) {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec6, null,
                        new BlockPos(vec6));
            }
            if (pointedEntity != null && (d3 < d2 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec6);
            }
            return pointedEntity;
        }
        return null;
	}

    public static boolean canEntityBeSeen(Entity entityIn) {
        double expand = entityIn.getCollisionBorderSize();
        AxisAlignedBB entityBox = entityIn.getEntityBoundingBox().expand(expand, expand, expand);
        for (double y = entityBox.minY; y <= entityBox.maxY; y += 0.008) {
            Vec3 startVec = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            Vec3 targetVec = new Vec3(entityIn.posX, y, entityIn.posZ);
            if (mc.theWorld.rayTraceBlocks(startVec, targetVec) == null) {
                return true;
            }
        }
        return false;
    }
}
