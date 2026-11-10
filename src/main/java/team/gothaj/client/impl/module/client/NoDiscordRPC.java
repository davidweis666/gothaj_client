package team.gothaj.client.impl.module.client;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.component.DiscordComponent;
import team.gothaj.client.main.GothajClient;

/**
 * @author vajzii
 * @since 2025
 */
public class NoDiscordRPC extends Module {

    public NoDiscordRPC() {
        super("NoDiscordRPC", "", ModuleCategory.CLIENT);
    }

}
