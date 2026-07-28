package today.vanta.client.event.impl.game.player;

import today.vanta.client.event.Event;

import static today.vanta.util.game.Commons.mc;

public class MoveEvent extends Event {
    public double x, y, z;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setSpeed(double moveSpeed) {
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F) {
            this.x = 0.0D;
            this.z = 0.0D;
        } else {
            if (forward != 0.0D) {
                if (strafe > 0.0D) {
                    yaw += (float) (forward > 0.0D ? -45 : 45);
                } else if (strafe < 0.0D) {
                    yaw += (float) (forward > 0.0D ? 45 : -45);
                }
                strafe = 0.0F;
                if (forward > 0.0F) {
                    forward = 1.0F;
                } else if (forward < 0.0F) {
                    forward = -1.0F;
                }
            }

            double radianYaw = Math.toRadians(yaw + 90.0F);
            double cosYaw = Math.cos(radianYaw);
            double sinYaw = Math.sin(radianYaw);

            this.x = (double) forward * moveSpeed * cosYaw + (double) strafe * moveSpeed * sinYaw;
            this.z = (double) forward * moveSpeed * sinYaw - (double) strafe * moveSpeed * cosYaw;
        }
    }
}