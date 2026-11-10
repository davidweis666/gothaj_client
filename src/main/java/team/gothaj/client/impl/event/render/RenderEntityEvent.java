package team.gothaj.client.impl.event.render;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.entity.Entity;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */

@Getter
@Setter
@RequiredArgsConstructor
public class RenderEntityEvent extends Event {

    private final Entity entity;

    private final float partialTicks;

    private Renderable preRenderCallback;
    private Renderable postRenderCallback;


    public void onPreRender() {
        if (preRenderCallback != null)
            this.preRenderCallback.draw();
    }

    public void onPostRender() {
        if (postRenderCallback != null)
            postRenderCallback.draw();
    }
}
