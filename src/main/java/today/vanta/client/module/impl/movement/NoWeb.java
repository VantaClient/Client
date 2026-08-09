package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.player.SlowdownEvent;
import today.vanta.client.event.impl.game.player.WebSlowdownEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;

public class NoWeb extends Module {
    private StringSetting mode = Setting.of("Mode", "Cancel", "Cancel", "Strafe");
    private NumberSetting motion = Setting.of("Strafe motion", 0.2f,0,0.2f,3);
    public NoWeb() {
        super("NoWeb", "Prevents you from being slowed down in cobwebs.", Category.MOVEMENT);
    }

    @EventListen
    private void onSlowdown(SlowdownEvent event) {
        if (mc.thePlayer.isInWeb) {
            if (mode.isValue("Strafe")) {
                MovementUtil.strafe(motion.getValue().floatValue());
            }
            mc.thePlayer.isInWeb = false;
        }
    }

    @EventListen
    private void onWebSlowdown(WebSlowdownEvent event) {
        event.cancelled = true;
    }
}
