package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.player.SlowdownEvent;
import today.vanta.client.event.impl.game.player.WebSlowdownEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;

public class NoWeb extends Module {
    public NoWeb() {
        super("NoWeb", "Prevents you from being slowed down in cobwebs.", Category.MOVEMENT);
    }

    @EventListen
    private void onSlowdown(SlowdownEvent event) {
        if (mc.thePlayer.isInWeb) {
            mc.thePlayer.isInWeb = false;
        }
    }

    @EventListen
    private void onWebSlowdown(WebSlowdownEvent event) {
        event.cancelled = true;
    }
}
