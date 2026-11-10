package team.gothaj.client.api.property.properties;

import lombok.Getter;
import team.gothaj.client.api.property.Property;

public class RangedNumberProperty extends Property<Float> {

    @Getter
    private final float min, max, increment;
    @Getter
    private float left, right;
    @Getter
    private final Type type;

    public RangedNumberProperty(String name, float left, float right, float min, float max, float increment) {
        super(name, right - left);
        this.left = left;
        this.right = right;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.type = Type.DOUBLE;
    }

    public RangedNumberProperty(String name, int left, int right, int min, int max, int increment) {
        super(name, (float) right - left);
        this.left = left;
        this.right = right;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.type = Type.INT;
    }

    public void setLeft(float value) {
        this.left = Math.max(min, Math.min(right, value));
    }

    public void setRight(float value) {
        this.right = Math.max(left, Math.min(max, value));
    }

    public void setProperty(Integer property) {
        setRight((float) property);
    }

    @Override
    public void setProperty(Float property) {
        setRight(property);
    }

    public enum Type {
        INT, DOUBLE
    }
}
