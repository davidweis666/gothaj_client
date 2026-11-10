package team.gothaj.client.api.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

@AllArgsConstructor
@Getter
public class Action {

    private final Runnable queue;
    @Setter
    private int tick = 0;

}
