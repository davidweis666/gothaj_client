package team.gothaj.client.impl.gui.security;

import net.minecraft.client.gui.*;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.security.LoginThread;
import team.gothaj.client.api.util.other.security.RememberedCredentials;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.security.field.GuiPasswordConsoleField;
import team.gothaj.client.impl.gui.security.field.GuiTextConsoleField;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class ConsoleLogin extends GuiScreen implements GuiYesNoCallback {


    private GuiTextConsoleField username;
    private GuiPasswordConsoleField password;

    private boolean showPassword, showValidate, canLog;
    private boolean userTextCaptured = false;
    private long invalidUsernameTime = 0;
    public static boolean loginQueued = false;
    private boolean blurEnabled = true;
    private boolean rememberCredentials = RememberedCredentials.exists();
    private boolean attemptedRememberedLogin;

    private final File rootDir = new File(GothajClient.getInstance().getClientDir().toURI());
    private LoginThread thread;

    private String userText;

    @Override
    public void initGui() {


        (this.username = new GuiTextConsoleField(0, this.fontRendererObj, (int) (this.width / 4f - 26.7f + Fonts.ubuntu19.getStringWidth("username@gothaj-client:~$") + 2), (int) (this.height / 4f + 24.2f), 200, 20)).setMaxStringLength(Integer.MAX_VALUE);
        username.setFocused(true);
        this.password = new GuiPasswordConsoleField(this.fontRendererObj, (int) (this.width / 4f - 26.7f + Fonts.ubuntu19.getStringWidth("password@gothaj-client:~$") + 2), (int) (this.height / 4f + 47f), 200, 20);
        super.initGui();

        if (!attemptedRememberedLogin && RememberedCredentials.exists() && !loginQueued) {
            RememberedCredentials.Credentials credentials = RememberedCredentials.load();
            if (credentials != null) {
                attemptedRememberedLogin = true;
                rememberCredentials = true;
                username.setText(credentials.username());
                password.setText(credentials.password());
                userText = credentials.username();
                userTextCaptured = true;
                showValidate = true;
                showPassword = true;
                canLog = true;
                thread = new LoginThread(credentials.username().replace(" ", ""), credentials.password(), true);
                thread.start();
                loginQueued = true;
            }
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        super.drawScreen(mouseX, mouseY, partialTicks);

        RoundedUtil.drawRound(5, 5, 80, 20, 6, new Color(15,15,15, 180));
        String blurText = "blur: " + (blurEnabled ? EnumChatFormatting.GREEN + "on" : EnumChatFormatting.RED + "off");
        Fonts.ubuntu19.drawCenteredString(blurText , 45, 13.5f, -1);

        if (blurEnabled) {
            Blur.startBlur();
            RoundedUtil.drawShaderRound(this.width / 4f - 28, this.height / 4f - 34, 550, 350, 4, Color.BLACK);
            Blur.endBlur(10, 1);
        }

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(this.width / 4f - 28, this.height / 4f - 34, 550, 350, 4, Color.BLACK);
        RenderUtil.drawGradientRect(this.width / 4f - 28.05, this.height / 4f - 35, 551.3, 20, Color.BLACK.getRGB(), Color.BLACK.getRGB());
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(this.width / 4f - 28, this.height / 4f - 34, 550, 350, 4, new Color(10,10,10, 200));
        RenderUtil.drawGradientRect(this.width / 4f - 28.05, this.height / 4f - 35, 551.3, 22, new Color(0x2C2C2C).darker().getRGB(), new Color(0x2C2C2C).getRGB());

        Fonts.ubuntu19.drawString("login@gothaj-client:~", this.width / 4f + 195, this.height / 4f - 25, -1);
        Fonts.ubuntu19.drawString("To login to client please use your forum credentials.", this.width / 4f - 26.7f, this.height / 4f - 9.5f, -1);
        Fonts.ubuntu19.drawString("In case you forgot your credentials, please reset them on forums.", this.width / 4f - 26.7f, this.height / 4f + 0.5f, -1);
        FontUtil.drawHorizontalGradient(Fonts.ubuntu19, "username@gothaj-client" + EnumChatFormatting.WHITE + ":" + EnumChatFormatting.RESET + "~" + EnumChatFormatting.WHITE + "$", this.width / 4f - 26.7f, this.height / 4f + 30, 5);

        this.username.drawTextBox();

        if (showValidate) {
            boolean validUsername = !(username.getText().contains("/") || username.getText().contains(",") || username.getText().contains("."));
            String validate = "Username validation check... " + (!validUsername ? EnumChatFormatting.RED + "[Error]" : EnumChatFormatting.GREEN + "[Success]");
            Fonts.ubuntu19.drawString(validate, this.width / 4f - 26.7f, this.height / 4f + 42, -1);

            if (validUsername) {
                showPassword = true;
                invalidUsernameTime = 0;
            } else {
                if (invalidUsernameTime == 0) {
                    invalidUsernameTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - invalidUsernameTime >= 2000) {
                    showValidate = false;
                    invalidUsernameTime = 0;
                }
                return;
            }

            if (!userTextCaptured) {
                userText = username.getText();
                userTextCaptured = true;
            }
        }
        if (showPassword) {

            if (userTextCaptured && username.getText().isEmpty()) {
                username.setText(userText);
            }
            this.password.setFocused(true);
            this.password.drawTextBox();
            FontUtil.drawHorizontalGradient(Fonts.ubuntu19, "password@gothaj-client" + EnumChatFormatting.WHITE + ":" + EnumChatFormatting.RESET + "~" + EnumChatFormatting.WHITE + "$", this.width / 4f - 26.7f, this.height / 4f + 54, 5);
            Fonts.ubuntu19.drawString(LoginThread.status, this.width / 4f - 26.7f, this.height / 4f + 66, new Color(0x33588A).getRGB());
            Fonts.ubuntu19.drawString("[" + (rememberCredentials ? "x" : " ") + "] remember credentials", this.width / 4f - 26.7f, this.height / 4f + 82, -1);
            canLog = true;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= 5 && mouseX <= 85 && mouseY >= 5 && mouseY <= 25) {
            blurEnabled = !blurEnabled;
            return;
        }

        if (showPassword && mouseX >= this.width / 4f - 28 && mouseX <= this.width / 4f + 180
                && mouseY >= this.height / 4f + 78 && mouseY <= this.height / 4f + 93) {
            rememberCredentials = !rememberCredentials;
            if (!rememberCredentials) {
                RememberedCredentials.delete();
            }
            return;
        }

        if (!showPassword || (LoginThread.status != null && LoginThread.status.contains("failed"))) {
            this.username.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            this.username.setFocused(false);
            this.password.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!showValidate) {
            this.username.textboxKeyTyped(typedChar, keyCode);
        } else if (showPassword) {
            this.username.setFocused(false);
            this.password.textboxKeyTyped(typedChar, keyCode);
        }
        if (keyCode == 15) {
            username.setFocused(!username.isFocused());
            password.setFocused(!password.isFocused());
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            if (!username.getText().isEmpty() && username.getText().length() > 2)
                showValidate = true;

            if (showPassword) {
                this.username.setFocused(false);
                this.password.setFocused(true);
            }

            if (canLog && !loginQueued) {
                if (!username.getText().isEmpty()) {
                    thread = new LoginThread(userText.replace(" ", ""), password.getText(), rememberCredentials);
                    thread.start();
                    loginQueued = true;
                }
            }
        }

        if (keyCode == Keyboard.KEY_V && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String clip;
                try {
                    clip = (String) cb.getData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException e) {
                    throw new RuntimeException(e);
                }

                if (clip != null && !clip.isEmpty()) {
                    if (!showPassword) {
                        username.setText(clip);
                    } else {
                        password.setText(clip);
                    }
                }
            }
        }
    }

}
