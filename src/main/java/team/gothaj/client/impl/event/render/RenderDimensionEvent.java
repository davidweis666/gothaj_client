package team.gothaj.client.impl.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.gothaj.eventbus.impl.Event;

@Getter
@AllArgsConstructor
public class RenderDimensionEvent extends Event {

    float partialTicks;
}
