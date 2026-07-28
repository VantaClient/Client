package today.vanta.client.processor.impl;

import net.minecraft.util.MathHelper;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.FrameEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.RotationLookEvent;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.RotationUtil;
import today.vanta.util.game.player.constructors.Rotation;

public class RotationProcessor extends Processor {
    public Rotation rotations;

    private Rotation targetRotation;
    private Rotation currentRotation;
    private Rotation returnRotation;
    private Rotation lastSentRotation;

    private enum RotateState {
        INACTIVE, AIMING, RETURNING
    }

    private RotateState state = RotateState.INACTIVE;

    private int lastUpdateTick;

    @EventListen(priority = EventPriority.HIGHEST)
    private void onFrame(FrameEvent event) {
        if (mc.thePlayer == null) {
            state = RotateState.INACTIVE;
            return;
        }

        if (state == RotateState.INACTIVE) {
            return;
        }

        currentRotation = targetRotation;
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || event.state == EventState.POST) {
            rotations = null;
            return;
        }

        if (state == RotateState.INACTIVE) {
            rotations = new Rotation(event.yaw, event.pitch);
            return;
        }

        if (currentRotation == null || lastSentRotation == null) return;

        float currentYaw = currentRotation.yaw;
        while (currentYaw - lastSentRotation.yaw > 180) currentYaw -= 360;
        while (currentYaw - lastSentRotation.yaw < -180) currentYaw += 360;

        float yawDelta  = currentYaw - lastSentRotation.yaw;
        float pitchDelta = (float) ((double) currentRotation.pitch - lastSentRotation.pitch);

        Rotation raw = new Rotation(
                lastSentRotation.yaw + yawDelta,
                lastSentRotation.pitch + pitchDelta
        );

        Rotation gcdRot = RotationUtil.gcd(raw, lastSentRotation);

        if (Float.isNaN(gcdRot.yaw) || Float.isNaN(gcdRot.pitch)) {
            event.yaw = currentRotation.yaw;
            event.pitch = currentRotation.pitch;
        } else {
            event.yaw = gcdRot.yaw;
            event.pitch = gcdRot.pitch;
        }

        lastSentRotation = new Rotation(event.yaw, event.pitch);

        mc.thePlayer.renderPitchHead = currentRotation.pitch;
        mc.thePlayer.rotationYawHead = currentRotation.yaw;

        if (state == RotateState.AIMING && mc.thePlayer.ticksExisted - lastUpdateTick > 1 && isClose(currentRotation, targetRotation, 0.4f)) {
            state = RotateState.RETURNING;
            targetRotation = returnRotation;
        } else if (state == RotateState.RETURNING && isClose(currentRotation, returnRotation, 0.1f)) {
            state = RotateState.INACTIVE;
            targetRotation = null;
            currentRotation = null;
            returnRotation = null;
        }
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onLook(RotationLookEvent event) {
        if (state != RotateState.INACTIVE && currentRotation != null) {
            event.rotationVector = RotationUtil.getVectorForRotation(currentRotation.pitch, currentRotation.yaw);
        } else if (rotations != null) {
            event.rotationVector = RotationUtil.getVectorForRotation(rotations.pitch, rotations.yaw);
        }
    }

    public void setTargetRotation(Rotation target) {
        if (mc.thePlayer == null || target == null) {
            return;
        }

        this.targetRotation = target;
        this.currentRotation = target;
        this.lastUpdateTick = mc.thePlayer.ticksExisted;
        this.returnRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

        if (state == RotateState.INACTIVE || lastSentRotation == null) {
            this.lastSentRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        this.state = RotateState.AIMING;
    }

    public Rotation getCurrentRotation() {
        if (this.currentRotation != null) {
            return this.currentRotation;
        }
        if (mc.thePlayer != null) {
            return new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        return new Rotation(0, 0);
    }

    public boolean isActive() {
        return this.state != RotateState.INACTIVE;
    }

    private static boolean isClose(Rotation a, Rotation b, float threshold) {
        float deltaYaw = Math.abs(MathHelper.wrapAngleTo180_float(a.yaw - b.yaw));
        float deltaPitch = Math.abs(a.pitch - b.pitch);
        return deltaYaw < threshold && deltaPitch < threshold;
    }

    public static RotationProcessor getInstance() {
        return Vanta.instance.processorStorage.getT(RotationProcessor.class);
    }
}
