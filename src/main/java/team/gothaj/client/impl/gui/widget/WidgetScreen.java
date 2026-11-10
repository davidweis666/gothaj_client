package team.gothaj.client.impl.gui.widget;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;

public class WidgetScreen extends GuiScreen {

    private boolean dragging;
    private Widget selectedWidget;
    private float lastPosX, lastPosY;
    private static final int MIN_VISIBLE = 1;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        var sr = new ScaledResolution(mc);

        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 70).getRGB());

        Fonts.nunito20.drawCenteredStringWithShadow(
                "Widget Manager",
                sr.getScaledWidth() / 2f,
                3,
                -1
        );

        GothajClient.getInstance().getWidgetManager().getItems().forEach(hudComp -> {

            boolean uninitialized = hudComp.getX() < -100000 || hudComp.getY() < -100000;

            hudComp.onRender(sr);

            if (!uninitialized) {
                Fonts.nunito16.drawStringWithShadow(
                        hudComp.getLabel(),
                        (float) (hudComp.getX() + hudComp.getWidth() / 2 - (double) Fonts.nunito16.getStringWidth(hudComp.getLabel()) / 2),
                        (float) (hudComp.getY() - 11),
                        -1
                );
            }

            if (dragging && hudComp == selectedWidget) {
                hudComp.setX(mouseX + lastPosX);
                hudComp.setY(mouseY + lastPosY);
                hudComp.onDrag();
                if (!uninitialized) clampPosition(hudComp, sr);
            }

            if (!uninitialized) {
                snapToWidget(hudComp, selectedWidget);
                clampPosition(hudComp, sr);
            }
        });

        snapToGuideLines(sr);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0 && dragging) {
            if (selectedWidget != null) {
                selectedWidget.setDragging(false);
            }
            dragging = false;
            selectedWidget = null;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(hudComp -> {
            if (mouseWithinBounds(mouseX, mouseY, hudComp.getX(), hudComp.getY(), hudComp.getWidth(), hudComp.getHeight())) {
                if (mouseButton == 0) {
                    dragging = true;
                    selectedWidget = hudComp;
                    lastPosX = hudComp.getX() - mouseX;
                    lastPosY = hudComp.getY() - mouseY;
                }
            }
        });
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (selectedWidget != null) {
            selectedWidget.setDragging(false);
        }
        dragging = false;
        selectedWidget = null;
    }

    private void clampPosition(Widget widget, ScaledResolution sr) {
        float maxX = sr.getScaledWidth() - MIN_VISIBLE;
        float maxY = sr.getScaledHeight() - MIN_VISIBLE;

        if (widget.getX() + widget.getWidth() < MIN_VISIBLE)
            widget.setX(MIN_VISIBLE - widget.getWidth());
        else if (widget.getX() > maxX)
            widget.setX(maxX);

        if (widget.getY() + widget.getHeight() < MIN_VISIBLE)
            widget.setY(MIN_VISIBLE - widget.getHeight());
        else if (widget.getY() > maxY)
            widget.setY(maxY);
    }

    private void snapToWidget(Widget widget, Widget dragging) {
        if (dragging == null || dragging == widget) return;

        final float[] xs = {
                widget.getX() - dragging.getX(),
                widget.getX() + widget.getWidth() - dragging.getX() - dragging.getWidth(),
                widget.getX() + widget.getWidth() - dragging.getX(),
                widget.getX() - (dragging.getX() + dragging.getWidth())
        };

        final float[] ys = {
                widget.getY() - dragging.getY(),
                widget.getY() + widget.getHeight() - dragging.getY() - dragging.getHeight(),
                widget.getY() + widget.getHeight() - dragging.getY(),
                widget.getY() - (dragging.getY() + dragging.getHeight())
        };

        for (float xDiff : xs) {
            if (Math.abs(xDiff) <= 2) {
                dragging.setX(dragging.getX() + xDiff);
                break;
            }
        }

        for (float yDiff : ys) {
            if (Math.abs(yDiff) <= 2) {
                dragging.setY(dragging.getY() + yDiff);
                break;
            }
        }
    }

    private void snapToGuideLines(ScaledResolution sr) {
        if (selectedWidget == null) return;

        final float centerX = sr.getScaledWidth() / 2.0f;
        final float centerY = sr.getScaledHeight() / 2.0f;

        final float[] xEdges = {
                selectedWidget.getX(),
                selectedWidget.getX() + selectedWidget.getWidth(),
                selectedWidget.getX() + selectedWidget.getWidth() / 2.0f
        };

        final float[] yEdges = {
                selectedWidget.getY(),
                selectedWidget.getY() + selectedWidget.getHeight(),
                selectedWidget.getY() + selectedWidget.getHeight() / 2.0f
        };

        for (float x : xEdges) {
            if (checkBounds(x, centerX)) {
                selectedWidget.setX(centerX - (x == xEdges[2] ? selectedWidget.getWidth() / 2.0f : (x == xEdges[1] ? selectedWidget.getWidth() : 0)));
                break;
            }
        }

        for (float y : yEdges) {
            if (checkBounds(y, centerY)) {
                selectedWidget.setY(centerY - (y == yEdges[2] ? selectedWidget.getHeight() / 2.0f : (y == yEdges[1] ? selectedWidget.getHeight() : 0)));
                break;
            }
        }
    }

    private boolean checkBounds(float f1, float f2) {
        return Math.abs(f1 - f2) <= 2;
    }

    private boolean mouseWithinBounds(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
