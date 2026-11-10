package team.gothaj.client.impl.module.combat;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class Backtrack67 extends Module {

	private final Deque<Packet<?>> packets = new ArrayDeque<>();
	private boolean delay = false;
	private final HashMap<Integer, Vec3> positions = new HashMap<>();

	private final RangedNumberProperty distance = new RangedNumberProperty("Distance", 1.0f, 3.0f, 0.0f, 6.0f, 0.1f);
	private final NumberProperty activateAfterHurtTime = new NumberProperty("Activate After Hurt Time", 8, 1, 10, 1);
	private final NumberProperty keepDelayedFor = new NumberProperty("Keep Delayed For", 3, 1, 10, 1);
	private final NumberProperty disableWhenHurt = new NumberProperty("Disable When Hurt", 6, 1, 10, 1);

	public Backtrack67() {
		super("Backtrack67", "a bt for dobule hits", ModuleCategory.COMBAT);
		registerProperties(distance, activateAfterHurtTime, keepDelayedFor, disableWhenHurt);
	}

	private void flush() {
		for (var p : packets) {
			mc.getNetHandler().addToReceiveQueueUnregistered(p);
		}
		packets.clear();
		positions.clear();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		flush();
	}

	@Listen
	public void onTick(TickEvent event) {
		delay = false;
		if (KillAura.target != null) {
			if (KillAura.target.hurtTime >= activateAfterHurtTime.getProperty())
				delay = true;
			else if (KillAura.target.hurtTimeTicks < keepDelayedFor.getProperty() && KillAura.target.hurtTime == 0)
				delay = true;
			if (mc.thePlayer.getDistanceToEntity(KillAura.target) < distance.getLeft() || mc.thePlayer.getDistanceToEntity(KillAura.target) > distance.getRight())
				delay = false;
		}
		if (mc.thePlayer != null && mc.thePlayer.hurtTime < disableWhenHurt.getProperty())
			delay = false;
	}

	@Listen
	public void onRenderDimension(RenderDimensionEvent event) {
		for (var e : positions.entrySet()) {
			var id = e.getKey();
			var pos = e.getValue();

			final float x = (float) (pos.xCoord - RenderManager.renderPosX);
			final float y = (float) (pos.yCoord - RenderManager.renderPosY);
			final float z = (float) (pos.zCoord - RenderManager.renderPosZ);

			RenderUtil.drawESPPos(
					x, y, z,
					0.2f,
					1.8f,
					Color.WHITE,
					true,
					true
			);
		}
	}

	@Listen
	public void onPacket(PacketEvent event) {
		if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
		if (!delay)
			flush();
		else {
			if (event.getPacket() instanceof S13PacketDestroyEntities p) {
				for (var id : p.getEntityIDs()) {
					positions.remove(id);
				}
			}
			if (event.getPacket() instanceof S14PacketEntity.S15PacketEntityRelMove p) {
				var ent = mc.theWorld.getEntityByID(p.entityId);
				if (ent != null) {
					positions.putIfAbsent(
							p.entityId,
							new Vec3(ent.posX, ent.posY, ent.posZ)
					);
					var cur = positions.get(p.entityId);
					var dx = p.func_149062_c() / 32.0D;
					var dy = p.func_149061_d() / 32.0D;
					var dz = p.func_149064_e() / 32.0D;
					positions.put(
							p.entityId,
							cur.addVector(dx, dy, dz)
					);
				}
			}
			if (event.getPacket() instanceof S18PacketEntityTeleport p) {
				positions.put(
						p.getEntityId(),
						new Vec3(
								p.getX() / 32.0D,
								p.getY() / 32.0D,
								p.getZ() / 32.0D
						)
				);
			}
			if (event.getPacket() instanceof S14PacketEntity.S17PacketEntityLookMove p) {
				var ent = mc.theWorld.getEntityByID(p.entityId);
				if (ent != null) {
					positions.putIfAbsent(
							p.entityId,
							new Vec3(ent.posX, ent.posY, ent.posZ)
					);
					var cur = positions.get(p.entityId);
					var dx = p.func_149062_c() / 32.0D;
					var dy = p.func_149061_d() / 32.0D;
					var dz = p.func_149064_e() / 32.0D;
					positions.put(
							p.entityId,
							cur.addVector(dx, dy, dz)
					);
				}
			}
			packets.offer(event.getPacket());
			event.setCancelled(true);
		}
	}

}
