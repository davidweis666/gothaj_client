package team.gothaj.client.api.module;

import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.impl.module.client.*;
import team.gothaj.client.impl.module.combat.*;
import team.gothaj.client.impl.module.ghost.*;
import team.gothaj.client.impl.module.misc.*;
import team.gothaj.client.impl.module.movement.*;
import team.gothaj.client.impl.module.player.*;
import team.gothaj.client.impl.module.world.*;
import team.gothaj.client.impl.module.visual.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager extends ListManager<Module> {


    @Override
    public void init() {
        final Module[] modules = {
                new ESP(),
                new ShaderProcessing(),
                new BetterChat(),
                new BetterHotbar(),
                new BetterTab(),
                new LagRange(),
                new TickBase(),
                new ClickFriends(),
                new Scaffold(),
                new Backtrack67(),
                new AutoHeal(),
                new AirStuck(),
                new AutoBlockIn(),
                new DamageEffects(),
                new AntiInvisibles(),
                new NoDiscordRPC(),
                new FreeLook(),
                new AntiCrash(),
                new PacketModifier(),
                new NoCustomPack(),
                new Disabler(),
                new BalanceAbuse(),
                new ClientSpoofer(),
                new InertiaDelay(),
                new HealthFix(),
                new AntiFireball(),
                new Detector(),
                new FastBreak(),
                new TimerRange(),
                new MoreParticles(),
                new DamageCalculator(),
                new ThirdPerson(),
                new KillEffect(),
                new NoFall(),
                new Timer(),
                new Debug(),
                new Notify(),
                new AutoBlock(),
                new AutoGG(),
                new ChatBypass(),
                new ResourceSpoof(),
                new CustomFOV(),
                new CheatDetector(),
                new AutoSprint(),
                new InvMove(),
                new JoinClaim(),
                new AspectRatio(),
                new FastLadder(),
                new WallClimb(),
                new Capes(),
                new ClickGUI(),
                new MediaPlayer(),
                new Flight(),
                new Camera(),
                new Ambience(),
                new Backtrack(),
                new InputSounds(),
                new ToggleSounds(),
                new Tweaks(),
                new AutoTool(),
                new Chams(),
                new KeepSprint(),
                new MoreKB(),
                new KillAura(),
                new HitMarker(),
                new Speed(),
                new ChestESP(),
                new ViewModel(),
                new TargetHUD(),
                new CustomSkin(),
                new Targets(),
                new Breadcrumbs(),
                new AntiEffect(),
                new ChinaHat(),
                new FakeLag(),
                new Breaker(),
                new EnchantGlint(),
                new HitColor(),
                new AutoRegister(),
                new ItemPhysics(),
                new Waypoints(),
                new Manager(),
                new Stealer(),
                new ScaffoldRecorder(),
                new NoShadows(),
                new Regen(),
                new SkeletonESP(),
                new TruePosisition(),
                new AntiAim(),
                new Velocity(),
                new NoSlow(),
                new FastPlace(),
                new AutoBedDefense(),
                new AutoProjectile(),
                new Phase(),
                new Clipper(),
                new NameProtect(),
                new Extinguish(),
                new Annoyer(),
                new Killsults(),
                new KnockbackDisplacement(),
                new AimAssist(),
                new AutoClicker(),
                new BlockHit(),
                new Eagle(),
                new Hud(),

        };

        for (final Module module : modules) {
            add(module);
        }

        super.init();
    }


    @SuppressWarnings("unchecked")
    public <T extends Module> T getAndCast(Class<T> clazz) {
        return (T) new ArrayList<>(getItems()).stream()
                .filter(m -> m.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }


    public List<Module> getModulesInCategory(ModuleCategory category) {
        List<Module> modulesInCategory = new ArrayList<>();

        for (Module module : new ArrayList<>(getItems())) {
            if (module.getModuleCategory().equals(category)) {
                modulesInCategory.add(module);
            }
        }
        return modulesInCategory;
    }


    public Module find(String name) {
        final String lookupName = name != null && (name.equalsIgnoreCase("AutoArmor") || name.equalsIgnoreCase("InvManager") || name.equalsIgnoreCase("InventoryManager"))
                ? "Manager"
                : name;

        return new ArrayList<>(getItems()).stream()
                .filter(m -> m.getName().equalsIgnoreCase(lookupName))
                .findFirst()
                .orElse(null);
    }

    public <T extends Module> T find(Class<? extends Module> clazz) {
        return (T) new ArrayList<>(getItems()).stream()
                .filter(m -> m.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }
}
