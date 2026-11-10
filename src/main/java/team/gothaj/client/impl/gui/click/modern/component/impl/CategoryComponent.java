package team.gothaj.client.impl.gui.click.modern.component.impl;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.click.modern.ModernDropdown;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.ModuleComponent;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;
import team.gothaj.client.impl.module.visual.ClickGUI;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Boolean.compare;
import static java.util.stream.Collectors.toList;
import static team.gothaj.client.impl.gui.click.modern.ModernDropdown.TITLE_RECT_HEIGHT;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class CategoryComponent extends Component {

    private static final IFontRenderer title = Fonts.nunito21;

    public final List<ModuleComponent> moduleComponentList;
    private final ModernDropdown modernGUI;
    private ModuleComponent expandedComponent;

    private static final float ANIMATION_SPEED = 0.2f;
    private float animX, animY, targetX, targetY;
    private boolean isDragging, setPrevXY, shown = true;

    private float scrollOffset = 0f;
    private float maxScroll = 0f;
    private float viewportHeight = 0f;

    private static final float SCROLL_STEP = 14f;

    private float prevX, prevY;

    public CategoryComponent(ModernDropdown modernGUI, List<Module> moduleList, float x, float y) {
        super(x, y);

        final AtomicReference<Float> height = new AtomicReference<>(y + TITLE_RECT_HEIGHT);

        this.moduleComponentList = moduleList.stream().map(module -> {
            final ModuleComponent component = new ModuleComponent(this, modernGUI, module, x, TITLE_RECT_HEIGHT + 1F);
            component.ownedBy(module);
            component.setWidth(100);
            component.setHeight(15);
            height.set(height.get() + component.getHeight());
            return component;
        }).sorted((before, after) -> -compare(before.getOwner() instanceof Hud, after.getOwner() instanceof Hud)).collect(toList());

        this.width = 100;
        this.height = height.get();

        this.animX = x;
        this.animY = y;
        this.targetX = x;
        this.targetY = y;

        this.modernGUI = modernGUI;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        float modulesHeight = 0f;
        for (ModuleComponent mc : moduleComponentList) {
            if (!isVisible(mc)) continue;
            modulesHeight += mc.getHeight();
        }
        return TITLE_RECT_HEIGHT + modulesHeight;
    }

    @Override
    public float getX() {
        return animX;
    }

    @Override
    public float getY() {
        return animY;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr) {
        GL11.glPushMatrix();

        if (isDragging) {
            targetX = mouseX - prevX;
            targetY = mouseY - prevY;
        }
        animX += (targetX - animX) * ANIMATION_SPEED;
        animY += (targetY - animY) * ANIMATION_SPEED;

        width = 100;
        float totalHeight = 17;
        for (ModuleComponent mc : moduleComponentList) {
            if (!isVisible(mc)) continue;
            totalHeight += mc.getHeight();
            mc.setWidth(width);
        }
        height = totalHeight;

        Blur.startBlur();
        RoundedUtil.drawRound(animX + 0.7f, animY + 0.2F, width - 0.4F, height - 2.6f, 6.2f, 6.2f, 6.2f, 6.2f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawRound(animX + 0.7f, animY + 0.2F, width - 0.4F, height - 2.6f, 6f, 6f, 6f, 6f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 2, Color.BLACK.getRGB());

        RenderUtil.resetColor();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RoundedUtil.drawRound(animX + 0.5f, animY, width, TITLE_RECT_HEIGHT, 6f, 6f, 0, 0, new Color(10, 10, 10, 120));
        RoundedUtil.drawRound(animX + 0.5f, animY + TITLE_RECT_HEIGHT + 1.4f, width, height - TITLE_RECT_HEIGHT - 3f, 0f, 0f, 6f, 6f,  new Color(10, 10, 10, 150));

        RenderUtil.resetColor();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        String letter = identifier.equalsIgnoreCase("Client") ? "a"
                : identifier.equalsIgnoreCase("Combat") ? "b"
                : identifier.equalsIgnoreCase("Misc") ? "c"
                : identifier.equalsIgnoreCase("Movement") ? "d"
                : identifier.equalsIgnoreCase("Player") ? "e"
                : identifier.equalsIgnoreCase("Visual") ? "f"
                : identifier.equalsIgnoreCase("Ghost") ? "h"
                : "g";

        Fonts.catFont16.drawStringWithShadow(letter, animX + width - 14F, animY + 7, -1);

        Fonts.nunito18.drawStringWithShadow(identifier, animX + 6F, animY + 4.5F, -1);

        RenderUtil.resetColor();

        float contentHeight = 0f;
        for (ModuleComponent mc : moduleComponentList) {
            if (isVisible(mc)) contentHeight += mc.getHeight();
        }

        float availableBelow = sr.getScaledHeight() - (animY + TITLE_RECT_HEIGHT) - 4f;
        viewportHeight = Math.max(0f, Math.min(contentHeight, availableBelow));

        maxScroll = Math.max(0f, contentHeight - viewportHeight);
        if (scrollOffset < 0f) scrollOffset = 0f;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        float listX0 = animX - 1.5f;
        float listY0 = animY + TITLE_RECT_HEIGHT;
        float listX1 = animX + width + 3;
        float listY1 = listY0 + viewportHeight + 2f;

        if (shown && viewportHeight > 0f) {
            ScissorStack.push(listX0, listY0, listX1, listY1, sr);

            float moduleY = listY0 - scrollOffset;
            for (ModuleComponent component : moduleComponentList) {
                if (!isVisible(component)) continue;
                component.moveTo(animX, moduleY);
                component.draw(mouseX, mouseY, sr);
                moduleY += component.getHeight();
            }

            ScissorStack.pop();
        }

        x = animX;
        y = animY;

        RenderUtil.resetColor();

        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseX >= animX && mouseX <= animX + width && mouseY >= animY && mouseY <= animY + TITLE_RECT_HEIGHT) {
            if (mouseButton == 0) {
                isDragging = true;
                prevX = mouseX - animX;
                prevY = mouseY - animY;
                setPrevXY = true;
            }
            return;
        }

        if (this.shown && viewportContains(mouseX, mouseY)) {
            for (ModuleComponent moduleComponent : this.moduleComponentList) {
                if (isVisible(moduleComponent) && moduleComponent.isMouseOver(mouseX, mouseY)) {
                    moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
                    break;
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        isDragging = false;
        setPrevXY = false;
        if (this.shown && viewportContains(mouseX, mouseY)) {
            for (ModuleComponent moduleComponent : this.moduleComponentList) {
                if (isVisible(moduleComponent)) {
                    moduleComponent.mouseReleased(mouseX, mouseY, state);
                }
            }
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.shown && viewportContains(mouseX, mouseY)) {
            for (ModuleComponent moduleComponent : this.moduleComponentList) {
                if (isVisible(moduleComponent) && moduleComponent.shouldLockDragging()) {
                    moduleComponent.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                }
            }
        }
    }

    @Override
    public void mouseScrolled(int mouseX, int mouseY, int dWheel) {
        if (!shown || isDragging) return;

        boolean overHeader = mouseX >= animX && mouseX <= animX + width && mouseY >= animY && mouseY <= animY + TITLE_RECT_HEIGHT;
        boolean overViewport = viewportContains(mouseX, mouseY);

        if (!(overViewport || overHeader)) return;

        float step = -(dWheel) * SCROLL_STEP;
        if (step != 0f) {
            scrollOffset = Math.max(0f, Math.min(maxScroll, scrollOffset + step));
        }
    }

    private boolean viewportContains(int mouseX, int mouseY) {
        float y0 = animY + TITLE_RECT_HEIGHT;
        float y1 = y0 + viewportHeight;
        return mouseX >= animX && mouseX <= animX + width && mouseY >= y0 && mouseY <= y1;
    }

    public void updateSize() {
        final AtomicReference<Float> height = new AtomicReference<>(animY + TITLE_RECT_HEIGHT);

        this.moduleComponentList.forEach(module ->
                height.set(height.get() + module.getHeight())
        );

        this.height = height.get();
    }

    @Override
    public void moveTo(float x, float y) {
        this.targetX = x;
        this.targetY = y;
    }

    @Override
    public boolean shouldLockDragging() {
        return isDragging;
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return isMouseInBounds(mouseX, mouseY) || isDragging;
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if(this.shown) {
            for (ModuleComponent component : this.moduleComponentList) {
                if (isVisible(component) && component.isAcceptingKeypresses()) {
                    component.keyTyped(key, keyCode);
                }
            }
        }
    }

    private boolean isVisible(ModuleComponent component) {
        return !modernGUI.isSearchActive() || modernGUI.isModuleVisible((Module) component.getOwner());
    }

    @Override
    public boolean isAcceptingKeypresses() {
        return true;
    }

    @Override
    public void onGuiClosed() {
        for (ModuleComponent component : moduleComponentList) {
            component.onGuiClosed();
        }
    }

    public void componentExpanded(ModuleComponent expandedComponent) {
        if(this.expandedComponent != null && GothajClient.getInstance().getModuleManager().getAndCast(ClickGUI.class).closeSetting.getProperty())
            this.expandedComponent.collapse();

        this.expandedComponent = expandedComponent;
    }

    public void componentCollapsed(ModuleComponent collapsedComponent) {
        if(this.expandedComponent == collapsedComponent)
            this.expandedComponent = null;
    }
}
