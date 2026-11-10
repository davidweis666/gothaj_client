package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import lombok.Getter;

public class NumberProperty extends Property<Float> {
    @Getter
    private final float min, max, increment, defaultValue;

    @Getter
    private final Type type;

    public NumberProperty(String name, float defaultValue, float min, float max, float increment) {
        super(name, defaultValue);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.type = Type.DOUBLE;
    }


    public NumberProperty(String name, int defaultValue, int min, int max, int increment) {
        super(name, (float) defaultValue);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.type = Type.INT;

    }


    public void setProperty(Integer property) {
        setProperty((float) property);
    }

    public enum Type {
        INT, DOUBLE
    }
}