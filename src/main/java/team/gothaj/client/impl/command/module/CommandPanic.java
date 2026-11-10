package team.gothaj.client.impl.command.module;

import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

public class CommandPanic extends Command {

    @Override
    public CommandCategory getCategory() { return CommandCategory.MODULE; }

    @Override
    public String getDescription() {
        return "Disables all modules except the HUD";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"panic", "p"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "panic";
    }

    @Override
    public void runCommand(String[] args) {
        int disabled = 0;

        for (Module module : GothajClient.getInstance().getModuleManager().getItems()) {
            if (module instanceof Hud) continue;
            if (module.isToggled()) {
                module.toggle();
                disabled++;
            }
        }

        Logger.clientChat(EnumChatFormatting.RED + "Panic: " + EnumChatFormatting.WHITE
                + disabled + " module(s) disabled");
    }
}
