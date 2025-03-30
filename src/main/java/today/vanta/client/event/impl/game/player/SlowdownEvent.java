package today.vanta.client.event.impl.game.player;

import today.vanta.client.event.Event;

public class SlowdownEvent extends Event {
    public float strafe, forward;

    public SlowdownEvent(float strafe, float forward) {
        this.strafe = strafe;
        this.forward = forward;
    }
}
