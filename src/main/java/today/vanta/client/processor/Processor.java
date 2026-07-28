package today.vanta.client.processor;

import today.vanta.Vanta;
import today.vanta.util.game.Commons;

public abstract class Processor implements Commons {
    public void onInitialize() {
        Vanta.instance.eventBus.register(this);
    }
}