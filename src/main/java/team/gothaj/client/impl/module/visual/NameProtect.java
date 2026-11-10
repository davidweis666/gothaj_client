package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.property.properties.ModeProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class NameProtect extends Module {

    public final ListProperty targets = new ListProperty("Targets", "Yourself", "Everyone", "Client Users");

    public final InputProperty customEveryone = new InputProperty("Custom Everyone", "Protected").setVisible(() -> targets.getOptionState("Everyone"));

    public final ModeProperty ownMode = new ModeProperty("Self Mode", "IRC", "IRC", "Custom").setVisible(() -> targets.getOptionState("Yourself"));

    public final InputProperty customSelf = new InputProperty("Custom Self", "You").setVisible(() -> targets.getOptionState("Yourself") && ownMode.is("Custom"));


    public NameProtect() {
        super("NameProtect", "", ModuleCategory.VISUAL);
        registerProperties(targets, customEveryone, ownMode, customSelf);
    }
}
