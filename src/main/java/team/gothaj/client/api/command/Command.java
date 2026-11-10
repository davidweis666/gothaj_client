package team.gothaj.client.api.command;

public abstract class Command {

    public abstract CommandCategory getCategory();

    public abstract String getDescription();

    public abstract String[] getAliases();

    public abstract String getUsage();

    public abstract void runCommand(String[] args);


}
