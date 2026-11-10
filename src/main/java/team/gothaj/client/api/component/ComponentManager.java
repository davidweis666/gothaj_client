package team.gothaj.client.api.component;

import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.impl.component.*;

/**
 * @author Kawase
 * @since 15.08.2025
 */
public class ComponentManager extends ListManager<Component> {

    @Override
    public void init() {
        final Component[] components = {
                new RotationComponent(),
                new BlinkComponent(),
                new ProgressComponent(),
                new SessionComponent(),
                new DiscordComponent()
        };

        for (final Component component : components) {
            add(component);
        }

        super.init();
    }


    @SuppressWarnings("unchecked")
    public <T extends Component> T getAndCast(Class<T> clazz) {
        return (T) getItems().stream()
                .filter(m -> m.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }

    public Component find(Class<? extends Component> clazz) {
        return getItems().stream()
                .filter(m -> m.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }
}
