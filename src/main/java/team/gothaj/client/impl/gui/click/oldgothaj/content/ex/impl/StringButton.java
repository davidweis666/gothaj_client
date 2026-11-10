package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import net.minecraft.client.gui.Gui;

public class StringButton extends SettingsButton {
    private Property<String> setting;
    private GuiTextField textField;
    private double animation;
    private static final int FIELD_WIDTH = 100;
    private static final int FIELD_HEIGHT = 12;

    public StringButton(Property<String> setting, PositionUtils pos) {
        this.settingMain = setting;
        this.setting = setting;
        this.position = pos;
        this.textField = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, FIELD_WIDTH, FIELD_HEIGHT);
        this.textField.setText(setting.getProperty() != null ? setting.getProperty() : "");
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        Fonts.nunito14.drawString(setting.getName(), (float) (position.getX() + 8.0), (float) (position.getY() + 1.0), -1);
        textField.xPosition = (int) (position.getX2() - 5.0 - FIELD_WIDTH);
        textField.yPosition = (int) (position.getY() + 1.0);
        Gui.drawRect(textField.xPosition, textField.yPosition,
                textField.xPosition + FIELD_WIDTH, textField.yPosition + FIELD_HEIGHT, 0xFF1A1A1A);

        if (textField.isFocused()) {
            animation = (animation * 9.0 + (FIELD_WIDTH / 2.0) - 3.0) / 10.0;
        } else {
            animation = animation * 9.0 / 10.0;
        }

        Gui.drawRect(
                (int) ((double) (textField.xPosition + FIELD_WIDTH / 2) - animation),
                textField.yPosition + FIELD_HEIGHT - 2,
                (int) ((double) (textField.xPosition + FIELD_WIDTH / 2) + animation),
                textField.yPosition + FIELD_HEIGHT - 1,
                -12424715
        );

        String render = textField.getText();
        if (render == null || render.isEmpty()) {
            render = "Add your text";
        }

        Fonts.nunito12.drawString(
                render,
                (float) (textField.xPosition + FIELD_WIDTH / 2 - Fonts.nunito12.getStringWidth(render) / 2.0),
                (float) (textField.yPosition + FIELD_HEIGHT / 2 - Fonts.nunito12.getHeight() / 2.0F),
                -5592406
        );
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        textField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void key(char character, int key) {
        if (character == '\t' && !textField.isFocused()) {
            textField.setFocused(true);
        }
        textField.textboxKeyTyped(character, key);
    }
}
