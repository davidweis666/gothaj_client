package team.gothaj.client.impl.command.module;

import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.command.CommandCategory;
import team.gothaj.client.api.config.Config;
import team.gothaj.client.api.config.ConfigManager;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class CommandConfig extends Command {

	@Override
	public CommandCategory getCategory() {
		return CommandCategory.MODULE;
	}

	@Override
	public String getDescription() {
		return "allows you to delete, create, save and load configs";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "config", "c" };
	}

	@Override
	public String getUsage() {
		return GothajClient.getInstance().getCommandManager().getPrefix()
				+ "config delete/create/save/load <config_name>";
	}

	@Override
	public void runCommand(String[] args) {
		if (args.length < 2) {
			Logger.clientChat(getUsage());
			return;
		}
		
		ConfigManager cfg = GothajClient.getInstance().getConfigManager();
		if (args[1].equalsIgnoreCase("Create")) {
			cfg.save(new Config(args[2]));
			cfg.load(new Config(args[2]), true);
			Logger.clientChat("Created Config: " + args[2]);
		} else if (args[1].equalsIgnoreCase("Load")) {
			if (cfg.exists(args[2])) {
				cfg.load(new Config(args[2]), true);
				Logger.clientChat("Loaded Config: " + args[2]);
			} else {
				Logger.clientChat("Config: " + args[2] + " does not exist");
			}
		} else if (args[1].equalsIgnoreCase("Save")) {
			if (cfg.exists(args[2])) {
				cfg.save(new Config(args[2]));
				Logger.clientChat("Saved Config: " + args[2]);
			} else {
				Logger.clientChat("Config: " + args[2] + " does not exist");
			}
		} else if (args[1].equalsIgnoreCase("Delete")) {
			if (cfg.exists(args[2])) {
				cfg.delete(args[2]);
				Logger.clientChat("Deleted Config: " + args[2]);
			} else {
				Logger.clientChat("Config: " + args[2] + " does not exist");
			}
		} else {
			Logger.clientChat(getUsage());
		}
	}
}
