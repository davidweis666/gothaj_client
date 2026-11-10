package team.gothaj.client.api.command;

import lombok.Getter;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.impl.command.general.CommandHelp;
import team.gothaj.client.impl.command.general.CommandIGN;
import team.gothaj.client.impl.command.irc.CommandIRCPM;
import team.gothaj.client.impl.command.module.CommandBind;
import team.gothaj.client.impl.command.module.CommandConfig;
import team.gothaj.client.impl.command.irc.CommandIRC;
import team.gothaj.client.impl.command.irc.CommandIRCChat;
import team.gothaj.client.impl.command.general.CommandFriend;
import team.gothaj.client.impl.command.module.CommandPanic;
import team.gothaj.client.impl.command.module.CommandToggle;

import java.util.Arrays;

public class CommandManager extends ListManager<Command> {


    @Getter
    public String prefix;

    @Override
    public void init() {

        //General
        add(new CommandHelp());
        add(new CommandFriend());
        add(new CommandIGN());

        //Modules
        add(new CommandConfig());
        add(new CommandBind());
        add(new CommandToggle());
        add(new CommandPanic());


        //IRC
        add(new CommandIRC());
        add(new CommandIRCChat());
        add(new CommandIRCPM());


        super.init();
    }


    public Command find(String name) {
        return getItems().stream()
                .filter(c -> Arrays.stream(c.getAliases())
                        .anyMatch(a -> a.equalsIgnoreCase(name)))
                .findFirst()
                .orElse(null);
    }

    public Command find(Class<? extends Command> clazz) {
        return getItems().stream()
                .filter(c -> c.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }
}
