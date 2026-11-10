package team.gothaj.client.impl.event.input;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class InputEvent extends Event {

    private int code;

    private boolean key;
}
