package team.gothaj.client.api.listener;

import net.minecraft.client.Minecraft;
import team.gothaj.client.main.GothajClient;

public abstract class Listener {

    protected final Minecraft mc = Minecraft.getMinecraft();

    protected final team.gothaj.eventbus.PubSub eventBus = GothajClient.getInstance().getEventBus();

    public void register() {
        eventBus.subscribe(this);
    }
}
