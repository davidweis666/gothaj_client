package team.gothaj.client.impl.event.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.progress.Progress;
import team.gothaj.eventbus.impl.Event;

@AllArgsConstructor
@Getter
@Setter
public class ProgressEvent extends Event {
	private Progress progress;
}
