package team.gothaj.client.impl.module.misc;

import systems.vajzii.irc.players.IrcPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.ActionProperty;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.input.MiddleClickEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class ClickFriends extends Module {

    public final BooleanProperty clientUsers = new BooleanProperty("Client Friends", false);

    public final ActionProperty removeAll = new ActionProperty("Remove All Friends").setListener(() -> {
        GothajClient.getInstance().getFriendManager().clear();

        GothajClient.getInstance().getNotificationManager().add("Friends", "Removed all your friends!", Notification.Type.INFO);
    });

    public ClickFriends() {
        super("ClickFriends", "", ModuleCategory.MISC);
        registerProperties(clientUsers, removeAll);
    }

    @Listen
    public void onMiddleClick(MiddleClickEvent event) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
            if (!GothajClient.getInstance().getFriendManager().isFriend(mc.objectMouseOver.entityHit.getName())) {
                GothajClient.getInstance().getFriendManager().addFriend(mc.objectMouseOver.entityHit.getName());
            } else {
                GothajClient.getInstance().getFriendManager().removeFriend(mc.objectMouseOver.entityHit.getName());
            }
        }
    }

    @Listen
    public void onUpdate(TickEvent event) {
        if (!clientUsers.getProperty()) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player instanceof EntityOtherPlayerMP entity) {
                if (IrcPlayer.getByIngameName(entity.getName()) != null) {
                    if (!GothajClient.getInstance().getFriendManager().isFriend(mc.objectMouseOver.entityHit.getName())) {
                        GothajClient.getInstance().getFriendManager().addFriend(mc.objectMouseOver.entityHit.getName());
                    }
                }
            }
        }
    }
}
