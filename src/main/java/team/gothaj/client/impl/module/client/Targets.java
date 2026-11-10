package team.gothaj.client.impl.module.client;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class Targets extends Module {

    public static final ListProperty combatTargets = new ListProperty("Combat", "Players", "Teammates", "Animals", "Monsters", "Invisibles", "Bots");

    public static final ListProperty visualTargets = new ListProperty("Visual", "Players", "Teammates", "Animals", "Monsters", "Invisibles", "Bots");

    public static final ListProperty botFilter = new ListProperty("Bot Filter", "Tablist");

    public Targets() {
        super("Targets", "Select your targets here", ModuleCategory.CLIENT);
        registerProperties(combatTargets, visualTargets, botFilter);
    }

    @Override
    public void setToggled(boolean toggle) {

    }
}
