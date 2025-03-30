package today.vanta.client.event.impl.game.player;

import net.minecraft.util.Vec3;
import today.vanta.client.event.Event;

public class RotationLookEvent extends Event {
    public Vec3 rotationVector;
    public final float partialTicks;

    public RotationLookEvent(Vec3 rotationVector, float partialTicks) {
        this.rotationVector = rotationVector;
        this.partialTicks = partialTicks;
    }
}
