package team.gothaj.client.impl.command.module;

import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandToggle extends Command {


    @Override
    public CommandCategory getCategory() { return CommandCategory.MODULE; }

    @Override
    public String getDescription() {
        return "Enable/Disable modules";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"toggle", "t"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "t/toggle <module>";
    }

    @Override
    public void runCommand(String[] args) {

        if (args.length < 1) {
            Logger.clientChat(getUsage());
            return;
        }

        Module module = GothajClient.getInstance().getModuleManager().find(args[1]);

        if (module == null) {
            Logger.clientChat("Module not found");
            return;
        }

        Logger.clientChat(module.getName() + " has been " + (module.isToggled() ? EnumChatFormatting.RED + "disabled" : EnumChatFormatting.GREEN + "enabled"));
        module.toggle();


    }
}
