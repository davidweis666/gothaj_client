package team.gothaj.client.impl.command.general;

import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.impl.command.irc.CommandIRC;
import team.gothaj.client.impl.command.irc.CommandIRCChat;
import team.gothaj.client.main.GothajClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author vajzii
 * @since 2025
 */
public class CommandHelp extends Command {

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.GENERAL;
    }

    @Override
    public String getDescription() {
        return "Prints list of available commands";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"help", "h"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "h/help";
    }

    @Override
    public void runCommand(String[] args) {
        List<Command> commandList = new ArrayList<>(GothajClient.getInstance().getCommandManager().getItems()
                .stream()
                .sorted(Comparator.comparing(Command::getCategory))
                .toList());

        Map<String, List<Command>> commandsByCategory = new HashMap<>();

        if (GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() < 20) {
            commandList.removeIf(c -> c instanceof CommandIRC);
        }

        Logger.clientChat(" ");

        for (Command c : commandList) {
            String category = String.valueOf(c.getCategory());
            List<Command> commands = commandsByCategory.getOrDefault(category, new ArrayList<>());
            commands.add(c);
            commandsByCategory.put(category, commands);
        }

        for (Map.Entry<String, List<Command>> entry : commandsByCategory.entrySet()) {
            String category = entry.getKey();
            List<Command> commands = entry.getValue();

            Logger.clientChat("------ " + category + " ------");

            for (Command command : commands) {
                String commandName = command instanceof CommandIRCChat ? "Chat" : command.getAliases()[0];
                Logger.clientChat(FontUtil.upperCaseFirst(commandName) + " - " + command.getDescription());
            }
        }

        Logger.clientChat(" ");

    }


}
