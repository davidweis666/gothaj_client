package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

import static net.minecraft.util.EnumChatFormatting.GRAY;

public class InputComponent extends Component {

    private final InputProperty parent;
    private boolean acceptingKeypresses, animateForwards;
    private float typingAlpha;

    private static final IFontRenderer font = Fonts.nunito18;

    public InputComponent(InputProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return 30;
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

        Keyboard.enableRepeatEvents(true);
        typingAlpha = RenderUtil.animate(animateForwards ? 0.9F : 0F, typingAlpha, 0.03F);

        if(typingAlpha >= 0.88F && animateForwards)
            animateForwards = false;
        else if(typingAlpha <= 0.04F && !animateForwards)
            animateForwards = true;

        float yPos = this.y + 2;

        font.drawString(GRAY + parent.getName(), x + 5, yPos, -1, true);

        yPos += 4.5F + font.getHeight();

        String toDraw = parent.getProperty();
        while (font.getStringWidth(toDraw + "_") > width - 10) {
            toDraw = toDraw.substring(1);
        }

        if (parent.isPassword()) {
            font.drawString(toDraw.replaceAll("(?s).", "*"), x + 5.5f, yPos, -1, true);
        } else {
            font.drawString(toDraw, x + 5.5f, yPos, -1, true);
        }

        if(acceptingKeypresses) {
            font.drawString("_", x + 5 + font.getStringWidth(toDraw), yPos,
                    new Color(1, 1, 1, typingAlpha).getRGB(), true);
        }

        float lineY = yPos + 1 + font.getHeight();
        RenderUtil.rect(x + 5, lineY, x + width - 5, lineY + 0.5F, -1);

        GL11.glPopMatrix();
    }

    public String getText() {
        return parent.getProperty();
    }


    @Override
    public void keyTyped(char key, int keyCode) {
        if (!acceptingKeypresses) return;

        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA)
                || Keyboard.isKeyDown(Keyboard.KEY_RMETA);

        if (keyCode == Keyboard.KEY_V && ctrlDown) {
            try {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String clip = (String) cb.getData(DataFlavor.stringFlavor);
                    if (clip != null && !clip.isEmpty()) {
                        parent.setProperty(parent.getProperty() + clip);
                    }
                }
            } catch (Exception ignore) {}
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            String cur = parent.getProperty();
            if (!cur.isEmpty()) parent.setProperty(cur.substring(0, Math.max(0, cur.length() - 1)));
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            acceptingKeypresses = false;
            return;
        }

        if (!Character.isISOControl(key)) {
            parent.setProperty(parent.getProperty() + key);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float yPos = this.y + 2;
        yPos += 4.5F + font.getHeight();

        float inputHeight = font.getHeight() + 1 + 0.5F;

        acceptingKeypresses = MouseUtil.isHovered(mouseX, mouseY, x + 3, yPos - 2, x + width - 3, yPos + inputHeight);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + getHeight());
    }

    @Override
    public boolean isAcceptingKeypresses() {
        return acceptingKeypresses;
    }

    @Override
    public boolean shouldUpdateWidth() {
        return true;
    }

    @Override
    public void onGuiClosed() {
        this.acceptingKeypresses = false;
        Keyboard.enableRepeatEvents(false);
    }
}
