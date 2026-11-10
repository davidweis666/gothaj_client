package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;

public abstract class PanelPropertyComponent {

    protected float x, y, width;

    public abstract float getHeight();

    public abstract void draw(int mouseX, int mouseY, ScaledResolution sr, float alpha);

    public abstract boolean isVisible();

    public void setPosition(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {}

    public void mouseReleased(int mouseX, int mouseY, int button) {}

    public void mouseDragged(int mouseX, int mouseY, int button) {}

    public void keyTyped(char typedChar, int keyCode) {}

    public void onClose() {}

    // Utility methods
    protected Color getPrimaryColor() {
        return Hud.primaryColor.getProperty();
    }

    protected Color getSecondaryColor() {
        return Hud.secondaryColor.getProperty();
    }

    protected Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.max(0, Math.min(255, (int) (color.getAlpha() * alpha))));
    }

    protected Color interpolateColor(Color c1, Color c2, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * factor);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * factor);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * factor);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * factor);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b)),
            Math.max(0, Math.min(255, a))
        );
    }

    protected boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
    }
}

