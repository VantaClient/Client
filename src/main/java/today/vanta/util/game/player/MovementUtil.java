package today.vanta.util.game.player;

import net.minecraft.potion.Potion;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.player.MoveInputEvent;
import today.vanta.client.module.impl.movement.Fly;
import today.vanta.client.module.impl.movement.LongJump;
import today.vanta.storage.impl.ModuleStorage;
import today.vanta.util.game.Commons;

public class MovementUtil implements Commons {
    public static boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    public static void stop() {
        if (mc.thePlayer == null) {return;}
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

    public static void correctMovement(MoveInputEvent event, float yaw) {
        if (event.forward == 0 && event.strafe == 0) {
            return;
        }

        float realYaw = mc.thePlayer.rotationYaw;

        float moveX = event.strafe * (float) Math.cos(Math.toRadians(realYaw)) - event.forward * (float) Math.sin(Math.toRadians(realYaw));
        float moveZ = event.forward * (float) Math.cos(Math.toRadians(realYaw)) + event.strafe * (float) Math.sin(Math.toRadians(realYaw));

        double[] bestMovement = null;

        for (int forward = -1; forward <= 1; forward++) {
            for (int strafe = -1; strafe <= 1; strafe++) {
                if (forward == 0 && strafe == 0) continue;

                float newMoveX = strafe * (float) Math.cos(Math.toRadians(yaw)) - forward * (float) Math.sin(Math.toRadians(yaw));
                float newMoveZ = forward * (float) Math.cos(Math.toRadians(yaw)) + strafe * (float) Math.sin(Math.toRadians(yaw));

                float deltaX = newMoveX - moveX;
                float deltaZ = newMoveZ - moveZ;

                double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (bestMovement == null || bestMovement[0] > dist) {
                    bestMovement = new double[]{dist, forward, strafe};
                }
            }
        }

        event.forward = (float) Math.round(bestMovement[1]);
        event.strafe = (float) Math.round(bestMovement[2]);
    }


    public static double getBPS() {
        double bps = (Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX, mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * mc.timer.timerSpeed) * 20;
        return Math.round(bps * 100.0) / 100.0;
        // lost my calculation so i used the tenacity one :D
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = mc.thePlayer.isSprinting() ? 0.28630206268501246 : 0.2202643217126144;
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() +
                    1 - (mc.thePlayer.isPotionActive(Potion.moveSlowdown) ? mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1 : 0);
            baseSpeed *= 1.0 + 0.2 * amplifier;
        }

        return baseSpeed;
    }

    public static boolean movementModuleEnabled() {
        ModuleStorage moduleStorage = Vanta.instance.moduleStorage;
        if (moduleStorage.getT(LongJump.class).isEnabled() || moduleStorage.getT(Fly.class).isEnabled()) {
            return true;
        }
        return false;
    }
    public static void blockMovement() {
        mc.gameSettings.keyBindRight.pressed = false;
        mc.gameSettings.keyBindLeft.pressed = false;
        mc.gameSettings.keyBindForward.pressed = false;
        mc.gameSettings.keyBindBack.pressed = false;
        mc.gameSettings.keyBindJump.pressed = false;
    }
}