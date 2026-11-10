package team.gothaj.client.impl.module.visual.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.progress.Progress;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;

public class ProgressBarWidget extends Widget {


    @Getter
    @Setter
    private Progress progress = null;

    public ProgressBarWidget() {
        super("Progress Bar", 250, 250, 125, 8, Type.MODULE);
    }
}
