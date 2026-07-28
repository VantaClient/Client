package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.MovementUtil;

public class LongJump extends Module {
    private final StringSetting mode = Setting.of("Mode", "NCP", "NCP", "Mospixel-Jump");

    private final NumberSetting
            timer = Setting.of("Timer speed", 1, 0.1, 2, 1).hide(() -> !mode.isValue("NCP")),
            groundSpeed = Setting.of("Ground speed", 0.4, 0.1, 3, 1).hide(() -> !mode.isValue("NCP")),
            airSpeed = Setting.of("Air speed", 1.4, 0.1, 3, 1).hide(() -> !mode.isValue("NCP"));

    public LongJump() {
        super("LongJump", "Makes you jump longer.", Category.MOVEMENT);
    }

    private int offGroundTicks;
    private boolean canDisable;

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state == EventState.PRE) {
            switch (mode.getValue()) {
                case "NCP":
                    if (MovementUtil.isMoving()) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                            mc.thePlayer.motionY = MovementUtil.getJumpMotion((float) (0.424 - Math.random() / 500));
                            MovementUtil.strafe(airSpeed.getValue().doubleValue());
                        }

                        if (offGroundTicks == 1)
                            MovementUtil.strafe(groundSpeed.getValue().doubleValue());

                        if (mc.thePlayer.fallDistance > 0 && mc.thePlayer.fallDistance < 3)
                            mc.thePlayer.motionY += 0.03 + Math.random() / 500;

                        mc.timer.timerSpeed = timer.getValue().floatValue();
                    } else {
                        MovementUtil.stop();
                        mc.timer.timerSpeed = 1;
                    }
                    MovementUtil.strafe();
                    break;
                case "Mospixel-Jump":
                    if (offGroundTicks > 21 && offGroundTicks < 59) {
//            MovementUtil.strafe(MovementUtil.getMovementSpeed() + 0.5f);
                        mc.thePlayer.motionY = -0.2f;
                        //if (mc.thePlayer.onGround) {
                        //   System.out.println("what");
                        //}
                        canDisable = true;
                    }

                    if (offGroundTicks == 1) {
                        canDisable = true;
                        MovementUtil.strafe(1.5f);
                    }

                    if (canDisable && mc.thePlayer.onGround) {
                        setEnabled(false);
                    }

                    break;
            }

        }
    }

    @Override
    public void onEnable() {
        offGroundTicks = 0;
        canDisable = false;

        if (mc.thePlayer == null) return;

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        mc.timer.timerSpeed = 1.0f;
        MovementUtil.stop();
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}