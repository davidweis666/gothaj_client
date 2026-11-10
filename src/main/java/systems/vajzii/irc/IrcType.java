package systems.vajzii.irc;

public enum IrcType {

    GOTHAJ("Gothaj");

    private String prefix;

    IrcType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
