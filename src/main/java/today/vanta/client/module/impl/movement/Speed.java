package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;

public class Speed extends Module {
    private final StringSetting
    mode = StringSetting.builder()
            .name("Mode")
            .value("OldNCP")
            .values("OldNCP", "NCP")
            .build(),

    oncpmode = StringSetting.builder()
            .name("ONCP mode")
            .value("Y-Port")
            .values("Y-Port", "Strafe")
            .build()
            .hide(() -> !mode.getValue().equals("OldNCP"));

    public Speed() {
        super("Speed", "Makes you go faster.", Category.MOVEMENT);
        displayNames = new String[]{"Speed", "FastMove", "Fast Move"};
    }

    @EventListen
    public void onUpdate(UpdateEvent event) {
        if (MovementUtil.isMoving()) {
            mc.gameSettings.keyBindSprint.pressed = true;

            switch (mode.getValue()) {
                case "OldNCP":
                    switch (oncpmode.getValue()) {
                        case "Y-Port":
                            if (mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                                MovementUtil.strafe(0.51);
                            } else {
                                mc.thePlayer.motionY -= 0.16;
                            }
                            break;
                        case "Strafe":
                            if (mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                                MovementUtil.strafe(0.32);
                            }
                            break;
                    }
                    break;
                case "NCP":
                    mc.gameSettings.keyBindJump.pressed = MovementUtil.isMoving();
                    MovementUtil.strafe();
                    break;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.gameSettings.keyBindSprint.pressed = false;
        mc.gameSettings.keyBindJump.pressed = false;
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}
