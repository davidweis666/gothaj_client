package de.florianmichael.viamcp.gui;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

import java.util.List;

public class AsyncVersionSlider extends GuiButton {
    private float dragValue;
    private final List<ProtocolVersion> values;
    private float sliderValue;
    public boolean dragging;

    public AsyncVersionSlider(int buttonId, int x, int y, int widthIn, int heightIn) {
        super(buttonId, x, y, Math.max(widthIn, 110), heightIn, "");
        this.values = ViaLoadingBase.PROTOCOLS;
        if (!values.isEmpty()) {
            this.dragValue = (float) values.indexOf(ViaLoadingBase.getInstance().getTargetVersion()) / (values.size() - 1);
        }
        this.sliderValue = dragValue;
        this.displayString = values.isEmpty() ? "N/A" : values.get(getIndex()).getName();
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        super.drawButton(mc, mouseX, mouseY);
    }

    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.dragging) {
                this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
                this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
                this.dragValue = sliderValue;

                int idx = getIndex();
                this.displayString = values.get(idx).getName();
                ViaLoadingBase.getInstance().reload(values.get(idx));
            }

            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)), this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
        }
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
            this.dragValue = sliderValue;

            int idx = getIndex();
            this.displayString = values.get(idx).getName();
            ViaLoadingBase.getInstance().reload(values.get(idx));
            this.dragging = true;
            return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    public void setVersion(int protocol) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getVersion() == protocol) {
                this.dragValue = (float) i / (values.size() - 1);
                this.sliderValue = this.dragValue;
                this.displayString = values.get(i).getName();
                return;
            }
        }
    }

    private int getIndex() {
        if (values.isEmpty()) return 0;
        return Math.max(0, Math.min(values.size() - 1, (int) Math.round(this.sliderValue * (values.size() - 1))));
    }
}
