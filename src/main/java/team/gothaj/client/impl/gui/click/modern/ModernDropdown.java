package team.gothaj.client.impl.gui.click.modern;

import lombok.Getter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.ClientClickGUI;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.CategoryComponent;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.ModuleComponent;
import team.gothaj.client.impl.gui.config.ConfigMenu;
import team.gothaj.client.impl.gui.theme.SelectThemeMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author vajzii
 * @since 2025
 */
public class ModernDropdown extends ClientClickGUI {

    private static final float PANEL_GAP = 5;
    public static final float TITLE_RECT_HEIGHT = 15;

    private static final float SEARCH_WIDTH = 200;
    private static final float SEARCH_HEIGHT = 15;

    private final List<Component> panels = new ArrayList<>();
    private final List<Component> reversedListOfPanels = new ArrayList<>();

    private CategoryComponent draggingPanel;

    public boolean listeningForKey;

    private final GothajClient gothajClient;

    private String searchText = "";
    private boolean searchFocused = false;

    private float barAnim = 0f;
    private float barFocusAnim = 0f;

    @Getter
    private static final ModernDropdown instance = new ModernDropdown();

    public ModernDropdown() {
        float x = 10, y = 30;

        for (ModuleCategory moduleCategory : ModuleCategory.values()) {
            CategoryComponent component;

            panels.add(component = (CategoryComponent) new CategoryComponent(
                    this,
                    GothajClient.getInstance().getModuleManager().getModulesInCategory(moduleCategory),
                    x, y
            ).setIdentifier(moduleCategory.getName()));

            x += component.getWidth() + PANEL_GAP;
        }

        reversedListOfPanels.addAll(panels);
        Collections.reverse(reversedListOfPanels);

        gothajClient = GothajClient.getInstance();
    }

    @Override
    public void initGui() {
        super.initGui();
        barAnim = 0f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        RenderUtil.drawRect(0, 0, width, height, new Color(10,10,10,100));

        Fonts.nunito16.drawStringWithShadow("[MOUSE3] - Bind Module" , 2, scaledResolution.getScaledHeight() - 30, new Color(128, 128, 128, 220));
        Fonts.nunito16.drawStringWithShadow("[CTRL] - See Module Binds" , 2, scaledResolution.getScaledHeight() - 20, new Color(128, 128, 128, 220));
        Fonts.nunito16.drawStringWithShadow("[SHIFT + MOUSE3] - Hide/Unhide Module" , 2, scaledResolution.getScaledHeight() - 10, new Color(128, 128, 128, 220));

        drawSearchBar(scaledResolution);

        for (Component panel : panels) {
            if (isSearchActive() && panel instanceof CategoryComponent category && !hasVisibleModules(category)) {
                continue;
            }
            panel.draw(mouseX, mouseY, new ScaledResolution(mc));
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSearchBar(ScaledResolution sr) {
        barAnim += (1f - barAnim) * 0.18f;
        if (barAnim > 0.99f) barAnim = 1f;

        float focusTarget = searchFocused ? 1f : 0f;
        barFocusAnim += (focusTarget - barFocusAnim) * 0.2f;
        if (Math.abs(focusTarget - barFocusAnim) < 0.01f) barFocusAnim = focusTarget;

        float barX = sr.getScaledWidth() / 2f - SEARCH_WIDTH / 2f;
        float barY = 6f - (1f - barAnim) * (SEARCH_HEIGHT + 12f);

        int bgAlpha = (int) (170 * barAnim);
        Color bg = new Color(
                (int) (10 + (35 - 10) * barFocusAnim),
                (int) (10 + (35 - 10) * barFocusAnim),
                (int) (10 + (35 - 10) * barFocusAnim),
                (int) (bgAlpha + (220 - 170) * barFocusAnim * barAnim)
        );

        RoundedUtil.drawRound(barX, barY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, 6f, 6f, 6f, bg);

        int textAlpha = (int) (255 * barAnim);
        if (searchText.isEmpty() && !searchFocused) {
            Fonts.nunito16.drawStringWithShadow("Search modules...", barX + 5f,
                    barY + (SEARCH_HEIGHT - Fonts.nunito16.getHeight()) / 2f, new Color(128, 128, 128, (int) (200 * barAnim)));
        } else {
            boolean cursorOn = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
            Fonts.nunito16.drawStringWithShadow(searchText + (cursorOn ? "_" : ""), barX + 5f,
                    barY + (SEARCH_HEIGHT - Fonts.nunito16.getHeight()) / 2f, new Color(255, 255, 255, textAlpha));
        }
    }

    public boolean isSearchActive() {
        return !searchText.isEmpty();
    }

    public boolean isModuleVisible(Module module) {
        if (!isSearchActive()) return true;
        return module.getName().toLowerCase().contains(searchText.toLowerCase());
    }

    public boolean hasVisibleModules(CategoryComponent category) {
        for (ModuleComponent moduleComponent : category.moduleComponentList) {
            if (isModuleVisible((Module) moduleComponent.getOwner())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            for (Component panel : reversedListOfPanels) {
                if (panel.isMouseOver(mouseX, mouseY)) {
                    panel.mouseScrolled(mouseX, mouseY, dWheel);
                    break;
                }
            }
        }
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
            } else if (typedChar >= 32 && typedChar <= 126) {
                searchText += typedChar;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_RIGHT) {
            mc.displayGuiScreen(new ConfigMenu());
        }

        if (keyCode == Keyboard.KEY_LEFT) {
            mc.displayGuiScreen(new SelectThemeMenu());
        }

        if (keyCode == Keyboard.KEY_ESCAPE && !listeningForKey) {
            if (!searchText.isEmpty()) {
                searchText = "";
            } else {
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
            }
        }

        for (Component panel : reversedListOfPanels) {
            if (panel.isAcceptingKeypresses()) {
                panel.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        searchText = "";
        searchFocused = false;

        for (Component panel : reversedListOfPanels) {
            panel.onGuiClosed();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingPanel = null;

        for (Component panel : reversedListOfPanels) {
            if (panel.isMouseOver(mouseX, mouseY)) {
                panel.mouseReleased(mouseX, mouseY, state);
            }
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        ScaledResolution sr = new ScaledResolution(mc);
        float barX = sr.getScaledWidth() / 2f - SEARCH_WIDTH / 2f;
        float barY = 6f;

        if (mouseX >= barX && mouseX <= barX + SEARCH_WIDTH && mouseY >= barY && mouseY <= barY + SEARCH_HEIGHT) {
            searchFocused = true;
            return;
        }
        searchFocused = false;

        for (Component panel : reversedListOfPanels) {
            if (panel.isMouseOver(mouseX, mouseY)) {
                panel.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingPanel != null) {
            draggingPanel.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        } else {
            for (Component panel : reversedListOfPanels) {
                if (panel.isMouseOver(mouseX, mouseY)) {
                    panel.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

                    if (panel.shouldLockDragging())
                        draggingPanel = (CategoryComponent) panel;
                    break;
                }
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            mc.displayGuiScreen(new ConfigMenu());
        }
        super.actionPerformed(button);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    
}
