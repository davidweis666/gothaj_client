package team.gothaj.client.api.component;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.main.GothajClient;

/**
 * @author Kawase
 * @since 15.08.2025
 */
@Getter
@Setter
public class Component implements Utils {

    public Component() {
        onEnable();
    }

    public void onEnable() {
        GothajClient.getInstance().getEventBus().subscribe(this);
    }

    public void onDisable() {
        GothajClient.getInstance().getEventBus().unsubscribe(this);
    }
}
