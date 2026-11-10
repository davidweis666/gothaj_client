package team.gothaj.client.impl.module.combat;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.impl.CancellableEvent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class Backtrack extends Module {

	private WorldClient lastWorld;

	public EntityLivingBase entity;

	public TimerUtil timer = new TimerUtil();

	public BooleanProperty outBoundBlink = new BooleanProperty("Out Bound Blink", false);
	public NumberProperty hitRange = new NumberProperty("Hit Range", 3, 0, 10, 0.1f);

	public BooleanProperty adaptiveDelay = new BooleanProperty("Adaptive Delay", false);

	public NumberProperty delay = new NumberProperty("Delay", 400, 0, 2000, 10).setVisible(() -> !adaptiveDelay.getProperty());

	public BooleanProperty esp = new BooleanProperty("ESP", true);

	public Backtrack() {
		super("Backtrack", "a backtrack", ModuleCategory.COMBAT);
		registerProperties(outBoundBlink, hitRange, adaptiveDelay, delay, esp);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		timer.reset();
	}

	private BlinkComponent blink() {
		return GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class);
	}

	@Override
	public String getSuffix() {
		return delay.getProperty().intValue() + "ms";
	}

	@Listen
	public void onPacket(PacketEvent event) {
		try {

			if (event.getDirection().equals(PacketEvent.Direction.RECEIVE)) {
				synchronized (this) {
					if (!canWork())
						return;

					Packet packet = event.getPacket();

					if (packet instanceof S14PacketEntity) {
						S14PacketEntity packetEntity = (S14PacketEntity) packet;
						final Entity e = mc.theWorld.getEntityByID(packetEntity.entityId);

						if (e instanceof EntityLivingBase) {
							EntityLivingBase entityLivingBase = (EntityLivingBase) e;
							entityLivingBase.realPosX += packetEntity.func_149062_c();
							entityLivingBase.realPosY += packetEntity.func_149061_d();
							entityLivingBase.realPosZ += packetEntity.func_149064_e();
						}
					}

					if (packet instanceof S18PacketEntityTeleport) {
						S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;
						final Entity e = mc.theWorld.getEntityByID(teleportPacket.getEntityId());

						if (e instanceof EntityLivingBase) {
							EntityLivingBase entityLivingBase = (EntityLivingBase) e;
							entityLivingBase.realPosX = teleportPacket.getX();
							entityLivingBase.realPosY = teleportPacket.getY();
							entityLivingBase.realPosZ = teleportPacket.getZ();
						}
					}

					if (mc.theWorld != null) {
						if (lastWorld != mc.theWorld) {
							lastWorld = mc.theWorld;
						}

						entity.lastTickRealPosX = entity.realPosX;
						entity.lastTickRealPosY = entity.realPosY;
						entity.lastTickRealPosZ = entity.realPosZ;

						addIncomingPackets(packet, event);
					}

					lastWorld = mc.theWorld;
				}
			}
			if (event.getDirection().equals(PacketEvent.Direction.SEND) && outBoundBlink.getProperty()) {
				if (!canWork())
					return;

				Packet packet = event.getPacket();
				addOutgoingPackets(packet, event);
			}
		} catch (Exception ex) {
		}
	}

	@Listen
	public void onTick(TickEvent event) {
		try {
			if (!canWork())
				return;

			double realX = entity.realPosX / 32.0;
			double realY = entity.realPosY / 32.0;
			double realZ = entity.realPosZ / 32.0;

			double currentX = entity.posX;
			double currentY = entity.posY;
			double currentZ = entity.posZ;

			double realDistance = EntityUtil.getDistanceToEntityPositionBox(entity, realX, realY, realZ);
			double currentDistance = EntityUtil.getDistanceToEntityPositionBox(entity, currentX, currentY, currentZ);

			double maxRange = hitRange.getProperty();
			if (!mc.thePlayer.canEntityBeSeen(entity)) {
				maxRange = Math.min(maxRange, 3.0);
			}

			boolean need = mc.thePlayer.getDistance(realX, realY, realZ) >= mc.thePlayer.getDistance(currentX, currentY,
					currentZ);
			boolean lastPosCloser = mc.thePlayer.getDistance(entity.lastTickRealPosX / 32, entity.lastTickRealPosY / 32,
					entity.lastTickRealPosZ / 32) > mc.thePlayer.getDistance(entity.realPosX, entity.realPosY,
							entity.realPosZ);
			boolean withinRange = mc.thePlayer.getDistance(realX, realY, realZ) < maxRange;

			double deltaX = (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * 2;
			double deltaZ = (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * 2;
			double predictedPlayerX = mc.thePlayer.posX + deltaX;
			double predictedPlayerZ = mc.thePlayer.posZ + deltaZ;

			boolean close = mc.thePlayer.getDistance(currentX, currentY, currentZ) < 2.6 && mc.thePlayer.hurtTime == 0;

			long maxDelay = delay.getProperty().longValue();
			if (adaptiveDelay.getProperty()) {
				double factor = Math.max(0.0, Math.min(1.0, realDistance / hitRange.getProperty().doubleValue()));
				maxDelay = (long) (maxDelay * factor);
			}

			boolean timerExpired = timer.hasTimeReached(maxDelay, false);
			boolean isTargetMoving = !MovementUtil.isEntityMoving(entity);

			if (isTargetMoving || !need || lastPosCloser || close || !withinRange || timerExpired) {
				resetIncomingPackets();
				resetOutgoingPackets();
			}
		} catch (Exception ex) {

		}
	}

	@Listen
	public void onRender(RenderDimensionEvent event) {
		if (esp.getProperty()) {
			try {
				if (!canWork())
					return;

				double realX = entity.realPosX / 32.0;
				double realY = entity.realPosY / 32.0;
				double realZ = entity.realPosZ / 32.0;

				double currentX = entity.posX;
				double currentY = entity.posY;
				double currentZ = entity.posZ;

				double realDistance = EntityUtil.getDistanceToEntityPositionBox(entity, realX, realY, realZ);
				double currentDistance = EntityUtil.getDistanceToEntityPositionBox(entity, currentX, currentY,
						currentZ);

				double maxRange = hitRange.getProperty();
				if (!mc.thePlayer.canEntityBeSeen(entity)) {
					maxRange = Math.min(maxRange, 3.0);
				}

				boolean need = mc.thePlayer.getDistance(realX, realY, realZ) >= mc.thePlayer.getDistance(currentX,
						currentY, currentZ);
				boolean lastPosCloser = mc.thePlayer.getDistance(entity.lastTickRealPosX / 32,
						entity.lastTickRealPosY / 32, entity.lastTickRealPosZ / 32) > mc.thePlayer
								.getDistance(entity.realPosX, entity.realPosY, entity.realPosZ);
				boolean withinRange = mc.thePlayer.getDistance(realX, realY, realZ) < maxRange;
				boolean close = mc.thePlayer.getDistance(currentX, currentY, currentZ) < 2.6
						&& mc.thePlayer.hurtTime == 0;

				long maxDelay = delay.getProperty().longValue();
				if (adaptiveDelay.getProperty()) {
					double factor = Math.max(0.0, Math.min(1.0, realDistance / hitRange.getProperty().doubleValue()));
					maxDelay = (long) (maxDelay * factor);
				}

				boolean timerExpired = timer.hasTimeReached(maxDelay, false);
				boolean isTargetMoving = !MovementUtil.isEntityMoving(entity);

				if (isTargetMoving || !need || lastPosCloser || close || !withinRange || timerExpired) {
					RenderUtil.drawOutlinedCylinder(entity);
				}
			} catch (Exception ex) {

			}
		}
	}

	private void releasePacketsToDistance() {
		if (entity == null)
			return;

		BlinkComponent bc = blink();
		final double[] pos = { entity.posX, entity.posY, entity.posZ };

		while (mc.thePlayer.getDistance(pos[0], pos[1], pos[2]) < 3 && !bc.isInboundEmpty()) {
			try {
				bc.receiveAndRemoveFirstInbound(pkt -> {
					if (pkt instanceof S14PacketEntity packet) {
						final Entity e = mc.theWorld.getEntityByID(packet.entityId);
						if (e instanceof EntityLivingBase) {
							pos[0] += packet.func_149062_c() / 32.0;
							pos[1] += packet.func_149061_d() / 32.0;
							pos[2] += packet.func_149064_e() / 32.0;
						}
					}
					if (pkt instanceof S18PacketEntityTeleport packet) {
						final Entity e = mc.theWorld.getEntityByID(packet.getEntityId());
						if (e instanceof EntityLivingBase) {
							pos[0] = packet.getX() / 32.0;
							pos[1] = packet.getY() / 32.0;
							pos[2] = packet.getZ() / 32.0;
						}
					}
				});
			} catch (Exception ex) {
				break;
			}
		}
	}

	private void resetIncomingPackets() {
		blink().flushInboundPackets();
		timer.reset();
	}

	private void resetOutgoingPackets() {
		blink().flushOutboundPackets();
		timer.reset();
	}

	private void addIncomingPackets(Packet packet, CancellableEvent event) {
		if (event == null || packet == null || event.isCancelled()) {
			return;
		}
		if (blockIncomingPacket(packet)) {
			blink().offerInboundPacket(packet);
			event.setCancelled(true);
		}

	}

	private void addOutgoingPackets(Packet packet, CancellableEvent event) {
		if (event == null || packet == null || event.isCancelled()) {
			return;
		}
		blink().offerOutboundPacket(packet);
		event.setCancelled(true);
	}

	private boolean blockIncomingPacket(Packet packet) {
		return (packet instanceof S32PacketConfirmTransaction || packet instanceof S27PacketExplosion
				|| packet instanceof S12PacketEntityVelocity || packet instanceof S00PacketKeepAlive
				|| packet instanceof S03PacketTimeUpdate || packet instanceof S14PacketEntity
				|| packet instanceof S19PacketEntityHeadLook || packet instanceof S18PacketEntityTeleport
				|| packet instanceof S0FPacketSpawnMob || packet instanceof S08PacketPlayerPosLook
				|| (packet instanceof S01PacketPong && !outBoundBlink.getProperty()));
	}

	private boolean entityExists() {
		if (entity == null)
			return false;
		return entity.getEntityBoundingBox() != null && mc.thePlayer != null && mc.theWorld != null
				&& entity.realPosX != 0 && entity.realPosY != 0 && entity.realPosZ != 0 && entity.width != 0
				&& entity.height != 0;
	}

	private boolean canWork() {
		entity = null;
		if (getCastedModule(KillAura.class).isToggled()) {
			entity = getCastedModule(KillAura.class).target;
		}

		if (mc.thePlayer == null || mc.theWorld == null
				|| mc.getNetHandler().getNetworkManager().getNetHandler() == null) {
			resetIncomingPackets();
			resetOutgoingPackets();
			return false;
		}
		if (getCastedModule(Scaffold.class).isToggled()) {
			resetIncomingPackets();
			resetOutgoingPackets();
			return false;
		}
		if (entity == null) {
			resetIncomingPackets();
			resetOutgoingPackets();
			return false;
		}
		return true;
	}
}
