package today.vanta.client.module.impl.hud;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.util.game.events.EventListen;

public class TargetHUDRecode extends Module {
    private EntityPlayer entityPlayer;
    private int state;
    private int ANIMATE_IN = 1;
    private int ANIMATE_OUT = 2;
    private int STANDBY = 3;
    private boolean drawn;
    private float entityHealth;
    private float scale;
    public TargetHUDRecode() {
        super("TargetHUDRecode", "TargetHUD module but recoded.", Category.RENDER);
    }

    private void checkState() {
        if (TargetProcessor.getInstance().target instanceof EntityPlayer) {
            entityPlayer = (EntityPlayer) TargetProcessor.getInstance().target;
        }

        if (entityPlayer.isDead) {
            state = ANIMATE_OUT;
        }
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent e) {
        checkState();
        if (entityPlayer != null) {
        }
    }
}
