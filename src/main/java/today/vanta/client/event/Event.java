package today.vanta.client.event;

import today.vanta.Vanta;
import today.vanta.util.game.events.EventState;

public class Event {
    public boolean cancelled = false;
    public EventState state = EventState.PRE;

    public void call() {
        Vanta.instance.eventBus.call(this);
    }
}
