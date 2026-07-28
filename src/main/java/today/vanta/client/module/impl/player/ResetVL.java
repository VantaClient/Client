package today.vanta.client.module.impl.player;

import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.system.math.Counter;

public class ResetVL extends Module {
    private final Counter counter = new Counter();

    public ResetVL() {
        super("ResetVL", "Resets some anticheat violations level.", Category.PLAYER);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        MovementUtil.blockMovement();
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.2f;
        }

        if (counter.hasElapsed(9000)) {
            setEnabled(false);
        }

        ChatUtil.send(ChatUtil.Prefix.INFO, String.valueOf(counter.getElapsedTime()));
    }

    @Override
    public void onEnable() {
        counter.reset();
        if (mc.thePlayer == null) return;
        MovementUtil.stop();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        MovementUtil.stop();
    }
}