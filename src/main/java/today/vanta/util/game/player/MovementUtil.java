package today.vanta.util.game.player;

import net.minecraft.potion.Potion;
import today.vanta.util.game.IMinecraft;

public class MovementUtil implements IMinecraft {

    public static boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    public static void stop() {
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    public static double getSpeed() {
        if (mc.thePlayer == null) {
            return 0.0;
        }

        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            yaw += (strafe > 0.0D) ? (forward > 0.0D ? -45 : 45) : (strafe < 0.0D) ? (forward > 0.0D ? 45 : -45) : 0;
            strafe = 0.0D;
            forward = (forward > 0.0D) ? 1.0D : -1.0D;
        }

        if (strafe != 0.0D) {
            strafe = (strafe > 0.0D) ? 1.0D : -1.0D;
        }

        double radianYaw = Math.toRadians(yaw + 90.0F);
        double cosYaw = Math.cos(radianYaw);
        double sinYaw = Math.sin(radianYaw);

        mc.thePlayer.motionX = forward * moveSpeed * cosYaw + strafe * moveSpeed * sinYaw;
        mc.thePlayer.motionZ = forward * moveSpeed * sinYaw - strafe * moveSpeed * cosYaw;
    }

    public static void setSpeed(double moveSpeed) {
        setSpeed(moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(double moveSpeed) {
        if (mc.thePlayer.movementInput.moveForward != 0.0) {
            mc.thePlayer.movementInput.moveForward = (mc.thePlayer.movementInput.moveForward > 0.0) ? 1.0f : -1.0f;
        }

        if (mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveStrafe = (mc.thePlayer.movementInput.moveStrafe > 0.0) ? 1.0f : -1.0f;
        }

        if (mc.thePlayer.movementInput.moveForward == 0.0 && mc.thePlayer.movementInput.moveStrafe == 0.0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            return;
        }

        if (mc.thePlayer.movementInput.moveForward != 0.0 && mc.thePlayer.movementInput.moveStrafe != 0.0) {
            mc.thePlayer.movementInput.moveForward *= (float) Math.sin(Math.toRadians(36.67));
            mc.thePlayer.movementInput.moveStrafe *= (float) Math.cos(Math.toRadians(36.67));
        }

        double yawRadians = Math.toRadians(mc.thePlayer.rotationYaw);
        mc.thePlayer.motionX = mc.thePlayer.movementInput.moveForward * moveSpeed * -Math.sin(yawRadians)
                + mc.thePlayer.movementInput.moveStrafe * moveSpeed * Math.cos(yawRadians);
        mc.thePlayer.motionZ = mc.thePlayer.movementInput.moveForward * moveSpeed * Math.cos(yawRadians)
                - mc.thePlayer.movementInput.moveStrafe * moveSpeed * -Math.sin(yawRadians);
    }

    public static double getJumpMotion(float motionY) {
        Potion potion = Potion.jump;

        if (mc.thePlayer.isPotionActive(potion)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(potion).getAmplifier();
            motionY += (amplifier + 1) * 0.1F;
        }

        return motionY;
    }
}
