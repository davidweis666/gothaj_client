package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ActionProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;

import java.awt.*;

/**
 * @author vajzii
 * @since 2025
 */
public class ActionComponent extends Component {

    private final ActionProperty parent;

    public ActionComponent(ActionProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.identifier = parent.getName();
    }

    @Override
    public float getWidth() {
        return 100;
    }

    @Override
    public float getHeight() {
        return 15;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        float xPos = getX() + 5;
        float nWidth = getWidth() - 10;

        RenderUtil.drawRect(xPos, getY() - 3, nWidth, getHeight(), new Color(20, 20, 20, 60));

        RenderUtil.drawGradientRectHorizontal(xPos, getY() - 3 + getHeight(), nWidth, 1, getPrimaryColor().getRGB(), getSecondaryColor().getRGB());

        Fonts.nunito17.drawCenteredStringWithShadow(parent.getName(), xPos + nWidth / 2f, getY() - 2.5f + Fonts.nunito17.getMiddleOfBox(getHeight()), -1);

        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        parent.runAction();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }
}
