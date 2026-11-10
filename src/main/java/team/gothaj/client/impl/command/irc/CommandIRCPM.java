package team.gothaj.client.impl.command.irc;

import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandIRCPM extends Command {


    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IRC;
    }

    @Override
    public String getDescription() {
        return "Private message to other IRC user";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"pm", "msg"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "pm / " + GothajClient.getInstance().getCommandManager().getPrefix() + "msg <user> <msg>";
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length < 2) {
            Logger.clientChat(getUsage());
            return;
        }

        StringBuilder r = new StringBuilder();
        for (int i = 2; i < args.length; ++i) {
            r.append(args[i]).append(" ");
        }

        String message = r.toString().trim();
        if (message.isEmpty()) {
            Logger.clientChat(getUsage());
            return;
        }

        GothajClient.getInstance().getIrcManager().sendPrivateMessage(args[1], r.toString());
    }
}
