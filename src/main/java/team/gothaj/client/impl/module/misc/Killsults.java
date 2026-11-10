package team.gothaj.client.impl.module.misc;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.eventbus.Listen;

public class Killsults extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Normal", "Normal", "Gypsy");
    private final NumberProperty delay = new NumberProperty("Delay", 5, 1, 10, 1);

    private final String[] insultsNormal = {
            "buy gothaj.top",
            "gothaj.top - skill issue solved",
            "get clapped by gothaj.top",
            "gothaj.top owns you",
            "cope from gothaj.top",
            "gothaj.top on top",
            "gothaj.top diff",
            "gothaj.top > you",
            "you just got gothaj'd",
            "gothaj.top merchant",
            "gothaj.top is unbeatable",
            "cry about gothaj.top",
            "skill issue? try gothaj.top",
            "gothaj.top runs this server",
            "mad cause gothaj.top",
            "gothaj.top owns your account",
            "stay mad, stay clean - gothaj.top",
            "always dying? try gothaj.top",
            "stop coping, start winning - gothaj.top",
            "gothaj.top living rent free",
    };

    private final String[] insultsGypsy = {
        "mulo gadjo, buy gothaj.top",
        "dzukel chavo get clapped",
        "nasul dikhel, gothaj.top on top",
        "baro gadjo, sit down",
        "mulo dikhel, stay mad",
        "chavo mulo, ez gothaj.top",
        "sastar chavo? nah, gothaj.top owns you",
        "nasul rrom, cope harder",
        "gothaj.top marde your whole server",
        "dzukel chavo get rekt",
        "xoxado gadjo, uninstall",
        "gothaj.top phral, you're dogwater",
        "tikno chavo, stop crying",
        "gothaj.top is nasul proof",
        "mulo gadjo, get gothaj'd",
        "but nasul, stay bad",
        "gothaj.top dzukel merchant",
        "sastar chavo vs gothaj.top diff",
        "chavo xoxado, gothaj.top living free",
        "phral get clapped by gothaj.top",
    };

    private EntityLivingBase lastTarget;
    private long lastInsultTime;

    public Killsults() {
        super("Killsults", "Sends random insults on kill", ModuleCategory.MISC);
        registerProperties(mode, delay);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Listen
    public void onAttack(AttackEntityEvent event) {
        lastTarget = event.getEntity();
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (lastTarget == null) return;
        if (!lastTarget.isDead) return;

        insult();
        lastTarget = null;
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (lastTarget == null) return;
        if (!(event.getPacket() instanceof S19PacketEntityStatus packet)) return;
        if (packet.getOpCode() != 3) return;

        EntityLivingBase dead = (EntityLivingBase) packet.getEntity(mc.theWorld);
        if (dead == null) return;
        if (dead.getEntityId() != lastTarget.getEntityId()) return;

        insult();
        lastTarget = null;
    }

    public void insult() {
        long cooldown = (long) (delay.getProperty() * 1000L);
        if (System.currentTimeMillis() - lastInsultTime < cooldown) return;
        lastInsultTime = System.currentTimeMillis();

        if (mode.is("Normal")) {
            mc.thePlayer.sendChatMessage(insultsNormal[MathUtil.getRandomNumber(0, insultsNormal.length)]);
        }
        if (mode.is("Gypsy")) {
            mc.thePlayer.sendChatMessage(insultsGypsy[MathUtil.getRandomNumber(0, insultsGypsy.length)]);
        }
    }
}
