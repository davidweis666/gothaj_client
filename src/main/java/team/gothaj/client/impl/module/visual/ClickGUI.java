package team.gothaj.client.impl.module.visual;

import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.impl.gui.click.gothaj.GothajGUI;
import team.gothaj.client.impl.gui.click.modern.ModernDropdown;
import team.gothaj.client.impl.gui.click.oldgothaj.OldGothajGUI;
import team.gothaj.client.impl.gui.click.panel.PanelGUI;

public class ClickGUI extends Module {


    public final ModeProperty mode = new ModeProperty("UI Mode", "Modern", "Panel", "Modern", "Gothaj", "OldGothaj");

    public final BooleanProperty closeSetting = new BooleanProperty("Close Previous", true);

    public final BooleanProperty blur = new BooleanProperty("Blur", true);


    public ClickGUI() {
        super("ClickGUI", "Displays your modules", ModuleCategory.VISUAL);
        registerProperties(mode, blur);
        setKeyCode(Keyboard.KEY_RSHIFT);

    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        switch (mode.getProperty()) {
            case "Modern" -> mc.displayGuiScreen(ModernDropdown.getInstance());
            case "Gothaj" -> mc.displayGuiScreen(GothajGUI.getGUI());
            case "Panel" -> mc.displayGuiScreen(PanelGUI.getInstance());
            case "OldGothaj" -> mc.displayGuiScreen(new OldGothajGUI());
        }


        setToggled(false);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
