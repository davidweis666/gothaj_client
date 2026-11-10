package team.gothaj.client.impl.command.general;

import org.lwjgl.Sys;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CommandIGN extends Command {

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.GENERAL;
    }

    @Override
    public String getDescription() {
        return "Copy IGN to clipboard";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"ign"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "ign";
    }

    @Override
    public void runCommand(String[] args) {
        String ign = Utils.mc.thePlayer.getName();

        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection data = new StringSelection(ign);
        cb.setContents(data, null);

        Logger.clientChat("Your IGN has been copied to clipboard.");
    }
}
