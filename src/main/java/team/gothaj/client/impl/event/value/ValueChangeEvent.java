package team.gothaj.client.impl.event.value;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.gothaj.client.api.property.Property;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */

@Getter
@AllArgsConstructor
public class ValueChangeEvent extends Event {

    private Property<?> property;

}
