package today.vanta.client.event.impl.game.player;

import today.vanta.client.event.Event;
import today.vanta.util.game.events.EventState;

public class MotionEvent extends Event {
    public float yaw, pitch;
    public double x, y, z;
    public boolean onGround;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
