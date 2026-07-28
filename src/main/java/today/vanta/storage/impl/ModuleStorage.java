package today.vanta.storage.impl;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.*;
import today.vanta.client.module.impl.combat.*;
import today.vanta.client.module.impl.hud.*;
import today.vanta.client.module.impl.misc.*;
import today.vanta.client.module.impl.movement.*;
import today.vanta.client.module.impl.player.*;
import today.vanta.client.module.impl.render.*;
import today.vanta.storage.Storage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleStorage extends Storage<Module> {
    public Module context;

    @Override
    public void subscribe() {
        super.subscribe();

        // Client
        list.add(new ClickGUI());
        list.add(new ClickSettings());
        list.add(new Theme());
        list.add(new AutoDisable());
        list.add(new Test());
        list.add(new ClientSounds());
        list.add(new OSEnhancements());
        list.add(new WindowSettings());

        // Combat
        list.add(new AntiBot());
        list.add(new Criticals());
        list.add(new KillAura());
        list.add(new Velocity());
        list.add(new BlockHit());
        list.add(new KeepSprint());
        list.add(new TriggerBot());

        // Movement
        list.add(new Sprint());
        list.add(new LongJump());
        list.add(new Speed());
        list.add(new MovementFix());
        list.add(new NoSlowdown());
        list.add(new FastSneak());
        list.add(new Fly());
        list.add(new Jesus());
        list.add(new NoJumpDelay());
        list.add(new SaveMoveKeys());
        list.add(new ClickTeleport());
        list.add(new NoWeb());

        // Player
        list.add(new Scaffold());
        list.add(new FastUse());
        list.add(new AntiVoid());
        list.add(new ResetVL());
        list.add(new ChestStealer());
        list.add(new InventoryManager());
        list.add(new NoFall());
        list.add(new Phase());

        // Render
        list.add(new ESP());
        list.add(new Ambience());
        list.add(new Animations());
        list.add(new Nametags());
        list.add(new Scoreboard());
        list.add(new CustomFog());
        list.add(new CustomSky());
        list.add(new CustomWorld());
        list.add(new ChestESP());

        // Misc
        list.add(Disabler.INSTANCE);
        list.add(new ClientBrand());
        list.add(new Timer());
        list.add(new AntiExploit());
        list.add(new AntiCheat());
        list.add(new AutoRegister());
        list.add(new AutoPlay());
        list.add(new StaffDetector());

        //Hud
        list.add(new Arraylist());
        list.add(new Watermark());
        list.add(new TargetHUD());
        list.add(new BlockCounter());
        list.add(new Crosshair());
        list.add(new TargetList());
        list.add(new Information());
        list.add(new Arrows());
        list.add(new TabGUI());

        for (Module mod : list) {
            this.context = mod;
            mod.setup();
        }

        this.context = null;
    }

    public List<Module> getModulesByCategory(Category input) {
        return this.list.stream().filter(mod ->
                mod.category.equals(input)).collect(Collectors.toList()
        );
    }

    public Module getModule(String input) {
        return this.list.stream()
                .filter(mod ->
                        mod.name.equalsIgnoreCase(input) ||
                                (mod.displayNames != null && Arrays.stream(mod.displayNames)
                                        .anyMatch(a -> a.equalsIgnoreCase(input)))
                )
                .findFirst()
                .orElse(null);
    }
}