package team.gothaj.client.impl.command.irc;

import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandIRCChat extends Command {

    @Override
    public CommandCategory getCategory() { return CommandCategory.IRC; }

    @Override
    public String getDescription() {
        return "Chat with other client users";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"c", "chat"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "c / " + GothajClient.getInstance().getCommandManager().getPrefix() + "chat <msg>";
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length <= 1) {
            Logger.clientChat(getUsage());
            return;
        }

        StringBuilder r = new StringBuilder();
        for (int i = 1; i < args.length; ++i) {
            r.append(args[i]).append(" ");
        }

        String message = r.toString().trim();
        if (message.isEmpty()) {
            Logger.clientChat(getUsage());
            return;
        }

        GothajClient.getInstance().getIrcManager().sendChatMessage(message);
    }
}
