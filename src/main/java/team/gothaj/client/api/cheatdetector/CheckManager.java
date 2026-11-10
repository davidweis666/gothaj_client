package team.gothaj.client.api.cheatdetector;

import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.impl.cheatdetector.combat.*;
import team.gothaj.client.impl.cheatdetector.misc.InvalidNameCheck;
import team.gothaj.client.impl.cheatdetector.movement.*;
import team.gothaj.client.impl.cheatdetector.player.FakeLagCheck;
import team.gothaj.client.impl.cheatdetector.world.*;

public class CheckManager extends ListManager<Check> {

    @Override
    public void init() {
        final Check[] checks = {
                new AutoBlockCheck(),
                new AimbotCheck(),
                new ReachCheck(),
                new AutoClickerCheck(),
                new NoSlowCheck(),
                new InvalidNameCheck(),
                new OmniSprintCheck(),
                new FakeLagCheck(),
                new FastPlaceCheck(),
                new KillAuraCheck(),
                new SpeedCheck(),
                new ScaffoldCheck()
        };

        for (final Check check : checks) {
            add(check);
        }
        super.init();
    }

}
