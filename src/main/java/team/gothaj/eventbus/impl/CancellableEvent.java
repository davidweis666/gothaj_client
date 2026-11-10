package team.gothaj.eventbus.impl;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CancellableEvent extends Event {

    boolean cancelled;
}
