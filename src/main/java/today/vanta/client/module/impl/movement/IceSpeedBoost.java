package today.vanta.client.module.impl.movement;

import today.vanta.client.event.impl.game.GameLoopEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.player.PlayerUtil;

public class IceSpeedBoost extends Module {
    private int tick = 0;

    public IceSpeedBoost() {
        super("IceSpeedBoost", "Boosts the vanilla speed on ice.", Category.MOVEMENT);
    }

    @EventListen
    private void onGameLoop(GameLoopEvent event) {
        if (mc.thePlayer != null) {
            if (PlayerUtil.isIceUnderneath()) {
                if (tick > 3) {
                    MovementUtil.strafe(1.05f);
                } else {
                    tick++;
                }
            } else {
                if (tick > 3) {
                    MovementUtil.stop();
                }
                tick = 0;
            }
        }
    }
}
