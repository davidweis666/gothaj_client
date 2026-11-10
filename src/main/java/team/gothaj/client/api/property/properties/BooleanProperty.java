package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;

import java.util.function.Supplier;

public class BooleanProperty extends Property<Boolean> {

    public BooleanProperty(String name, Boolean toggled) {
        super(name, toggled);
    }

    public void toggle() {
        setProperty(!getProperty());
    }

    @Override
    public void setProperty(Boolean property) {
        super.setProperty(property);

        if (!property) {
            disableDependents();
        }
    }
}
