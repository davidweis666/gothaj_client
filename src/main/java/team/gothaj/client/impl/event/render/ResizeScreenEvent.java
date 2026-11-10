package team.gothaj.client.impl.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class ResizeScreenEvent extends Event {

    private ScaledResolution sr;
}
