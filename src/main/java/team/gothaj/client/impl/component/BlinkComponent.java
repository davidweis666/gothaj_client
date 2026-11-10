package team.gothaj.client.impl.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import team.gothaj.client.api.component.Component;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.api.util.network.TimedPacket;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.eventbus.Listen;

import java.awt.Color;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;

public class BlinkComponent extends Component {
	public final LinkedList<TimedPacket> inBound = new LinkedList<>();
	public final LinkedList<TimedPacket> outBound = new LinkedList<>();

	@Getter
	@Setter
	public boolean ESP = true;

	/** When inbound capture is on, queue server → client packets only if true. */
	public boolean blinkingInBound = false;
	/** When outbound capture is on, queue client → server packets only if true. */
	public boolean blinkingOutBound = false;

	public boolean isBlinkingInbound() {
		return blinkingInBound;
	}

	public boolean isBlinkingOutbound() {
		return blinkingOutBound;
	}

	public void startBlinkingInbound() {
		if (!blinkingInBound) {
			synchronized (inBound) {
				inBound.clear();
			}
			blinkingInBound = true;
		}
	}

	public void startBlinkingOutbound() {
		if (!blinkingOutBound) {
			synchronized (outBound) {
				outBound.clear();
			}
			blinkingOutBound = true;
			ESP = true;
		}
	}

	public void stopBlinkingInbound() {
		if (!blinkingInBound)
			return;
		blinkingInBound = false;
		flushInboundPackets();
	}

	public void stopBlinkingOutbound() {
		if (!blinkingOutBound)
			return;
		blinkingOutBound = false;
		ESP = false;
		flushOutboundPackets();
	}

	public void startBlinking(boolean inBound, boolean outBound) {
		if (inBound) {
			startBlinkingInbound();
		}
		if (outBound) {
			startBlinkingOutbound();
		}
	}

	public void stopBlinking(boolean inBound, boolean outBound) {
		if (inBound) {
			stopBlinkingInbound();
		}
		if (outBound) {
			stopBlinkingOutbound();
		}
	}

	public void releaseInboundPacketsToMS(int time) {
		if (!blinkingInBound) {
			flushInboundPackets();
			return;
		}
		if (!blinkingInBound)
			return;

		synchronized (inBound) {
			while (!inBound.isEmpty()) {
				TimedPacket p = inBound.getFirst();
				if (p.getCreated() + time < System.currentTimeMillis()) {
					PacketUtil.receiveNoEvent(p.getPacket());
					inBound.removeFirst();
				} else {
					break;
				}
			}
		}
	}

	public void releaseOutboundPacketsToMS(int time) {
		if (!blinkingOutBound)
			return;

		synchronized (outBound) {
			while (!outBound.isEmpty()) {
				TimedPacket p = outBound.getFirst();
				if (p.getCreated() + time < System.currentTimeMillis()) {
					PacketUtil.sendSilent(p.getPacket());
					outBound.removeFirst();
				} else {
					break;
				}
			}
		}
	}

	public void releasePacketsToMS(int time) {
		releaseInboundPacketsToMS(time);
		releaseOutboundPacketsToMS(time);
	}

	public void flushInboundPackets() {
		synchronized (inBound) {
			while (!inBound.isEmpty()) {
				PacketUtil.receiveNoEvent(inBound.removeFirst().getPacket());
			}
		}
	}

	public void flushOutboundPackets() {
		synchronized (outBound) {
			while (!outBound.isEmpty()) {
				PacketUtil.sendSilent(outBound.removeFirst().getPacket());
			}
		}
	}

	public void clearInboundWithoutReceiving() {
		synchronized (inBound) {
			inBound.clear();
		}
	}

	public void offerInboundPacket(Packet<?> packet, long createdMillis) {
		synchronized (inBound) {
			inBound.add(new TimedPacket(packet, createdMillis));
		}
	}

	public void offerInboundPacket(Packet<?> packet) {
		offerInboundPacket(packet, System.currentTimeMillis());
	}

	public void offerOutboundPacket(Packet<?> packet, long createdMillis) {
		synchronized (outBound) {
			outBound.add(new TimedPacket(packet, createdMillis));
		}
	}

	public void offerOutboundPacket(Packet<?> packet) {
		offerOutboundPacket(packet, System.currentTimeMillis());
	}

	public boolean isInboundEmpty() {
		synchronized (inBound) {
			return inBound.isEmpty();
		}
	}

	public TimedPacket peekFirstInbound() {
		synchronized (inBound) {
			return inBound.isEmpty() ? null : inBound.getFirst();
		}
	}

	public boolean receiveAndRemoveFirstInbound() {
		synchronized (inBound) {
			if (inBound.isEmpty())
				return false;
			PacketUtil.receiveNoEvent(inBound.removeFirst().getPacket());
			return true;
		}
	}

	public boolean receiveAndRemoveFirstInbound(Consumer<Packet> beforeReceive) {
		synchronized (inBound) {
			if (inBound.isEmpty())
				return false;
			TimedPacket tp = inBound.getFirst();
			if (beforeReceive != null)
				beforeReceive.accept(tp.getPacket());
			PacketUtil.receiveNoEvent(tp.getPacket());
			inBound.removeFirst();
			return true;
		}
	}

	@Listen
	public void onRender(RenderDimensionEvent event) {
		try {
			if (mc.thePlayer != null && mc.gameSettings.thirdPersonView > 0 && isESP() && blinkingOutBound) {
				C03PacketPlayer c03 = null;
				synchronized (outBound) {
					if (outBound.isEmpty())
						return;
					for (TimedPacket p : outBound) {
						if (p.getPacket() instanceof C03PacketPlayer) {
							c03 = (C03PacketPlayer) p.getPacket();
							break;
						}
					}
				}

				if (c03 == null)
					return;

				GlStateManager.pushMatrix();
				mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glEnable(GL11.GL_LINE_SMOOTH);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GlStateManager.disableCull();
				GL11.glDepthMask(false);

				final float x = (float) (c03.getPositionX() - mc.getRenderManager().renderPosX);
				final float y = (float) (c03.getPositionY() - mc.getRenderManager().renderPosY);
				final float z = (float) (c03.getPositionZ() - mc.getRenderManager().renderPosZ);

				RenderUtil.drawESPPos(x, y, z, 0.23f, mc.thePlayer.height + 0.1f, Color.white, true, true);

				GL11.glDepthMask(true);
				GlStateManager.enableCull();
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glDisable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDisable(GL11.GL_LINE_SMOOTH);
				GlStateManager.popMatrix();
			}
		} catch (Exception ex) {

		}
	}

	@Listen
	public void onPacket(PacketEvent event) {
		if (mc.thePlayer != null && mc.thePlayer.isDead) {
			stopBlinkingInbound();
			stopBlinkingOutbound();
		}
		if (mc.theWorld == null || event.isCancelled()) {
			return;
		}

		if (event.getDirection() == PacketEvent.Direction.SEND) {
			if (!isBlinkingOutbound()) {
				return;
			}
			
			event.setCancelled(true);
			synchronized (outBound) {
				outBound.add(new TimedPacket(event.getPacket(), System.currentTimeMillis()));
			}
			return;
		}

		if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
			if (!isBlinkingInbound()) {
				return;
			}

			event.setCancelled(true);
			synchronized (inBound) {
				inBound.add(new TimedPacket(event.getPacket(), System.currentTimeMillis()));
			}
		}
	}
}
