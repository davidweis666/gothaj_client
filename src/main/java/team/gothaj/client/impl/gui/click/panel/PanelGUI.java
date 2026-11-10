package team.gothaj.client.impl.gui.click.panel;

import lombok.Getter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.ClientClickGUI;
import team.gothaj.client.impl.gui.click.panel.property.*;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelGUI extends ClientClickGUI {

    @Getter
    private static final PanelGUI instance = new PanelGUI();

    private static final IFontRenderer iconFont = Fonts.catFont18;
    private static final IFontRenderer moduleFont = Fonts.nunito14;

    private static final float PANEL_WIDTH = 460f;
    private static final float PANEL_HEIGHT = 280f;
    private static final float HEADER_HEIGHT = 20f;
    private static final float CATEGORY_BAR_HEIGHT = 32f;
    private static final float MODULE_LIST_WIDTH = 120f;

    private float openAnimation = 0f;
    private boolean isClosing = false;

    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;
    private final float[] categoryHoverAnimation = new float[ModuleCategory.values().length];
    private float moduleScrollOffset = 0f;
    private float moduleTargetScroll = 0f;
    private float moduleMaxScroll = 0f;
    private float settingsScrollOffset = 0f;
    private float settingsTargetScroll = 0f;
    private float settingsMaxScroll = 0f;

    private Module bindingModule = null;
    private float bindBlinkAnimation = 0f;

    private final Map<Module, Float> moduleHoverAnimations = new HashMap<>();

    private final List<PanelPropertyComponent> settingsComponents = new ArrayList<>();

    private float panelX, panelY;
    private float dragOffsetX, dragOffsetY;
    private boolean isDragging = false;

    public boolean listeningForKey = false;

    public PanelGUI() {
    }

    private void centerPanel() {
        ScaledResolution sr = new ScaledResolution(mc);
        panelX = (sr.getScaledWidth() - PANEL_WIDTH) / 2f;
        panelY = (sr.getScaledHeight() - PANEL_HEIGHT) / 2f;
    }

    private String getCategoryIcon(ModuleCategory category) {
        return switch (category) {
            case CLIENT -> "a";
            case COMBAT -> "b";
            case MISC -> "c";
            case MOVEMENT -> "d";
            case PLAYER -> "e";
            case VISUAL -> "f";
            case WORLD -> "g";
            case GHOST -> "h";
        };
    }

    @Override
    public void initGui() {
        super.initGui();
        centerPanel();
        openAnimation = 0f;
        isClosing = false;
        if (selectedModule == null) {
            List<Module> modules = GothajClient.getInstance().getModuleManager().getModulesInCategory(selectedCategory);
            if (!modules.isEmpty()) {
                selectedModule = modules.getFirst();
                buildSettingsComponents();
            }
        }
    }

    private void buildSettingsComponents() {
        settingsComponents.clear();
        if (selectedModule == null) return;

        for (Property<?> property : selectedModule.getProperties()) {
            PanelPropertyComponent component = null;

            if (property instanceof BooleanProperty bp) {
                component = new PanelBooleanComponent(bp);
            } else if (property instanceof ModeProperty mp) {
                component = new PanelModeComponent(mp);
            } else if (property instanceof NumberProperty np) {
                component = new PanelSliderComponent(np);
            } else if (property instanceof RangedNumberProperty rnp) {
                component = new PanelRangedSliderComponent(rnp);
            } else if (property instanceof ColorProperty cp) {
                component = new PanelColorComponent(cp);
            } else if (property instanceof ListProperty lp) {
                component = new PanelListComponent(lp);
            } else if (property instanceof FontProperty fp) {
                component = new PanelFontComponent(fp);
            } else if (property instanceof PatternProperty pp) {
                component = new PanelPatternComponent(pp);
            }

            if (component != null) {
                settingsComponents.add(component);
            }
        }
        settingsScrollOffset = 0;
        settingsTargetScroll = 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (isClosing) {
            mc.displayGuiScreen(null);
            return;
        }

        openAnimation += (1f - openAnimation) * 0.15f;
        moduleScrollOffset += (moduleTargetScroll - moduleScrollOffset) * 0.4f;
        settingsScrollOffset += (settingsTargetScroll - settingsScrollOffset) * 0.4f;

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, (int)(100 * openAnimation)));
        drawPanel(mouseX, mouseY, sr, openAnimation);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(int mouseX, int mouseY, ScaledResolution sr, float alpha) {
        Color primary = Hud.primaryColor.getProperty();

        if (isDragging) {
            panelX = mouseX - dragOffsetX;
            panelY = mouseY - dragOffsetY;
        }

        Color panelBg = new Color(8, 8, 8, (int)(255 * alpha));
        RoundedUtil.drawRound(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 4f, panelBg);

        Color borderColor = new Color(30, 30, 35, (int)(255 * alpha));
        RoundedUtil.drawRound(panelX - 0.5f, panelY - 0.5f, PANEL_WIDTH + 1f, PANEL_HEIGHT + 1f, 4.5f, borderColor);
        RoundedUtil.drawRound(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 4f, panelBg);

        drawHeader(alpha, primary);

        float categoryBarY = panelY + PANEL_HEIGHT - CATEGORY_BAR_HEIGHT;
        drawCategoryBar(mouseX, mouseY, categoryBarY, alpha, primary);

        float contentY = panelY + HEADER_HEIGHT;
        float contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - CATEGORY_BAR_HEIGHT - 4f;

        drawModuleList(mouseX, mouseY, sr, contentY, contentHeight, alpha, primary);

        float settingsX = panelX + MODULE_LIST_WIDTH + 8f;
        float settingsWidth = PANEL_WIDTH - MODULE_LIST_WIDTH - 16f;
        drawSettings(mouseX, mouseY, sr, settingsX, contentY + 4f, settingsWidth, contentHeight - 8f, alpha, primary);
    }

    private void drawHeader(float alpha, Color primary) {
        String title = GothajClient.getInstance().getFancyName().toUpperCase();
        FontUtil.drawHorizontalGradient(Fonts.surf16, title, panelX + 8f, panelY + 10f, 5);

        RenderUtil.drawGradientRectHorizontal(panelX + 4f, panelY + HEADER_HEIGHT - 1f, PANEL_WIDTH - 8f, .5F,
            withAlpha(Hud.primaryColor.getProperty(), alpha).getRGB(), withAlpha(Hud.secondaryColor.getProperty(), alpha).getRGB());
    }

    private void drawCategoryBar(int mouseX, int mouseY, float y, float alpha, Color primary) {
        Color barBg = new Color(12, 12, 12, (int)(255 * alpha));
        RoundedUtil.drawRound(panelX + 4f, y, PANEL_WIDTH - 8f, CATEGORY_BAR_HEIGHT - 4f, 4f, barBg);

        ModuleCategory[] categories = ModuleCategory.values();
        float iconSize = 24f;

        float totalWidth = categories.length * iconSize + (categories.length - 1) * 8f;
        float startX = panelX + (PANEL_WIDTH - totalWidth) / 2f;

        for (int i = 0; i < categories.length; i++) {
            ModuleCategory cat = categories[i];
            boolean selected = cat == selectedCategory;
            String icon = getCategoryIcon(cat);

            float iconX = startX + i * (iconSize + 8f);
            float iconY = y + (CATEGORY_BAR_HEIGHT - 4f - iconSize) / 2f;

            boolean hovered = mouseX >= iconX && mouseX <= iconX + iconSize &&
                             mouseY >= iconY && mouseY <= iconY + iconSize;

            float targetHover = (hovered || selected) ? 1f : 0f;
            categoryHoverAnimation[i] += (targetHover - categoryHoverAnimation[i]) * 0.2f;

            if (selected) {
                Color selBg = withAlpha(primary, alpha * 0.25f);
                RoundedUtil.drawRound(iconX - 2f, iconY - 2f, iconSize + 4f, iconSize + 4f, 4f, selBg);
            } else if (categoryHoverAnimation[i] > 0.01f) {
                Color hoverBg = new Color(255, 255, 255, (int)(15 * categoryHoverAnimation[i] * alpha));

                RoundedUtil.drawRound(iconX - 2f, iconY - 2f, iconSize + 4f, iconSize + 4f, 4f, hoverBg);
            }

            Color iconColor;
            if (selected) {
                iconColor = withAlpha(primary, alpha);
            } else if (hovered) {
                iconColor = withAlpha(new Color(180, 180, 185), alpha);

            } else {
                iconColor = withAlpha(new Color(80, 80, 85), alpha);
            }

            float iconTextX = iconX + (iconSize - iconFont.getStringWidth(icon)) / 2f;
            float iconTextY = iconY + (iconSize - iconFont.getHeight()) / 2f + 1;
            iconFont.drawString(icon, iconTextX, iconTextY, iconColor.getRGB());
        }
    }

    private void drawModuleList(int mouseX, int mouseY, ScaledResolution sr, float y, float height, float alpha, Color primary) {
        List<Module> modules = GothajClient.getInstance().getModuleManager().getModulesInCategory(selectedCategory);

        float listX = panelX + 8f;
        float listY = y + 4f;
        float listWidth = MODULE_LIST_WIDTH - 8f;
        float listHeight = height - 8f;

        Color listBg = new Color(12, 12, 12, (int)(255 * alpha));
        RoundedUtil.drawRound(listX, listY, listWidth, listHeight, 4f, listBg);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = sr.getScaleFactor();

        GL11.glScissor(
            (int)(listX * scaleFactor),
            (int)((sr.getScaledHeight() - listY - listHeight) * scaleFactor),
            (int)(listWidth * scaleFactor),
            (int)(listHeight * scaleFactor));

        float modY = listY + 4f - moduleScrollOffset;
        float totalHeight = 0f;
        float itemHeight = 16f;

        for (Module module : modules) {
            boolean selected = module == selectedModule;
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth &&
                             mouseY >= modY && mouseY <= modY + itemHeight &&
                             mouseY >= listY && mouseY <= listY + listHeight;
            boolean enabled = module.isToggled();

            float currentHover = moduleHoverAnimations.getOrDefault(module, 0f);
            float targetHover = (hovered || selected) ? 1f : 0f;
            currentHover += (targetHover - currentHover) * 0.2f;
            moduleHoverAnimations.put(module, currentHover);

            if (selected) {
                Color selBg = withAlpha(primary, alpha * 0.05f);
                RoundedUtil.drawRound(listX + 2f, modY, listWidth - 4f, itemHeight, 3f, selBg);
                RoundedUtil.drawRound(listX + 3f, modY + 4f, 1.5f, itemHeight - 8f, 1f, withAlpha(primary, alpha));
            } else if (currentHover > 0.01f) {
                Color hoverBg = new Color(255, 255, 255, (int)(8 * currentHover * alpha));
                RoundedUtil.drawRound(listX + 2f, modY, listWidth - 4f, itemHeight, 3f, hoverBg);
            }

            String moduleName = module.getName();
            Color textColor = enabled ? withAlpha(new Color(200, 200, 205), alpha) :
                                       withAlpha(new Color(90, 90, 95), alpha);
            float textY = modY + moduleFont.getMiddleOfBox(itemHeight) + 1f;
            moduleFont.drawString(moduleName, listX + 10f, textY, textColor.getRGB());

            modY += itemHeight + 2f;
            totalHeight += itemHeight + 2f;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        moduleMaxScroll = Math.max(0, totalHeight - listHeight + 8f);

        if (moduleMaxScroll > 0) {
            float sbTrackHeight = listHeight - 8f;
            float sbHeight = Math.max(20f, sbTrackHeight * (listHeight / (listHeight + moduleMaxScroll)));
            float sbY = listY + 4f + (moduleScrollOffset / moduleMaxScroll) * (sbTrackHeight - sbHeight);
            RoundedUtil.drawRound(listX + listWidth - 4f, sbY, 2f, sbHeight, 1f, withAlpha(primary, alpha * 0.4f));
        }
    }

    private void drawSettings(int mouseX, int mouseY, ScaledResolution sr, float x, float y, float width, float height, float alpha, Color primary) {
        Color settingsBg = new Color(12, 12, 12, (int)(255 * alpha));
        RoundedUtil.drawRound(x, y, width, height, 4f, settingsBg);

        if (selectedModule == null) {
            String msg = "Select a module";
            float msgWidth = moduleFont.getStringWidth(msg);
            moduleFont.drawString(msg, x + (width - msgWidth) / 2f, y + height / 2f - 4f,
                withAlpha(new Color(50, 50, 55), alpha).getRGB());
            return;
        }

        float headerY = y + 8f;
        Fonts.nunito18.drawString(selectedModule.getName(), x + 10f, headerY, withAlpha(Color.WHITE, alpha).getRGB());

        String keyName = selectedModule.getKeyCode() == 0 ? "None" : Keyboard.getKeyName(selectedModule.getKeyCode());
        String bindText = bindingModule == selectedModule ? "..." : "[" + keyName + "]";

        float bindWidth = Fonts.nunito16.getStringWidth(bindText);
        float bindX = x + width - bindWidth - 10f;
        float bindY = headerY + 0.5f;

        boolean bindHovered = mouseX >= bindX - 4f && mouseX <= bindX + bindWidth + 4f &&
                mouseY >= bindY - 2f && mouseY <= bindY + Fonts.nunito16.getHeight() + 2f;

        if (bindingModule == selectedModule) {
            bindBlinkAnimation += 0.1f;
            float blinkAlpha = (float)(Math.sin(bindBlinkAnimation) * 0.3f + 0.7f);
            Color bindBg = withAlpha(primary, alpha * blinkAlpha * 0.3f);
            RoundedUtil.drawRound(bindX - 4f, bindY - 2f, bindWidth + 8f,
                    Fonts.nunito16.getHeight() + 4f, 3f, bindBg);
            Fonts.nunito16.drawString(bindText, bindX, bindY,
                    withAlpha(primary, alpha).getRGB());
        } else if (bindHovered) {
            Color bindBg = withAlpha(new Color(255, 255, 255), alpha * 0.1f);
            RoundedUtil.drawRound(bindX - 4f, bindY - 2f, bindWidth + 8f,
                    Fonts.nunito16.getHeight() + 4f, 3f, bindBg);
            Fonts.nunito16.drawString(bindText, bindX, bindY,
                    withAlpha(new Color(200, 200, 205), alpha).getRGB());
        } else {
            Fonts.nunito16.drawString(bindText, bindX, bindY,
                    withAlpha(new Color(60, 60, 65), alpha).getRGB());
        }


        float contentY = headerY + 18f;
        float contentHeight = height - 34f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = sr.getScaleFactor();
        GL11.glScissor(
            (int)(x * scaleFactor),
            (int)((sr.getScaledHeight() - contentY - contentHeight) * scaleFactor),
            (int)(width * scaleFactor),
            (int)(contentHeight * scaleFactor));

        float propY = contentY - settingsScrollOffset;
        float totalHeight = 0f;

        for (PanelPropertyComponent component : settingsComponents) {
            if (!component.isVisible()) continue;

            component.setPosition(x + 8f, propY, width - 16f);
            component.draw(mouseX, mouseY, sr, alpha);

            float compHeight = component.getHeight();
            propY += compHeight + 4f;
            totalHeight += compHeight + 4f;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        settingsMaxScroll = Math.max(0, totalHeight - contentHeight + 8f);

        if (settingsMaxScroll > 0) {
            float sbTrackHeight = contentHeight - 8f;
            float sbHeight = Math.max(20f, sbTrackHeight * (contentHeight / (contentHeight + settingsMaxScroll)));
            float sbY = contentY + 4f + (settingsScrollOffset / settingsMaxScroll) * (sbTrackHeight - sbHeight);
            RoundedUtil.drawRound(x + width - 5f, sbY, 2f, sbHeight, 1f, withAlpha(primary, alpha * 0.4f));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= panelY && mouseY <= panelY + HEADER_HEIGHT) {
            isDragging = true;
            dragOffsetX = mouseX - panelX;
            dragOffsetY = mouseY - panelY;
            return;
        }

        float categoryBarY = panelY + PANEL_HEIGHT - CATEGORY_BAR_HEIGHT;
        ModuleCategory[] categories = ModuleCategory.values();
        float iconSize = 24f;
        float totalWidth = categories.length * iconSize + (categories.length - 1) * 8f;
        float startX = panelX + (PANEL_WIDTH - totalWidth) / 2f;

        for (int i = 0; i < categories.length; i++) {
            ModuleCategory cat = categories[i];
            float iconX = startX + i * (iconSize + 8f);
            float iconY = categoryBarY + (CATEGORY_BAR_HEIGHT - 4f - iconSize) / 2f;

            if (mouseX >= iconX && mouseX <= iconX + iconSize &&
                mouseY >= iconY && mouseY <= iconY + iconSize) {
                if (selectedCategory != cat && mouseButton != 2) {

                    selectedCategory = cat;
                    moduleScrollOffset = 0;
                    moduleTargetScroll = 0;
                    List<Module> modules = GothajClient.getInstance().getModuleManager().getModulesInCategory(selectedCategory);
                    if (!modules.isEmpty()) {
                        selectedModule = modules.getFirst();
                        buildSettingsComponents();
                    } else {
                        selectedModule = null;
                        settingsComponents.clear();
                    }
                }
                return;
            }
        }


        float contentY = panelY + HEADER_HEIGHT;
        float contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - CATEGORY_BAR_HEIGHT - 4f;
        float listX = panelX + 8f;
        float listY = contentY + 4f;
        float listWidth = MODULE_LIST_WIDTH - 8f;

        float listHeight = contentHeight - 8f;

        if (mouseX >= listX && mouseX <= listX + listWidth &&
            mouseY >= listY && mouseY <= listY + listHeight) {

            List<Module> modules = GothajClient.getInstance().getModuleManager().getModulesInCategory(selectedCategory);
            float modY = listY + 4f - moduleScrollOffset;
            float itemHeight = 16f;

            for (Module module : modules) {
                if (mouseY >= modY && mouseY <= modY + itemHeight) {
                    if (mouseButton == 0) {
                        module.toggle();
                    } else if (mouseButton == 1) {
                        selectedModule = module;
                        buildSettingsComponents();
                    }
                    return;
                }
                modY += itemHeight + 2f;
            }
        }

        float settingsX = panelX + MODULE_LIST_WIDTH + 8f;
        if (mouseX >= settingsX && selectedModule != null) {
            for (PanelPropertyComponent component : settingsComponents) {
                component.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        if (selectedModule != null) {
            float settingsWidth = PANEL_WIDTH - MODULE_LIST_WIDTH - 16f;
            float settingsY = contentY + 4f;
            float headerY = settingsY + 8f;

            String keyName = selectedModule.getKeyCode() == 0 ? "None" : Keyboard.getKeyName(selectedModule.getKeyCode());
            String bindText = bindingModule == selectedModule ? "..." : "[" + keyName + "]";
            float bindWidth = Fonts.nunito16.getStringWidth(bindText);
            float bindX = settingsX + settingsWidth - bindWidth - 10f;
            float bindY = headerY + 0.5f;

            if (mouseX >= bindX - 4f && mouseX <= bindX + bindWidth + 4f &&
                    mouseY >= bindY - 2f && mouseY <= bindY + Fonts.nunito16.getHeight() + 2f &&
                    mouseButton == 0) {
                if (bindingModule == selectedModule) {
                    bindingModule = null;
                } else {
                    bindingModule = selectedModule;
                    bindBlinkAnimation = 0f;
                }
                return;
            }
        }


        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        isDragging = false;
        for (PanelPropertyComponent component : settingsComponents) {
            component.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (PanelPropertyComponent component : settingsComponents) {
            component.mouseDragged(mouseX, mouseY, clickedMouseButton);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getDWheel();
        if (scroll != 0) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;

            float contentY = panelY + HEADER_HEIGHT;
            float listX = panelX + 8f;
            float listWidth = MODULE_LIST_WIDTH - 8f;
            float settingsX = panelX + MODULE_LIST_WIDTH + 8f;

            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= contentY) {
                moduleTargetScroll -= scroll * 4.5f;
                moduleTargetScroll = Math.max(0, Math.min(moduleMaxScroll, moduleTargetScroll));
            }
            else if (mouseX >= settingsX && mouseX <= panelX + PANEL_WIDTH && mouseY >= contentY) {
                settingsTargetScroll -= scroll * 4.5f;
                settingsTargetScroll = Math.max(0, Math.min(settingsMaxScroll, settingsTargetScroll));
            }
        }

    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (bindingModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                bindingModule = null;
                return;
            } else if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                bindingModule.setKeyCode(0);
                bindingModule = null;
                return;
            } else if (keyCode != Keyboard.KEY_NONE) {
                bindingModule.setKeyCode(keyCode);
                bindingModule = null;
                return;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            isClosing = true;
            return;
        }

        for (PanelPropertyComponent component : settingsComponents) {
            component.keyTyped(typedChar, keyCode);
        }

        super.keyTyped(typedChar, keyCode);
    }


    @Override
    public void onGuiClosed() {
        openAnimation = 0f;
        bindingModule = null;
        for (PanelPropertyComponent component : settingsComponents) {
            component.onClose();
        }
        super.onGuiClosed();
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.max(0, Math.min(255, (int) (color.getAlpha() * alpha))));
    }
}
