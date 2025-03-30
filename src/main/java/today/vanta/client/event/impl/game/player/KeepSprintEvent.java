package today.vanta.client.event.impl.game.player;

import today.vanta.client.event.Event;

public class KeepSprintEvent extends Event {
    public boolean greater;

    public KeepSprintEvent(boolean greater) {
        this.greater = greater;
    }
}
