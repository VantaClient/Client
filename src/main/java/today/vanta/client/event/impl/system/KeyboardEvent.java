package today.vanta.client.event.impl.system;

import today.vanta.client.event.Event;

public class KeyboardEvent extends Event {
    public final int key;

    public KeyboardEvent(int key) {
        this.key = key;
    }
}
