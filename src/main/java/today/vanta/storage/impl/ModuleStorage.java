package today.vanta.storage.impl;

import today.vanta.client.event.impl.system.KeyboardEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.*;
import today.vanta.client.module.impl.hud.*;
import today.vanta.client.module.impl.movement.*;
import today.vanta.client.module.impl.combat.*;
import today.vanta.client.module.impl.player.*;
import today.vanta.storage.Storage;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleStorage extends Storage<Module> {
    public Module context;

    public List<String> changelog = new ArrayList<>();

    public ModuleStorage() {
        changelog.add("[+] Added 'Skin color' sorting to KillAura");
        changelog.add("[+] Added AntiBot module");
        changelog.add("[+] Added Speed[ONCP, NCP] module");
        changelog.add("[+] Added Scaffold[ONCP, NCP] module");
        changelog.add("[+] Added Velocity[Basic] module");
    }

    @Override
    public void subscribe() {
        super.subscribe();

        // Client
        list.add(new ClickGUI());
        list.add(new Theme());

        // Combat
        list.add(new AntiBot());
        list.add(new KillAura());
        list.add(new Velocity());

        // Movement
        list.add(new Sprint());
        list.add(new LongJump());
        list.add(new Speed());

        // Player
        list.add(new Scaffold());

        // Misc

        //Hud
        list.add(new Arraylist());
        list.add(new Watermark());

        this.context = null;
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onKey(KeyboardEvent event) {
        list.forEach(mod -> {
            if (event.key == mod.key) {
                mod.setEnabled(!mod.isEnabled());
            }
        });
    }

    public List<Module> getModulesByCategory(Category input) {
        return this.list.stream().filter(mod ->
                mod.category.equals(input)).collect(Collectors.toList()
        );
    }

    public Module getModule(String input) {
        return this.list.stream().filter(m -> m.name.equalsIgnoreCase(input)).findFirst().orElse(null);
    }

}
