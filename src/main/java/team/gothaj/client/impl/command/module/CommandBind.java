package team.gothaj.client.impl.command.module;

import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandBind extends Command {

    @Override
    public CommandCategory getCategory() { return CommandCategory.MODULE; }

    @Override
    public String getDescription() {
        return "Binds module to your preferred key";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"bind", "b"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "b/bind <module> <key> <hold>";
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length <= 3) {
            Logger.clientChat(getUsage());
            return;
        }
        int keyCode;
        String holdArg = args[3];
        boolean hold = Boolean.parseBoolean(holdArg);
        String keyArg = args[2];

        if (keyArg.startsWith("mouse")) {
            if (keyArg.equalsIgnoreCase("mouse0") || keyArg.equalsIgnoreCase("mouse1")) {
                Logger.clientChat("Left and right click cannot be bound!");
                return;
            }
            try {
                int mouseButton = Integer.parseInt(keyArg.substring(5));
                if (mouseButton >= 3 && mouseButton <= 5) {
                    keyCode = -mouseButton;
                } else {
                    keyCode = 0;
                }
            } catch (NumberFormatException e) {
                keyCode = 0;
            }
        } else {
            keyCode = Keyboard.getKeyIndex(keyArg.toUpperCase());
        }

        Module mod = GothajClient.getInstance().getModuleManager().find(args[1]);

        if (mod == null) {
            Logger.clientChat("Module " + args[1] + " not found!");
            return;
        }
        mod.setKeyCode(keyCode);
        mod.setHold(hold);
        Logger.clientChat(mod.getName() + " was bound to " + keyArg.toUpperCase());
    }
}
