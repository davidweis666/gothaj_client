package team.gothaj.client.api.notification;

import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.ConcurrentModificationException;

import static team.gothaj.client.api.util.Utils.mc;

public class NotificationManager extends ListManager<Notification> {

    @Override
    public void init() {
        GothajClient.getInstance().getEventBus().subscribe(this);
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        if (mc.theWorld == null && mc.currentScreen == null) {
            return;
        }

        try {
            items.removeIf(Notification::hasFinished);

            final int padding = 5;
            float y = event.getScaledResolution().getScaledHeight();
            final float x = event.getScaledResolution().getScaledWidth() - padding;
            for (final Notification notification : items) {
                y -= (float) (notification.getHeight() + padding);
                notification.renderShader(x - notification.getWidth(), y);
            }
        } catch (ConcurrentModificationException e) {

        }
    }

    @Listen
    public void onRender(RenderOverlayEvent event) {
        if (mc.theWorld == null && mc.currentScreen == null) {
            return;
        }

        try {
            items.removeIf(Notification::hasFinished);

            final int padding = 5;
            float y = event.getScaledResolution().getScaledHeight();
            final float x = event.getScaledResolution().getScaledWidth() - padding;
            for (final Notification notification : items) {
                y -= (float) (notification.getHeight() + padding);
                notification.render(x - notification.getWidth(), y);
            }
        } catch (ConcurrentModificationException e) {

        }

    }

    public void add(final String name, final String description, Notification.Type type) {
        items.add(new Notification(name, description, type));
    }

    public void add(final String name, final String description, Notification.Type type, long duration) {
        items.add(new Notification(name, description, type, duration));
    }
}
