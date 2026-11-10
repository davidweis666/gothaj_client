package team.gothaj.client.api.property;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.impl.event.value.ValueChangeEvent;
import team.gothaj.client.main.GothajClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
public class Property<P> {
    private final String name;
    private Supplier<Boolean> visible = () -> true;
    private Property<?> dependency;
    private List<Property<?>> dependents = new ArrayList<>();

    private final List<PropertyChangeListener<P>> changeListeners = new ArrayList<>();

    private P property;

    public Property(String name, P property) {
        this.name = name;
        this.property = property;
    }

    @SuppressWarnings("unchecked")
    public <T extends Property<P>> T setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return (T) this;
    }

    /**
     * Explicitly attach a dependency property, so this property will be
     * treated as disabled when the dependency is off.
     */
    @SuppressWarnings("unchecked")
    public <T extends Property<P>> T withDependency(Property<?> dependency) {
        setDependency(dependency);
        return (T) this;
    }

    public void setProperty(P property) {
        P oldValue = this.property;
        P newValue = property;

        for (PropertyChangeListener<P> listener : changeListeners) {
            P modifiedValue = listener.onChange(this, oldValue, newValue);
            if (modifiedValue != null) {
                newValue = modifiedValue;
            }
        }

        if (oldValue == null && newValue == null) {
            return;
        }

        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }

        this.property = newValue;

        GothajClient.getInstance().getEventBus().fire(new ValueChangeEvent(this));

        if (this instanceof BooleanProperty && !((BooleanProperty) this).getProperty()) {
            disableDependents();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Property<P>> T addChangeListener(PropertyChangeListener<P> listener) {
        this.changeListeners.add(listener);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Property<P>> T removeChangeListener(PropertyChangeListener<P> listener) {
        this.changeListeners.remove(listener);
        return (T) this;
    }

    public void clearChangeListeners() {
        this.changeListeners.clear();
    }

    public <T extends Property<P>> void setDependency(Property<?> dependency) {
        this.dependency = dependency;

        if (dependency != null) {
            dependency.getDependents().add(this);
        }
    }
    

    /**
     * Visible & enabled check.
     * If there is a BooleanProperty dependency, this property is only available
     * when that dependency is true.
     */
    public boolean isAvailable() {
        if (!visible.get()) {
            return false;
        }

        if (dependency instanceof BooleanProperty boolDep) {
            return boolDep.getProperty() && visible.get();
        }

        return visible.get();
    }

    protected void disableDependents() {
        for (Property<?> dependent : dependents) {
            if (dependent instanceof BooleanProperty boolDependent) {
                if (boolDependent.getProperty()) {
                    boolDependent.setProperty(false);
                }
            }
        }
    }

    @FunctionalInterface
    public interface PropertyChangeListener<P> {
        P onChange(Property<P> property, P oldValue, P newValue);
    }
}
