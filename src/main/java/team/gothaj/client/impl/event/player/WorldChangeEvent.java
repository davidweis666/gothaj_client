package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.multiplayer.WorldClient;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class WorldChangeEvent extends Event {

    private WorldClient world;
}
