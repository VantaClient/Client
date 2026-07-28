package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.PlayerUtil;

public class Phase extends Module {
    public Phase() {
        super("Phase", "PhaSINFGG.", Category.PLAYER);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (PlayerUtil.isGlassUnderneath()) {
            mc.thePlayer.motionY += 0.42f;
        }
    }
}