package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;

import java.util.concurrent.CompletableFuture;

/**
 * @author vajzii
 * @since 2025
 */
public class ActionProperty extends Property<Runnable> {

    public ActionProperty(String name) {
        super(name, null);
    }

    public ActionProperty(String name, Runnable property) {
        super(name, property);
    }

    public void runAction() {
        CompletableFuture.runAsync(() -> getProperty().run());
    }

    public ActionProperty setListener(Runnable listener) {
        this.setProperty(listener);
        return this;
    }


}
