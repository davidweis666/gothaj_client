package team.gothaj.client.impl.command.general;

import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandFriend extends Command {

    @Override
    public CommandCategory getCategory() { return CommandCategory.GENERAL; }

    @Override
    public String getDescription() {
        return "Allows you to modify your friend list";
    }


    @Override
    public String[] getAliases() {
        return new String[] {"friend", "f"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "f/friend <add/remove/list> <friend>";
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length <= 1) {
            Logger.clientChat(getUsage());
            return;
        }

        switch (args[1]) {
            case "add" -> {
                if (args.length != 2) {
                    Logger.clientChat(getUsage());
                    return;
                }
                if (!GothajClient.getInstance().getFriendManager().isFriend(args[2])) {
                    GothajClient.getInstance().getFriendManager().addFriend(args[2]);
                }

            }
            case "remove" -> {
                if (args.length != 2) {
                    Logger.clientChat(getUsage());
                    return;
                }
                if (GothajClient.getInstance().getFriendManager().isFriend(args[2])) {
                    GothajClient.getInstance().getFriendManager().removeFriend(args[2]);
                } else {
                    GothajClient.getInstance().getNotificationManager().add("Friends", args[2] + " is not your friend!", Notification.Type.INFO);
                }
            }
            case "list" -> {
               Logger.clientChat(GothajClient.getInstance().getFriendManager().getItems().toString());
            }

            default -> Logger.clientChat(getUsage());
        }
    }
}
