package team.gothaj.client.impl.listener.input;

import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.listener.Listener;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.impl.event.game.GameLoopEvent;
import team.gothaj.client.impl.gui.widget.WidgetScreen;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.HashMap;
import java.util.Map;

public class InputListener extends Listener {

    private final Map<Module, Boolean> states = new HashMap<>();

    @Listen
    public void onInput(GameLoopEvent event) {
        GothajClient.getInstance().getModuleManager().getItems().forEach(module -> {
            int key = module.getKeyCode();
            if (key == 0) return;
            boolean isKeyDownNow = Keyboard.isKeyDown(key);
            boolean lastKeyDownState = states.getOrDefault(module, false);
            if (mc.currentScreen == null) {
                if (module.isHold()) {
                    if (isKeyDownNow != lastKeyDownState) {
                        module.setToggled(isKeyDownNow);
                        states.put(module, isKeyDownNow);
                    }
                } else {
                    if (isKeyDownNow) {
                        if (!lastKeyDownState) {
                            module.setToggled(!module.isToggled());
                            states.put(module, true);
                        }
                    } else {
                        states.put(module, false);
                    }
                }
            } else {
                states.put(module, false);
                if (module.isHold() && module.isToggled()) {
                    module.setToggled(false);
                }
            }
        });

        if (GothajClient.getInstance().getWidgetKey() != 0 && Keyboard.isKeyDown(GothajClient.getInstance().getWidgetKey())) {
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new WidgetScreen());
            }
        }
    }
}