package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.MovementUtil;

public class LongJump extends Module {
    private final StringSetting mode = StringSetting.builder()
            .name("Mode")
            .value("NCP")
            .values("NCP")
            .build();

    private final NumberSetting timer = NumberSetting.builder()
            .name("Timer speed")
            .value(1)
            .min(0.1)
            .max(2)
            .places(1)
            .build()
            .hide(() -> !mode.getValue().equals("NCP"));

    private final NumberSetting groundSpeed = NumberSetting.builder()
            .name("Ground speed")
            .value(0.4)
            .min(0.1)
            .max(3)
            .places(1)
            .build()
            .hide(() -> !mode.getValue().equals("NCP"));

    private final NumberSetting airSpeed = NumberSetting.builder()
            .name("Air speed")
            .value(1.4)
            .min(0.1)
            .max(3)
            .places(1)
            .build()
            .hide(() -> !mode.getValue().equals("NCP"));

    public LongJump() {
        super("LongJump", "Makes you jump longer.", Category.MOVEMENT);
        displayNames = new String[] {"LongJump", "Long Jump"};
    }

    private int offGroundTicks;

    @EventListen
    private void onMotion(MotionEvent event) {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }

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
            }

        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        offGroundTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.thePlayer == null) {
            return;
        }

        mc.timer.timerSpeed = 1.0f;
        MovementUtil.stop();
    }
}
