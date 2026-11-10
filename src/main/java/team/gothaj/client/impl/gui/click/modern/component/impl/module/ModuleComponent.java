package team.gothaj.client.impl.gui.click.modern.component.impl.module;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.modern.ModernDropdown;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.impl.gui.click.modern.component.impl.CategoryComponent;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.*;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModuleComponent extends Component {
    private static final IFontRenderer nameFont = Fonts.nunito18;

    private static final float HEADER_GAP = 2.0f;
    private static final float ROW_SPACING = 4.0f;

    private final Module module;
    private boolean expanded;
    private final List<Component> valueComponents;
    private final ModernDropdown clickGUI;
    private final CategoryComponent parent;
    private boolean listeningForKey;

    private float expandAnimation = 0.0f;
    private static final float EXPAND_ANIMATION_SPEED = 0.21f;

    private Component expandedProperty = null;

    public ModuleComponent(CategoryComponent parent, ModernDropdown clickGUI, Module module, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.module = module;

        List<Component> list = new ArrayList<>();
        for (Property<?> value : module.getProperties()) {
            Component c;
            if (value instanceof ModeProperty v) {
                c = new ModeComponent(v, x, y);
            } else if (value instanceof BooleanProperty v) {
                c = new BooleanComponent(v, x, y);
                c.setHeight(10f);
                c.setWidth(10f);
            } else if (value instanceof ActionProperty v) {
                c = new ActionComponent(v, x, y);
                c.setHeight(10f);
                c.setWidth(10f);
            } else if (value instanceof ColorProperty v) {
                c = new ColorPickerComponent(v, x, y);
                c.setHeight(20f);
            } else if (value instanceof NumberProperty v) {
                c = new SliderComponent(v, x, y);
                c.setHeight(20f);
                c.setWidth(width);
            } else if (value instanceof RangedNumberProperty v) {
                c = new RangedSliderComponent(v, x, y);
                c.setHeight(20f);
                c.setWidth(width);
            } else if (value instanceof ListProperty v) {
                c = new ListComponent(v, x, y);
            } else if (value instanceof InputProperty v) {
                c = new InputComponent(v, x, y);
                c.setHeight(15f);
            } else if (value instanceof FontProperty v) {
                c = new FontComponent(v, x, y);
            } else if (value instanceof PatternProperty v) {
                c = new PatternComponent(v, x, y);
            } else if (value instanceof InvProperty v) {
                c = new InvComponent(v, x, y);
            } else {
                throw new IllegalStateException("Unknown value type: " + value.getClass());
            }
            c.ownedBy(value);
            list.add(c);
        }
        this.valueComponents = list;
        this.clickGUI = clickGUI;
        updateComponentPositions();
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public void setWidth(float width) {
        super.setWidth(width);
        for (Component valueComponent : valueComponents) {
            if (valueComponent.shouldUpdateWidth())
                valueComponent.setWidth(width);
        }
    }

    @Override
    public float getHeight() {
        float base = this.height;
        if (valueComponents == null || valueComponents.isEmpty()) return base;

        List<Component> visible = new ArrayList<>();
        for (Component vc : valueComponents) {
            if (vc.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                visible.add(vc);
            }
        }
        if (visible.isEmpty()) return base;

        float rowsHeight = 0f;
        for (Component vc : visible) {
            rowsHeight += vc.getHeight();
        }
        float gaps = Math.max(0, visible.size() - 1) * ROW_SPACING;
        float extra = HEADER_GAP + 2 + rowsHeight + gaps + 2;

        return base + extra * expandAnimation;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    private void togglePropertyExpansion(Component property) {
        if (property == expandedProperty) {
            collapseProperty(property);
            expandedProperty = null;
        } else {
            if (expandedProperty != null) {
                collapseProperty(expandedProperty);
            }
            expandProperty(property);
            expandedProperty = property;
        }
        updateComponentPositions();
    }

    private void expandProperty(Component property) {
        if (property instanceof ModeComponent) {
            ((ModeComponent) property).expanded = true;
        } else if (property instanceof ListComponent) {
            ((ListComponent) property).expanded = true;
        } else if (property instanceof FontComponent) {
            ((FontComponent) property).expanded = true;
        }
    }

    private void collapseProperty(Component property) {
        if (property instanceof ModeComponent) {
            ((ModeComponent) property).expanded = false;
        } else if (property instanceof ListComponent) {
            ((ListComponent) property).expanded = false;
        } else if (property instanceof FontComponent) {
            ((FontComponent) property).expanded = false;
        }
    }

    private boolean isExpandableProperty(Component property) {
        return property instanceof ModeComponent || property instanceof FontComponent || property instanceof ListComponent;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        float expandTarget = expanded ? 1.0f : 0.0f;
        expandAnimation += (expandTarget - expandAnimation) * EXPAND_ANIMATION_SPEED;
        if (Math.abs(expandTarget - expandAnimation) < 0.01f)
            expandAnimation = expandTarget;

        if ((expanded && expandAnimation > 0.01f) || (expandAnimation > 0.01f && expandAnimation < 0.99f)) {
            updateComponentPositions();
        }

        boolean doToggledColor = module.isToggled();
        final boolean isHovering = isMouseOverName(mouseX, mouseY);
        Color color = doToggledColor ? Color.WHITE : isHovering ? new Color(205, 205, 205, 210) : new Color(205, 205, 205, 120);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Color primary = Hud.primaryColor.getProperty();
        Color primaryRGB = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 255);

        Color secondary = Hud.secondaryColor.getProperty();
        Color secondaryRGB = new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 255);

        if (module.isToggled()) {
            boolean isLast = parent.moduleComponentList.getLast() == this;
            float bottomRadius = isLast && !parent.moduleComponentList.getLast().expanded ? 6f : 0f;
            RoundedUtil.drawGradientRound(
                    x - 0.8f,
                    y,
                    width + 2.1f,
                    isLast ? height + 2f : height + 0.8f,
                    bottomRadius, bottomRadius,0, 0,
                    primaryRGB, secondaryRGB
            );
        }

        nameFont.drawString(
                module.getName() + (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && module.getKeyCode() != 0 ? " [" + Keyboard.getKeyName(module.getKeyCode()) + "]" : listeningForKey ? " [...]" : ""),
                x + 6F,
                y + (height / 2F - nameFont.getHeight() / 2f) + 1.5f,
                -1,
                true
        );

        RenderUtil.resetColor();
        GL11.glPopMatrix();

        if (!valueComponents.isEmpty() && expandAnimation > 0.05f) {
            List<Component> visible = new ArrayList<>();
            for (Component vc : valueComponents) {
                if (vc.getOwner() instanceof Property<?> p && p.getVisible().get()) visible.add(vc);
            }

            float expandedHeight = 0f;
            if (!visible.isEmpty()) {
                float rowsHeight = 0f;
                for (Component vc : visible) rowsHeight += vc.getHeight();
                float gaps = Math.max(0, visible.size() - 1) * ROW_SPACING;
                expandedHeight = HEADER_GAP + 2 + rowsHeight + gaps + 2;
            }
            float visibleHeight = expandedHeight * expandAnimation;

            ScissorStack.push(x, y + height, x + width, y + height + visibleHeight, scaledResolution);

            for (Component vc : valueComponents) {
                if (vc.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                    vc.draw(mouseX, mouseY, scaledResolution);
                }
            }

            ScissorStack.pop();
        }
    }

    private void updateComponentPositions() {
        float currentY = y + height + HEADER_GAP + 2;
        boolean first = true;

        for (Component vc : valueComponents) {
            if (vc.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                if (!first) currentY += ROW_SPACING;
                first = false;
                vc.moveTo(x, currentY);
                currentY += vc.getHeight();
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isMouseOverName(mouseX, mouseY)) {
            if (mouseButton == 0)
                module.toggle();
            else if (mouseButton == 1) {
                if (!listeningForKey && !this.valueComponents.isEmpty()) {
                    expanded = !expanded;
                    if (expanded) {
                        parent.componentExpanded(this);
                        updateComponentPositions();
                    } else {
                        parent.componentCollapsed(this);
                        expandedProperty = null;
                    }
                } else if (listeningForKey) {
                    module.setKeyCode(0);
                    listeningForKey = false;
                    clickGUI.listeningForKey = false;
                }
            } else if (mouseButton == 2) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    module.setHidden(!module.isHidden());
                    GothajClient.getInstance().getNotificationManager().add("Module", module.getName() + (module.getName().substring(module.getName().length() - 1).equalsIgnoreCase("s") ? " are" : " is") + " now " + (module.isHidden() ? "hidden" : "visible"), Notification.Type.INFO);
                } else {
                    listeningForKey = !listeningForKey;
                    clickGUI.listeningForKey = listeningForKey;
                }
            }
            return;
        }

        if (expandAnimation > 0.1f) {
            for (Component valueComponent : this.valueComponents) {
                if (valueComponent.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                    if (valueComponent.isMouseOver(mouseX, mouseY)) {
                        if (isExpandableProperty(valueComponent)) {
                            boolean isHeaderClick = false;
                            if (valueComponent instanceof ModeComponent) {
                                isHeaderClick = MouseUtil.isHovered(mouseX, mouseY, valueComponent.getX(), valueComponent.getY(),
                                        valueComponent.getX() + valueComponent.getWidth(),
                                        valueComponent.getY() + 13f);
                            } else if (valueComponent instanceof ListComponent) {
                                isHeaderClick = MouseUtil.isHovered(mouseX, mouseY, valueComponent.getX(), valueComponent.getY(),
                                        valueComponent.getX() + valueComponent.getWidth(),
                                        valueComponent.getY() + 13f);
                            } else if (valueComponent instanceof FontComponent) {
                                isHeaderClick = MouseUtil.isHovered(mouseX, mouseY, valueComponent.getX(), valueComponent.getY(),
                                        valueComponent.getX() + valueComponent.getWidth(),
                                        valueComponent.getY() + 13f);
                            }

                            if (isHeaderClick) {
                                togglePropertyExpansion(valueComponent);
                            } else {
                                valueComponent.mouseClicked(mouseX, mouseY, mouseButton);
                            }
                        } else {
                            valueComponent.mouseClicked(mouseX, mouseY, mouseButton);
                        }
                        updateComponentPositions();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (expandAnimation > 0.1f) {
            for (Component valueComponent : this.valueComponents) {
                if (valueComponent.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                    valueComponent.mouseReleased(mouseX, mouseY, state);
                }
            }
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (expandAnimation > 0.1f) {
            for (Component valueComponent : this.valueComponents) {
                if (valueComponent.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                    if (valueComponent.shouldLockDragging()) {
                        valueComponent.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                    }
                }
            }
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (listeningForKey) {
            if (keyCode != Keyboard.KEY_BACK && keyCode != 1) {
                module.setKeyCode(keyCode);
            } else {
                module.setKeyCode(0);
            }
            listeningForKey = false;
            clickGUI.listeningForKey = false;
            return;
        }
        if (expandAnimation > 0.1f) {
            for (Component valueComponent : this.valueComponents) {
                if (valueComponent.getOwner() instanceof Property<?> p && p.getVisible().get()) {
                    if (valueComponent.isAcceptingKeypresses()) {
                        valueComponent.keyTyped(key, keyCode);
                    }
                }
            }
        }
    }

    @Override
    public boolean isAcceptingKeypresses() {
        return true;
    }

    @Override
    public boolean shouldLockDragging() {
        for (Component valueComponent : this.valueComponents) {
            if (valueComponent.shouldLockDragging())
                return true;
        }
        return false;
    }

    private boolean isMouseOverName(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + height);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return isMouseInBounds(mouseX, mouseY);
    }

    @Override
    public void onGuiClosed() {
        for (Component valueComponent : this.valueComponents) {
            valueComponent.onGuiClosed();
        }
        listeningForKey = false;
        expandedProperty = null;
    }

    @Override
    public void moveTo(float x, float y) {
        super.moveTo(x, y);
        updateComponentPositions();
    }

    public void collapse() {
        this.expanded = false;
        expandedProperty = null;
    }
}
