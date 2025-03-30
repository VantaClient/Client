package today.vanta.client.processor.impl;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import today.vanta.client.event.impl.game.player.EntityMotionEvent;
import today.vanta.client.event.impl.game.player.SlowdownEvent;
import today.vanta.client.event.impl.game.render.SwordItemActionEvent;
import today.vanta.client.event.impl.game.world.EntityCollisionBorderSizeEvent;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.player.MovementUtil;

public class VersionProcessor extends Processor {
    public boolean isNative() {
        return ViaLoadingBase.getInstance().getTargetVersion().equalTo(ProtocolVersion.v1_8);
    }

    @EventListen
    private void onSlowdown(SlowdownEvent event) {
        ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();

        if (currentItem == null || !mc.thePlayer.isUsingItem() || !MovementUtil.isMoving()) {
            return;
        }

        if (currentItem.getItem() instanceof ItemSword && !isNative()) {
            event.forward = 1.0f;
            event.strafe = 1.0f;
        }
    }

    @EventListen
    private void onEntityCollisionBorder(EntityCollisionBorderSizeEvent event) {
        if (!isNative()) {
            event.size = 0.03f;
        }
    }

    @EventListen
    private void onSwordItemAction(SwordItemActionEvent event) {
        if (!isNative()) {
            event.enumAction = EnumAction.NONE;
        }
    }

    @EventListen
    private void onEntityMotion(EntityMotionEvent event) {
        if (!isNative()) {
            event.motion = 0.003D;
        }
    }
}
