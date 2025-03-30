package today.vanta.client.processor.impl;

import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.RotationLookEvent;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.player.RotationUtil;
import today.vanta.util.game.player.constructors.Rotation;

public class RotationProcessor extends Processor {

    private Rotation rotations;

    @EventListen(priority = EventPriority.HIGHEST)
    private void onMotion(MotionEvent event) {
        if (mc.thePlayer != null) {
            rotations = new Rotation(event.yaw, event.pitch);
        } else {
            rotations = null;
        }
    }


    @EventListen(priority = EventPriority.HIGHEST)
    private void onLook(RotationLookEvent event) { //sets the server-side mouse over object to the rotations
        if (rotations != null) {
            event.rotationVector = RotationUtil.getVectorForRotation(rotations.pitch, rotations.yaw);
        }
    }
}
