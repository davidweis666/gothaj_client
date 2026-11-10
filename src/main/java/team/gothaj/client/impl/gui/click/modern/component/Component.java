package team.gothaj.client.impl.gui.click.modern.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.impl.module.visual.ClickGUI;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;

public abstract class Component implements Utils {

    public String identifier = "unknown";

    protected float x;
    protected float y;

    @Setter
    protected float width;

    @Setter
    protected float height;

    public boolean shown;

    @Getter
    protected Object owner;
    
    public Component(float x, float y) {
        this.x = x;
        this.y = y;

    }

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract float getX();

    public abstract float getY();

    public abstract void draw(int mouseX, int mouseY, ScaledResolution scaledResolution);

    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);

    public abstract void mouseReleased(int mouseX, int mouseY, int state);

    public abstract void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick);

    public boolean isMouseOver(int mouseX, int mouseY) {
        return isMouseInBounds(mouseX, mouseY);
    }
    public boolean isAcceptingKeypresses() {
        return false;
    }

    public boolean shouldLockDragging() {
        return false;
    }

    public void mouseScrolled(int mouseX, int mouseY, int dWheel) {

    }


    public void ownedBy(Object owner) {
        this.owner = owner;
    }

    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void addToPosition(float addX, float addY) {
        this.x += addX;
        this.y += addY;
    }

    public void keyTyped(char key, int keyCode) {}

    public Component setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    protected boolean isMouseInBounds(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + getHeight());
    }

    public void onGuiClosed() {

    }

    public Color getPrimaryColor() {
        ClickGUI clickGUI = GothajClient.getInstance().getModuleManager().getAndCast(ClickGUI.class);
        return Hud.primaryColor.getProperty();
    }

    public Color getSecondaryColor() {
        ClickGUI clickGUI = GothajClient.getInstance().getModuleManager().getAndCast(ClickGUI.class);
        return Hud.secondaryColor.getProperty();
    }

    public boolean shouldUpdateWidth() {
        return false;
    }
}
