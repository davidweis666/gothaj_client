package team.gothaj.eventbus;

public enum Priority {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    CRITICAL(3);

    public final int value;
    Priority(int value) { this.value = value; }
}
