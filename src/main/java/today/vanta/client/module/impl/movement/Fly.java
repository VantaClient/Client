package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.MoveEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.system.math.Counter;

public class Fly extends Module {
    private final StringSetting mode = Setting.of("Mode", "Vanilla", "Vanilla", "Teleport", "Jump");

    private final NumberSetting distance = Setting.of("TP distance", 3, 0, 10, "m").hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting ticks = Setting.of("TP ticks", 10, 1, 20).hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting viewBobbing = Setting.of("View-bob amount", 60.0f,0.0f,100f);
    private final NumberSetting timer = Setting.of("Timer", 10, 0.1, 100, 2)
            .hide(() -> mode.isValue("Teleport"));

    private final Counter jumpCounter = new Counter();

    public Fly() {
        super("Fly", "Allows you to fly like a pelican.", Category.MOVEMENT);
        displayNames = new String[]{"Fly", "Flight", "AirWalk", "AirJump"};
    }

    @SuppressWarnings("unused")
    @EventListen
    private void onUpdate(UpdateEvent ignored) {
        if (MovementUtil.isMoving()) {
            mc.thePlayer.cameraYaw = viewBobbing.getValue().floatValue() / 1000.0F;
        }
        mc.timer.timerSpeed = timer.getValue().floatValue();
        switch (mode.getValue()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0f;
                MovementUtil.strafe(1f);
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.thePlayer.motionY = 1f;
                }

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.thePlayer.motionY = -1f;
                }
                break;
            case "Jump":
                if (jumpCounter.hasElapsed(540, true)) {
                    mc.thePlayer.jump();
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state == EventState.PRE) {
            if (mode.isValue("Teleport")) {
                mc.thePlayer.motionY = 0;

                if ((mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0)
                        && mc.thePlayer.ticksExisted % ticks.getValue().intValue() == 0) {

                    double distance = this.distance.getValue().intValue();

                    mc.thePlayer.setPosition(
                            mc.thePlayer.posX - Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)) * distance,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)) * distance
                    );
                }
            }
        }
    }

    @EventListen
    @SuppressWarnings("unused")
    private void onMove(MoveEvent event) {
        if (mode.isValue("Teleport")) event.setSpeed(0);
    }

    @Override
    public void onEnable() {
        mc.timer.timerSpeed = timer.getValue().floatValue();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}