package team.gothaj.client.impl.module.visual.widget;

import java.awt.Color;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.gui.widget.WidgetScreen;

public class InventoryDisplayWidget extends Widget {

    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int MAIN_INVENTORY_START = 9;
    private static final int HEADER_HEIGHT = 14;
    private static final int PADDING = 5;
    private static final int DEFAULT_BOTTOM_OFFSET = 65;

    private boolean reported = false;

    public InventoryDisplayWidget() {
        super("Inventory Display", Integer.MIN_VALUE, Integer.MIN_VALUE,
                COLUMNS * SLOT_SIZE + PADDING * 2,
                HEADER_HEIGHT + PADDING + ROWS * SLOT_SIZE + PADDING);
    }

    @Override
    public void onRender(ScaledResolution sr) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat)
                && !(mc.currentScreen instanceof WidgetScreen)) return;

        try {
            draw(sr);
        } catch (Exception e) {
            if (!reported) {
                reported = true;
                Logger.addChat(new ChatComponentText(
                        "§4[InvDisplay] §cERROR: " + e));
            }
        }
    }

    private void draw(ScaledResolution sr) {
        if (!reported) {
            reported = true;
            Logger.addChat(new ChatComponentText(
                    "§a[InvDisplay] §frendering ok, option="
                            + (hud != null ? hud.widgets.getOptionState(getLabel()) : "hud-null") + ", x=" + x + ", y=" + y));
        }

        if (x < -100000 || y < -100000 || x + width < 10 || y + height < 10) {
            x = (sr.getScaledWidth() - width) / 2f;
            y = sr.getScaledHeight() - DEFAULT_BOTTOM_OFFSET - height;
        }

        float containerX = x;
        float containerY = y;
        float containerW = width;
        float containerH = height;

        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        RoundedUtil.drawRound(containerX - 0.5f, containerY - 0.5f, containerW + 1, containerH + 1, 6f,
                new Color(255, 255, 255, 20));
        RoundedUtil.drawRound(containerX, containerY, containerW, containerH, 5f,
                new Color(0, 0, 0, 110));

        int itemCount = 0;
        for (int i = MAIN_INVENTORY_START; i < MAIN_INVENTORY_START + COLUMNS * ROWS; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] != null) itemCount++;
        }

        Fonts.nunito14.drawStringWithShadow("Inventory", containerX + 7, containerY + 5,
                new Color(255, 255, 255, 200));
        String count = itemCount + "/" + (COLUMNS * ROWS);
        Fonts.nunito14.drawStringWithShadow(count,
                containerX + containerW - 7 - Fonts.nunito14.getStringWidth(count),
                containerY + 5, new Color(160, 160, 160, 180));

        float gridX = containerX + PADDING;
        float gridY = containerY + HEADER_HEIGHT + PADDING;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                float slotX = gridX + col * SLOT_SIZE + 1;
                float slotY = gridY + row * SLOT_SIZE + 1;

                boolean hovered = mouseX >= slotX && mouseX <= slotX + SLOT_SIZE - 2
                        && mouseY >= slotY && mouseY <= slotY + SLOT_SIZE - 2;
                RoundedUtil.drawRound(slotX, slotY, SLOT_SIZE - 2, SLOT_SIZE - 2, 2f,
                        new Color(255, 255, 255, hovered ? 50 : 12));
            }
        }

        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().zLevel = 100.0F;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slot = MAIN_INVENTORY_START + row * COLUMNS + col;
                ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
                if (stack == null) continue;

                int itemX = (int) (gridX + col * SLOT_SIZE) + 2;
                int itemY = (int) (gridY + row * SLOT_SIZE) + 2;

                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, itemX, itemY);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, itemX, itemY, null);
            }
        }

        mc.getRenderItem().zLevel = 0.0F;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
    }
}
