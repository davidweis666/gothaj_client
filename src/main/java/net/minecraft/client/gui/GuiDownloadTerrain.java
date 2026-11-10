package net.minecraft.client.gui;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.client.Minecraft;

import java.io.IOException;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;

public class GuiDownloadTerrain extends GuiScreen
{
    private NetHandlerPlayClient netHandlerPlayClient;
    private int progress;
    private CustomLoadingScreen customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();

    public GuiDownloadTerrain(NetHandlerPlayClient netHandler)
    {
        this.netHandlerPlayClient = netHandler;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    }

    public void initGui()
    {
        this.buttonList.clear();
    }

    public void updateScreen()
    {
        ++this.progress;

        if (this.progress % 20 == 0)
        {
            this.netHandlerPlayClient.addToSendQueue(new C00PacketKeepAlive());
        }

        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world != null) {
            int spawnX = world.getSpawnPoint().getX() >> 4;
            int spawnZ = world.getSpawnPoint().getZ() >> 4;
            Chunk spawnChunk = world.getChunkFromChunkCoords(spawnX, spawnZ);
            if ((spawnChunk != null && !spawnChunk.isEmpty()) || (!Minecraft.getMinecraft().isIntegratedServerRunning() && ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_20))) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        }

    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.customLoadingScreen != null)
        {
            this.customLoadingScreen.drawBackground(this.width, this.height);
        }
        else
        {
            this.drawBackground();
        }

        this.drawCenteredString(this.fontRendererObj, I18n.format("multiplayer.downloadingTerrain", new Object[0]), this.width / 2, this.height / 2 - 50, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
