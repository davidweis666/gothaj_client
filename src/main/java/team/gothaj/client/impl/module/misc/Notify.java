package team.gothaj.client.impl.module.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//TODO:: Target Behind ? Probably indicator
public class Notify extends Module {

    public final ListProperty notifyWhen = new ListProperty("Notify When", "Ender Pearl", "Fireball", "Target Behind");

    public final NumberProperty range = new NumberProperty("Notify Range", 10.0f, 4.0f, 25.0f, 0.5f);

    private final Map<EntityPlayer, Item> playerHoldingMap = new HashMap<>();

    private final TimerUtil timer = new TimerUtil();

    public Notify() {
        super("Notify", "Notifies you about important stuff", ModuleCategory.MISC);
        registerProperties(notifyWhen);
    }


    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {}

    @Listen
    public void onTick(TickEvent event) {
        List<EntityPlayer> players = mc.theWorld.playerEntities.stream()
                .filter(EntityUtil::isValid)
                .toList();

        if (!players.isEmpty()) {
            for (EntityPlayer player : players) {
                String playerName = player.getName();
                Item currentItem = null;

                if (player.getHeldItem() != null) {
                    if (notifyWhen.getOptionState("Ender Pearl") && player.getHeldItem().getItem() == Items.ender_pearl) {
                        currentItem = Items.ender_pearl;

                        if (!currentItem.equals(playerHoldingMap.get(player))) {
                            GothajClient.getInstance().getNotificationManager().add("Notify", playerName + " is holding an ender pearl!", Notification.Type.WARNING);
                        }
                    } else if (notifyWhen.getOptionState("Fireball") && player.getHeldItem().getItem() == Items.fire_charge) {
                        currentItem = Items.fire_charge;

                        if (!currentItem.equals(playerHoldingMap.get(player))) {
                            GothajClient.getInstance().getNotificationManager().add("Notify", playerName + " is holding a fireball!", Notification.Type.WARNING);
                        }
                    }
                }
                if (currentItem != null) {
                    playerHoldingMap.put(player, currentItem);

                    if (timer.hasTimeReached(10000)) {
                        playerHoldingMap.remove(player);
                        timer.reset();
                    }
                } else {
                    playerHoldingMap.remove(player);
                }
            }
        }
    }

    private Color getDistanceColor(double range) {
        if (range >= 15) {
            return new Color(53, 248, 18);
        } else if (range >= 10) {
            return new Color(171, 248, 18);
        } else if (range > 5) {
            return new Color(255, 245, 0);
        } else if (range <= 5) {
            return new Color(248, 18, 18);
        }
        return Color.WHITE;
    }


    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        playerHoldingMap.clear();
        super.onDisable();
    }
}
