package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import lombok.Getter;

@Getter
public class InputProperty extends Property<String> {
    private final String string;
    private final boolean isPassword;

    public InputProperty(String name) {
        this(name, "", false);
    }

    public InputProperty(String name, String value) {
        super(name, value);
        this.string = value != null ? value : "";
        this.isPassword = false;
    }

    public InputProperty(String name, String value, boolean isPassword) {
        super(name, value);
        this.string = value != null ? value : "";
        this.isPassword = isPassword;
    }
}