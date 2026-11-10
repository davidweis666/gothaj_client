package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

@Getter
@Setter
public class ModeProperty extends Property<String> {

    public final ArrayList<String> modes;

    public ModeProperty(String name, String mode, String... modes) {
        super(name, mode);
        this.modes = new ArrayList<>(Arrays.asList(modes));
    }

    public boolean is(String theMode) {
        return getProperty().equalsIgnoreCase(theMode);
    }


}
