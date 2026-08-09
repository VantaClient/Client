package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;

public class Spider extends Module {
    public Spider() {
        super("Spider", "Allows you to climb walls.", Category.MOVEMENT);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.motionY += 0.2f;
        } else {
            mc.thePlayer.motionY -= 0.2f;
        }
    }
}