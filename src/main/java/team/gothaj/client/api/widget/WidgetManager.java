package team.gothaj.client.api.widget;

import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.impl.module.visual.widget.*;

public class WidgetManager extends ListManager<Widget> {

    @Override
    public void init() {
        final Widget[] widgets = {
                new BlockCountWidget(),
                new ScoreboardWidget(),
                new TargetHUDWidget(),
                new ActivePotionsWidget(),
                new SessionInfoWidget(),
                new MediaPlayerWidget(),
                new TabGUIWidget(),
                new ArraylistWidget(),
                new WatermarkWidget(),
                new ProgressBarWidget(),
                new ArmorStatusWidget(),
                new InventoryDisplayWidget()
        };

        for  (Widget widget : widgets) {
            add(widget);
        }

        super.init();
    }

}
