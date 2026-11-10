package team.gothaj.client.impl.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.CancellableEvent;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@Setter
@AllArgsConstructor
public class OrientCameraEvent extends CancellableEvent {

    private double distance;

}
