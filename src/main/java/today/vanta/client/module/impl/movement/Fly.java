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
    private final StringSetting mode = Setting.of("Mode", "Vanilla", "Vanilla", "Miniblox", "Teleport", "Jump");

    private final NumberSetting distance = Setting.of("TP distance", 3, 0, 10, "m").hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting ticks = Setting.of("TP ticks", 10, 1, 20).hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting viewBobbing = Setting.of("View-bob amount", 60.0f,0.0f,100f);

    private final Counter jumpCounter = new Counter();

    private double prevposY;

    public Fly() {
        super("Fly", "Allows you to fly like a pelican.", Category.MOVEMENT);
        displayNames = new String[]{"Fly", "Flight", "AirWalk", "AirJump"};
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (MovementUtil.isMoving()) {
            mc.thePlayer.cameraYaw = viewBobbing.getValue().floatValue() / 1000.0F;
        }
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

    @EventListen
    private void onMotion(MotionEvent event) {
        if (mode.isValue("Miniblox")) {
            // dont work xd
//                mc.gameSettings.keyBindSneak.pressed = true;
            MovementUtil.strafe(0.15f);
            if (mc.thePlayer.posY <= prevposY) {
                mc.thePlayer.jump();
            }
        }
        if (event.state == EventState.PRE) {
            switch (mode.getValue()) {
                case "Teleport":
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
                    break;
            }
        }
    }

    @EventListen
    private void onMove(MoveEvent event) {
        if (mode.isValue("Teleport")) {
            event.setSpeed(0);
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        prevposY = mc.thePlayer.posY;

    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}