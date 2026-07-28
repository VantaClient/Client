package today.vanta.client.module.impl.movement;

import net.minecraft.item.*;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.player.SlowdownEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;

public class NoSlowdown extends Module {
    private final MultiStringSetting items = Setting.of("Items", new String[]{"Swords"}, new String[]{"Swords", "Bows", "Consumables"});

    private final NumberSetting
            forwardMultiplier = Setting.of("Forward multiplier", 1, 0, 1, 2),
            strafeMultiplier = Setting.of("Strafe multiplier", 1, 0, 1, 2);

    private final BooleanSetting
            shouldSprint = Setting.of("Should sprint", true);

    public NoSlowdown() {
        super("NoSlowdown", "Removes the slowdown effect when using an item.", Category.MOVEMENT);
        displayNames = new String[]{"NoSlowdown", "NoSlow", "NoSlowDown"};
    }

    @EventListen
    private void onSlowdown(SlowdownEvent event) {
        ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();

        if (currentItem == null || !mc.thePlayer.isUsingItem() || !MovementUtil.isMoving()) {
            return;
        }

        if ((items.isEnabled("Swords") && currentItem.getItem() instanceof ItemSword) ||
                (items.isEnabled("Consumables") && (currentItem.getItem() instanceof ItemFood || currentItem.getItem() instanceof ItemPotion)) ||
                (items.isEnabled("Bows") && currentItem.getItem() instanceof ItemBow)) {
            if (!shouldSprint.getValue()) {
                mc.gameSettings.keyBindSprint.pressed = false;
                mc.thePlayer.setSprinting(false);
            } else {
                mc.gameSettings.keyBindSprint.pressed = true;
                mc.thePlayer.setSprinting(true);
            }

            event.forward = forwardMultiplier.getValue().floatValue();
            event.strafe = strafeMultiplier.getValue().floatValue();
        }
    }

    @Override
    public void onDisable() {
        if (Vanta.instance.moduleStorage.getT(Sprint.class).isEnabled()) {
            mc.gameSettings.keyBindSprint.pressed = true;
        }
    }
}