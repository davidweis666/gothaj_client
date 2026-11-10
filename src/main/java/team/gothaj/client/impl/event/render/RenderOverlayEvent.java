package team.gothaj.client.impl.event.render;


import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.eventbus.impl.Event;

@Getter
@AllArgsConstructor
public class RenderOverlayEvent extends Event {

    ScaledResolution scaledResolution;

    float partialTicks;
}
