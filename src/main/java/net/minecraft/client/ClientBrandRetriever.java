package net.minecraft.client;

import team.gothaj.client.impl.module.client.ClientSpoofer;
import team.gothaj.client.main.GothajClient;

public class ClientBrandRetriever
{
    public static String getClientModName()
    {
        if (GothajClient.getInstance().getModuleManager() != null
                && GothajClient.getInstance().getModuleManager().find(ClientSpoofer.class) != null
                && GothajClient.getInstance().getModuleManager().find(ClientSpoofer.class).isToggled()
                && ClientSpoofer.client != null) {
            return ClientSpoofer.client;
        }

        return "vanilla";
    }
}
