package team.gothaj.client.impl.event.render.shader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class ShaderOverlayEvent extends Event {

    private final ScaledResolution scaledResolution;
    private Type type;


    public enum Type {
        BLUR, BLOOM, GOTHAJ
    }
}
