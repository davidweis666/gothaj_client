package team.gothaj.client.api.ui.button;


import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.SoundUtil;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

@Getter
@Setter
public class GothajButton implements IButton, Utils {

    private String displayText;
    private int id;

    private float xPos;
    private float yPos;
    private float width;
    private float height;

    private Runnable onClick;

    @Setter
    private static boolean useShader;

    private float scale = 1.0f;
    private float targetScale = 1.0f;

    private final int color;

    public GothajButton(String displayText, int id, Color color) {
        this.displayText = displayText;
        this.id = id;
        this.color = color.getRGB();
    }

    public GothajButton(String displayText, int id) {
        this(displayText, id, new Color(156, 156, 156));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        boolean hovered = mouseX >= this.xPos && mouseY >= this.yPos &&
                mouseX < this.xPos + this.width && mouseY < this.yPos + this.height;

        targetScale = hovered ? 1.07f : 1.0f;
        scale += (targetScale - scale) * 0.2f;

        Fonts.nunito14.drawCenteredString(this.displayText, (int) (this.xPos + this.width / 2), (int) (this.yPos + (this.height - 6) / 2), hovered ? new Color(216, 216, 216, 255).getRGB() : color);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean hovered = mouseX >= this.xPos && mouseY >= this.yPos && mouseX < this.xPos + this.width && mouseY < this.yPos + this.height;

        if (hovered) {
            onClick.run();
            SoundUtil.play(new ResourceLocation("gui.button.press"), 1.0F);
        }
    }
}
