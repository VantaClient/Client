package today.vanta.client.event.impl.game.player;

import today.vanta.client.event.Event;

public class EntityMotionEvent extends Event {
    public double motion;

    public EntityMotionEvent(double motion) {
        this.motion = motion;
    }
}
