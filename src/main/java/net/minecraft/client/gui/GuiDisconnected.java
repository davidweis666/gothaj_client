package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.impl.component.SessionComponent;
import team.gothaj.client.impl.module.misc.JoinClaim;
import team.gothaj.client.main.GothajClient;

public class GuiDisconnected extends GuiScreen
{
    private String reason;
    private IChatComponent message;
    private List<String> multilineMessage;
    private final GuiScreen parentScreen;
    private int field_175353_i;
    private boolean sentDisableNotice, banNotify;

    public GuiDisconnected(GuiScreen screen, String reasonLocalizationKey, IChatComponent chatComp)
    {
        this.parentScreen = screen;
        this.reason = I18n.format(reasonLocalizationKey, new Object[0]);
        this.message = chatComp;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1) {
            mc.displayGuiScreen(this.parentScreen);
        }
    }

    public void initGui()
    {
        this.buttonList.clear();
        this.multilineMessage = this.fontRendererObj.listFormattedStringToWidth(this.message.getFormattedText(), this.width - 50);
        this.field_175353_i = this.multilineMessage.size() * this.fontRendererObj.FONT_HEIGHT;
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 2 + this.field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT, I18n.format("gui.toMenu", new Object[0])));
        GothajClient.getInstance().getAccountRepository().getCurrentAccount().setPlayTime(GothajClient.getInstance().getComponentManager().getAndCast(SessionComponent.class).getPlayTime());
        GothajClient.getInstance().getComponentManager().getAndCast(SessionComponent.class).setPlayTime(0);
    }

    @Override
    public void onGuiClosed() {
        sentDisableNotice = false;
        banNotify = false;
        super.onGuiClosed();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, this.reason, this.width / 2, this.height / 2 - this.field_175353_i / 2 - this.fontRendererObj.FONT_HEIGHT * 2, 0xAA555A);
        int i = this.height / 2 - this.field_175353_i / 2;

        boolean shouldDisable = false;
        boolean shouldNotify = false;

        if (this.multilineMessage != null) {
            for (String s : this.multilineMessage) {
                if (s.contains("banned") && !shouldNotify) {
                    shouldNotify = true;
                }
                if (!sentDisableNotice && (s.contains("You are not logged into your Minecraft account")
                        || s.contains("Failed to login: null"))) {
                    shouldDisable = true;
                }
                this.drawCenteredString(this.fontRendererObj, s, this.width / 2, i, 0xFFFFFF);
                i += this.fontRendererObj.FONT_HEIGHT;
            }

          /*  if (shouldNotify && !banNotify) {
                Irc10LeaveServer packet = new Irc10LeaveServer();
                packet.reason = "cheating";
                GothajClient.getInstance().getIrcManager().getClient().sendPacket(packet);
            }*/

            if (shouldDisable && !sentDisableNotice) {

                JoinClaim jc = GothajClient.getInstance().getModuleManager().find(JoinClaim.class);
                if (jc != null && jc.isToggled()) {
                    jc.setToggled(false);
                }
                GothajClient.getInstance().getNotificationManager()
                        .add("Module", "JoinClaim disabled, re-log please", Notification.Type.INFO);

                sentDisableNotice = true;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

}
