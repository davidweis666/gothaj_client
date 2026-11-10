package team.gothaj.client.api.util;

import team.gothaj.client.api.listener.Listener;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.eventbus.Listen;

import java.util.*;

public class ActionManager extends Listener {

    private final Queue<Action> queues = new ArrayDeque<>();

    public void queue(Action queue) {
        queues.offer(queue);
    }

    @Listen
    public void onTick(TickEvent event) {
        Iterator<Action> iterator = queues.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            action.setTick(action.getTick() - 1);
            if (action.getTick() <= 0) {
                action.getQueue().run();
                iterator.remove();
            }
        }
    }

}
