package team.gothaj.client.impl.component;

import team.gothaj.client.api.component.Component;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.misc.ProgressEvent;
import team.gothaj.client.impl.module.visual.widget.ProgressBarWidget;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;

public class ProgressComponent extends Component {

	@Listen
	public void onTick(TickEvent event) {
		final ProgressEvent e = new ProgressEvent(null);
		GothajClient.getInstance().getEventBus().fire(e);

		GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
			if (w instanceof ProgressBarWidget progressBar) {
				progressBar.setProgress(e.getProgress());
			}
		});
	}
}
