package today.vanta.client.event.impl.client;

import today.vanta.client.event.Event;
import today.vanta.client.module.Module;

public class ModuleDisableEvent extends Event {
    public Module module;

    public ModuleDisableEvent(Module module) {
        this.module = module;
    }
}
