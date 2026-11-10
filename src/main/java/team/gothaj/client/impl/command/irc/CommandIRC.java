package team.gothaj.client.impl.command.irc;

import systems.vajzii.irc.main.IrcManager;
import systems.vajzii.irc.network.packets.Irc07ClientCommand;
import systems.vajzii.irc.network.packets.Irc24FriendRequest;
import systems.vajzii.irc.network.packets.Irc25FriendResponse;
import systems.vajzii.irc.network.packets.Irc26FriendRemove;
import systems.vajzii.irc.network.packets.Irc28PartyInvite;
import systems.vajzii.irc.network.packets.Irc29PartyResponse;
import systems.vajzii.irc.network.packets.Irc32PartyKick;
import systems.vajzii.irc.network.packets.Irc33PartyChat;
import systems.vajzii.irc.network.packets.Irc34PartyDisband;
import systems.vajzii.irc.network.packets.Irc31PartyLeave;
import systems.vajzii.irc.players.IrcPlayer;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandIRC extends Command {

    @Override
    public CommandCategory getCategory() { return CommandCategory.IRC; }

    @Override
    public String getDescription() {
        return "Variety of IRC commands";
    }

    @Override
    public String[] getAliases() {
        return new String[] {"irc"};
    }

    @Override
    public String getUsage() {
        return GothajClient.getInstance().getCommandManager().getPrefix() + "irc <list/friend/party/nick/info/rank/crash> <args>";
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length <= 1) {
            Logger.ircChat(getUsage());
            return;
        }

        // Friend/party are available to everyone (matches the server's IRCRank.USER
        // requirement) - only the staff/moderation subcommands below are rank-gated.
        boolean staffOnly = !args[1].equalsIgnoreCase("friend") && !args[1].equalsIgnoreCase("party");
        if (staffOnly && GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() < 20) {
            Logger.ircChat("You do not have permission to use this command.");
            return;
        }

        switch (args[1]) {
            case "friend": {
                if (args.length < 3) {
                    Logger.ircChat("Use .irc friend <add|accept|deny|remove|list> [name]");
                    break;
                }
                String sub = args[2].toLowerCase();
                String name = args.length >= 4 ? args[3] : null;
                switch (sub) {
                    case "add", "request" -> {
                        if (name == null) {
                            Logger.ircChat("Use .irc friend add <name>");
                        } else {
                            Irc24FriendRequest packet = new Irc24FriendRequest();
                            packet.toNick = name;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "accept" -> {
                        if (name == null) {
                            Logger.ircChat("Use .irc friend accept <name>");
                        } else {
                            Irc25FriendResponse packet = new Irc25FriendResponse();
                            packet.otherNick = name;
                            packet.accepted = true;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "deny", "decline" -> {
                        if (name == null) {
                            Logger.ircChat("Use .irc friend deny <name>");
                        } else {
                            Irc25FriendResponse packet = new Irc25FriendResponse();
                            packet.otherNick = name;
                            packet.accepted = false;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "remove", "delete" -> {
                        if (name == null) {
                            Logger.ircChat("Use .irc friend remove <name>");
                        } else {
                            Irc26FriendRemove packet = new Irc26FriendRemove();
                            packet.targetNick = name;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "list" -> {
                        Irc07ClientCommand cmd = new Irc07ClientCommand();
                        cmd.command = "friend list";
                        IrcManager.getIrcClient().sendPacket(cmd);
                    }
                    default -> Logger.ircChat("Use .irc friend <add|accept|deny|remove|list> [name]");
                }
                break;
            }
            case "party": {
                if (args.length < 3) {
                    Logger.ircChat("Use .irc party <invite|accept|deny|leave|kick|disband|list|chat> [name|message]");
                    break;
                }
                String sub = args[2].toLowerCase();
                switch (sub) {
                    case "invite" -> {
                        if (args.length < 4) {
                            Logger.ircChat("Use .irc party invite <name>");
                        } else {
                            Irc28PartyInvite packet = new Irc28PartyInvite();
                            packet.toNick = args[3];
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "accept" -> {
                        if (args.length < 4) {
                            Logger.ircChat("Use .irc party accept <name>");
                        } else {
                            Irc29PartyResponse packet = new Irc29PartyResponse();
                            packet.fromNick = args[3];
                            packet.accepted = true;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "deny", "decline" -> {
                        if (args.length < 4) {
                            Logger.ircChat("Use .irc party deny <name>");
                        } else {
                            Irc29PartyResponse packet = new Irc29PartyResponse();
                            packet.fromNick = args[3];
                            packet.accepted = false;
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "leave" -> IrcManager.getIrcClient().sendPacket(new Irc31PartyLeave());
                    case "kick" -> {
                        if (args.length < 4) {
                            Logger.ircChat("Use .irc party kick <name>");
                        } else {
                            Irc32PartyKick packet = new Irc32PartyKick();
                            packet.targetNick = args[3];
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    case "disband" -> IrcManager.getIrcClient().sendPacket(new Irc34PartyDisband());
                    case "list", "info" -> {
                        Irc07ClientCommand cmd = new Irc07ClientCommand();
                        cmd.command = "party list";
                        IrcManager.getIrcClient().sendPacket(cmd);
                    }
                    case "chat", "c" -> {
                        if (args.length < 4) {
                            Logger.ircChat("Use .irc party chat <message>");
                        } else {
                            StringBuilder r = new StringBuilder();
                            for (int i = 3; i < args.length; ++i) {
                                r.append(args[i]).append(" ");
                            }
                            Irc33PartyChat packet = new Irc33PartyChat();
                            packet.message = r.toString().trim();
                            IrcManager.getIrcClient().sendPacket(packet);
                        }
                    }
                    default -> Logger.ircChat("Use .irc party <invite|accept|deny|leave|kick|disband|list|chat> [name|message]");
                }
                break;
            }
            case "nick": {
                if (args.length == 3) {
                    String nick = args[2];
                    Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                    irc07ClientCommand.command = "nick " + nick;
                    IrcManager.getIrcClient().sendPacket(irc07ClientCommand);
                    break;
                }
                Logger.ircChat("Please use .irc nick <nickname>");
                break;
            }
            case "info": {
                if (args.length == 3) {
                    String nick = args[2];
                    Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                    irc07ClientCommand.command = "info " + nick;
                    GothajClient.getInstance().getIrcManager().getClient().sendPacket(irc07ClientCommand);
                    break;
                }
                Logger.ircChat("Please use .irc info <nickname>");
                break;
            }
            case "vanish": {
                IrcPlayer currentPlayer = GothajClient.getInstance().getCurrentPlayer();
                if (currentPlayer != null) {
                    if (currentPlayer.isVanished()) {
                        GothajClient.getInstance().getIrcManager().getClient().setUnvanish(currentPlayer.getIrcNick(), currentPlayer.getRank().getId());
                    } else {
                        GothajClient.getInstance().getIrcManager().getClient().setVanish(currentPlayer.getIrcNick());
                    }
                }
                break;
            }
            case "list": {
                Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                irc07ClientCommand.command = "list";
                GothajClient.getInstance().getIrcManager().getClient().sendPacket(irc07ClientCommand);
                break;
            }
            case "rank": {
                if (args.length == 2) {
                    Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                    irc07ClientCommand.command = "rank e";
                    GothajClient.getInstance().getIrcManager().getClient().sendPacket(irc07ClientCommand);
                    break;
                }
                if (args.length == 3) {
                    Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                    irc07ClientCommand.command = "rank " + args[2] + " e";
                    GothajClient.getInstance().getIrcManager().getClient().sendPacket(irc07ClientCommand);
                    break;
                }
                Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
                irc07ClientCommand.command = "rank " + args[2] + " " + args[3];
                GothajClient.getInstance().getIrcManager().getClient().sendPacket(irc07ClientCommand);
                break;
            }
            case "crash": {
                if (args.length >= 3) {
                    String target = args[2];
                    StringBuilder r = new StringBuilder();
                    for (int i = 3; i < args.length; ++i) {
                        r.append(args[i]).append(" ");
                    }
                    String reason = r.toString().trim();

                    GothajClient.getInstance().getIrcManager().crashUser(target, reason);
                } else {
                    Logger.ircChat("Please use .irc crash <username> <message>");
                }
                break;
            }

            default: {
                Logger.ircChat(getUsage());
            }
        }
    }
}
