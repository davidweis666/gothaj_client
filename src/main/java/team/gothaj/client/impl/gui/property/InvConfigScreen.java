package team.gothaj.client.impl.gui.property;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.InvProperty;
import team.gothaj.client.api.property.properties.inv.ItemType;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;

public class InvConfigScreen extends GuiScreen {

    private final InvProperty property;
    private final GuiScreen parentScreen;
    private static final IFontRenderer font = Fonts.nunito18;

    private float centerX, centerY;
    private static final int SLOT_SIZE = 34;
    private static final int SPACING = 6;

    private boolean dragging;
    private ItemType draggedItem;
    
    public InvConfigScreen(InvProperty property, GuiScreen parentScreen) {
        this.property = property;
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(mc);
        this.centerX = sr.getScaledWidth() / 2f;
        this.centerY = sr.getScaledHeight() / 2f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        drawDefaultBackground();

        String title = "Configure InvManager";
        font.drawString(title, centerX - font.getStringWidth(title) / 2, centerY - 100, -1, true);
        font.drawString("Drag items to the hotbar slots below", centerX - font.getStringWidth("Drag items to the hotbar slots below") / 2, centerY - 85, Color.GRAY.getRGB(), true);

        float hotbarWidth = (9 * SLOT_SIZE) + (8 * SPACING);
        float startX = centerX - (hotbarWidth / 2);
        float hotbarY = centerY + 20;

        for (int i = 0; i < 9; i++) {
            float slotX = startX + i * (SLOT_SIZE + SPACING);

            boolean hovered = MouseUtil.isHovered(mouseX, mouseY, slotX, hotbarY, slotX + SLOT_SIZE, hotbarY + SLOT_SIZE);

            RoundedUtil.drawRound(slotX, hotbarY, SLOT_SIZE, SLOT_SIZE, 16, hovered ? new Color(40,40,40, 140) : new Color(20,20,20, 140));

            font.drawCenteredStringWithShadow(String.valueOf(i + 1), slotX + SLOT_SIZE / 2f, hotbarY + 48, Color.GRAY.getRGB());

            for (Map.Entry<ItemType, Integer> entry : property.getProperty().entrySet()) {
                if (entry.getValue() == i) {
                    ItemType type = entry.getKey();
                    if (dragging && draggedItem == type) continue;
                    drawItemType(type, slotX + SLOT_SIZE / 2f, hotbarY + SLOT_SIZE / 2f);
                }
            }
        }

        float poolY = centerY - 50;

        float currentX = centerX - 150;
        for (ItemType type : ItemType.values()) {
            int slot = property.getSlot(type);
            boolean isAssigned = slot >= 0 && slot <= 8;

            if (!isAssigned) {
                if (dragging && draggedItem == type) continue;

                drawItemType(type, currentX, poolY);

                currentX += 35;
            }
        }

        if (dragging && draggedItem != null) {
            drawItemType(draggedItem, mouseX, mouseY);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawItemType(ItemType type, float x, float y) {
        GL11.glPushMatrix();

        GL11.glTranslatef(x, y, 0);

        GL11.glScalef(1.4f, 1.4f, 1.0f);

        GL11.glTranslatef(-8f, -8f, 0f);

        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemIntoGUI(type.getItemStack(), 0, 0);
        RenderHelper.disableStandardItemLighting();

        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {

        if (mouseButton != 0) return;

        float hotbarWidth = (9 * SLOT_SIZE) + (8 * SPACING);
        float startX = centerX - (hotbarWidth / 2);
        float hotbarY = centerY + 20;

        for (int i = 0; i < 9; i++) {
            float slotX = startX + i * (SLOT_SIZE + SPACING);
            if (MouseUtil.isHovered(mouseX, mouseY, slotX, hotbarY, slotX + SLOT_SIZE, hotbarY + SLOT_SIZE)) {
                for (Map.Entry<ItemType, Integer> entry : property.getProperty().entrySet()) {
                    if (entry.getValue() == i) {
                        dragging = true;
                        draggedItem = entry.getKey();
                        return;
                    }
                }
            }
        }

        float poolY = centerY - 50;
        float currentX = centerX - 150;
        
        for (ItemType type : ItemType.values()) {
            int slot = property.getSlot(type);
            boolean isAssigned = slot >= 0 && slot <= 8;

            if (!isAssigned) {
                if (MouseUtil.isHovered(mouseX, mouseY, currentX - 30, poolY - 20, currentX + 30, poolY + 40)) {
                    dragging = true;
                    draggedItem = type;
                    return;
                }
                currentX += 35;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
    
        if (!dragging || draggedItem == null) return;

        boolean droppedInSlot = false;

        float hotbarWidth = (9 * SLOT_SIZE) + (8 * SPACING);
        float startX = centerX - (hotbarWidth / 2);
        float hotbarY = centerY + 20;

        for (int i = 0; i < 9; i++) {
            float slotX = startX + i * (SLOT_SIZE + SPACING);
            if (MouseUtil.isHovered(mouseX, mouseY, slotX, hotbarY, slotX + SLOT_SIZE, hotbarY + SLOT_SIZE)) {
                for (Map.Entry<ItemType, Integer> entry : property.getProperty().entrySet()) {
                    if (entry.getValue() == i && entry.getKey() != draggedItem) {
                        property.setSlot(entry.getKey(), -1); 
                    }
                }

                property.setSlot(draggedItem, i);
                droppedInSlot = true;
                break;
            }
        }

        if (!droppedInSlot) {
            property.setSlot(draggedItem, -1);
        }

        dragging = false;
        draggedItem = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
