package team.gothaj.client.impl.module.combat;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageCalculator extends Module {

    public final BooleanProperty showDamage = new BooleanProperty("Show Damage", true);

    public final NumberProperty tolerance = new NumberProperty("Tolerance %", 40f, 5f, 95f, 5f);

    private static final int HEALTH_FIELD = 6;
    private static final long ATTACK_WINDOW_MS = 500L;
    private static final long BASELINE_TIMEOUT_MS = 10_000L;
    private static final int BASELINE_HITS = 3;
    private static final String MITIGATED = EnumChatFormatting.RED + "You are mitigated.";

    private final Map<EntityLivingBase, Float> knownHealth = new HashMap<>();
    private final Map<EntityLivingBase, PendingHit> pending = new HashMap<>();
    private final Map<EntityLivingBase, List<Float>> recentDamage = new HashMap<>();
    private final Map<EntityLivingBase, Long> lastHitTime = new HashMap<>();

    private static class PendingHit {
        final long time;

        PendingHit(long time) {
            this.time = time;
        }
    }

    public DamageCalculator() {
        super("Mitigation Detector", "Detects when a target is taking less damage than they should", ModuleCategory.COMBAT);
        registerProperties(showDamage, tolerance);
    }

    @Listen
    public void onAttack(AttackEntityEvent event) {
        EntityLivingBase target = event.getEntity();
        if (target == null || mc.thePlayer == null) return;

        pending.put(target, new PendingHit(System.currentTimeMillis()));
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (!(event.getPacket() instanceof S1CPacketEntityMetadata packet)) return;
        if (packet.func_149376_c() == null) return;

        Float newHealth = null;
        for (DataWatcher.WatchableObject obj : packet.func_149376_c()) {
            if (obj.getDataValueId() == HEALTH_FIELD && obj.getObject() instanceof Float) {
                newHealth = (Float) obj.getObject();
                break;
            }
        }
        if (newHealth == null) return;

        if (mc.theWorld == null) {
            knownHealth.clear();
            return;
        }

        Entity entity = mc.theWorld.getEntityByID(packet.getEntityId());
        if (!(entity instanceof EntityLivingBase living) || entity == mc.thePlayer) return;

        if (newHealth <= 0.0F) {
            knownHealth.remove(living);
            pending.remove(living);
            recentDamage.remove(living);
            lastHitTime.remove(living);
            return;
        }

        Float last = knownHealth.get(living);
        if (last != null) {
            float drop = last - newHealth;
            if (drop >= 0.05f && drop < 20f) {
                handleDamageTaken(living, drop);
            }
        }
        knownHealth.put(living, newHealth);
    }

    private void handleDamageTaken(EntityLivingBase target, float actual) {
        PendingHit hit = pending.remove(target);
        if (hit == null) return;

        long now = System.currentTimeMillis();
        if (now - hit.time > ATTACK_WINDOW_MS) return;

        if (showDamage.getProperty()) {
            Logger.clientChat(formatDamage(actual));
        }

        List<Float> recent = recentDamage.get(target);
        if (recent == null || now - lastHitTime.getOrDefault(target, 0L) > BASELINE_TIMEOUT_MS) {
            recent = new ArrayList<>();
        }

        if (!recent.isEmpty()) {
            float baseline = 0.0F;
            for (float value : recent) {
                baseline += value;
            }
            baseline /= recent.size();

            float threshold = baseline * (1.0F - tolerance.getProperty() / 100.0F);
            if (actual < threshold) {
                if (mc.theWorld != null) {
                    mc.ingameGUI.setRecordPlaying(MITIGATED, false);
                }
                Logger.addChat(new ChatComponentText(MITIGATED));
                return;
            }
        }

        recent.add(actual);
        if (recent.size() > BASELINE_HITS) {
            recent.remove(0);
        }
        recentDamage.put(target, recent);
        lastHitTime.put(target, now);
    }

    private String formatDamage(float damage) {
        return damage == Math.floor(damage) && !Float.isInfinite(damage)
                ? String.valueOf((int) damage)
                : String.format("%.1f", damage);
    }

    @Override
    public void onDisable() {
        knownHealth.clear();
        pending.clear();
        recentDamage.clear();
        lastHitTime.clear();
        super.onDisable();
    }
}
