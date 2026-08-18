package today.vanta.client.module.impl.movement;

import org.lwjgl.input.Keyboard;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;

public class HighJump extends Module {
    private final NumberSetting motion = Setting.of("Motion", 0.85f,0.0f,2f,2);
    private final BooleanSetting onlyOnJump = Setting.of("Only on jump", true);
    private final BooleanSetting toggle = Setting.of("Toggle off after", true);
    private boolean hasJumped;
    private boolean canDisable;
    public HighJump() {
        super("HighJump", "Jumps high.", Category.MOVEMENT);
    }

    private boolean canJump() {
        int jumpKey = mc.gameSettings.keyBindJump.getKeyCode();
        if (onlyOnJump.getValue()) {
            if (Keyboard.isKeyDown(jumpKey)) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        mc.gameSettings.keyBindJump.pressed = false;
        if (!mc.thePlayer.onGround && hasJumped) {
            canDisable = true;
        }

        if (mc.thePlayer.onGround && canDisable) {
            hasJumped = false;
            if (toggle.getValue()) super.setEnabled(false);
            canDisable = false;
        }
        if (mc.thePlayer.onGround && canJump() && !canDisable) {
            mc.thePlayer.motionY += motion.getValue().doubleValue();
            hasJumped = true;
        }

    }

    @Override
    public void onDisable() {
        canDisable = false;
        hasJumped = false;
    }
}
