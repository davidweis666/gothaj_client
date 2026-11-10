package team.gothaj.client.impl.gui.proxy.impl;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import team.gothaj.client.api.proxy.Proxy;
import team.gothaj.client.api.proxy.ProxyRepository;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class AddProxyMenu extends GuiScreen {

    private GuiTextField domain;
    private GuiTextField port;
    private GuiTextField username;
    private GuiTextField password;
    private final GuiScreen last;

    public AddProxyMenu(GuiScreen last) {
        this.last = last;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            close();
        }
        domain.textboxKeyTyped(typedChar, keyCode);
        port.textboxKeyTyped(typedChar, keyCode);
        username.textboxKeyTyped(typedChar, keyCode);
        password.textboxKeyTyped(typedChar, keyCode);
    }

    public void close() {
        this.mc.displayGuiScreen(last);

        if (this.mc.currentScreen == null)
        {
            this.mc.setIngameFocus();
        }
    }


    @Override
    public void initGui() {

        domain = new GuiTextField(1, mc.fontRendererObj, width / 2 - 160 / 2, (int) (height / 2 - 70), 160, 24);
        port = new GuiTextField(1, mc.fontRendererObj, width / 2 - 160 / 2, domain.yPosition + 30, 160, 24);
        username = new GuiTextField(1, mc.fontRendererObj, width / 2 - 160 / 2, port.yPosition + 30, 160, 24);
        password = new GuiTextField(1, mc.fontRendererObj, width / 2 - 160 / 2, username.yPosition + 30, 160, 24);

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(1337, width / 2 - 60, password.yPosition + 32, 120, 24, "Add Proxy"));

        this.buttonList.add(new GuiButton(2, width - 30, 10, 20, 20, "X"));


        super.initGui();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        domain.mouseClicked(mouseX, mouseY, mouseButton);
        port.mouseClicked(mouseX, mouseY, mouseButton);
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground();


        Fonts.surf20.drawCenteredStringWithShadow("PROXY LOGIN", width / 2f, height / 2f - Fonts.surf20.getHeight() - 78, -1);

        domain.setPlaceholder("Domain");
        port.setPlaceholder("Port");
        username.setPlaceholder("Username");
        password.setPlaceholder("Password");

        domain.drawTextBox();
        port.drawTextBox();
        username.drawTextBox();
        password.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1337) {
            GothajClient.getInstance().getProxyRepository().add(
                new Proxy(domain.getText(), port.getText(), username.getText(), password.getText(), ProxyRepository.Type.SOCKS5));

            close();

        } else if (button.id == 2) {
            close();
        }
        super.actionPerformed(button);
    }
}
