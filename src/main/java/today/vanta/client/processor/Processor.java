package today.vanta.client.processor;

import today.vanta.Vanta;
import today.vanta.util.game.IMinecraft;

public abstract class Processor implements IMinecraft {
    public void onInitialize() {
        Vanta.instance.eventBus.register(this);
    }
}
