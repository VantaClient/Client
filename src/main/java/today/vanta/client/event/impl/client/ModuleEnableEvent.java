package today.vanta.client.event.impl.client;

import today.vanta.client.event.Event;
import today.vanta.client.module.Module;

public class ModuleEnableEvent extends Event {
    public Module module;

    public ModuleEnableEvent(Module module) {
        this.module = module;
    }
}
