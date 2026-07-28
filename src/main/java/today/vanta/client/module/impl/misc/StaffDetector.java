package today.vanta.client.module.impl.misc;

import net.minecraft.entity.player.EntityPlayer;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StaffDetector extends Module {
    private static final List<Object> STAFF = Collections.unmodifiableList(Arrays.asList(
            "joudaalt", "MineTrumps"
    ));
    private String oldTarget;
    public StaffDetector() {
        super("StaffDetector", "Detects ze staff.", Category.MISC);
    }
    @EventListen
    private void onUpdate(UpdateEvent event) {
        for (int i = 0; i < TargetProcessor.getInstance().playerlist.size(); i++) {
            EntityPlayer entityPlayer = (EntityPlayer) TargetProcessor.getInstance().playerlist.get(i);
            if (STAFF.toString().toLowerCase().contains(mc.thePlayer.getName().toLowerCase()) && !mc.thePlayer.getName().equals(oldTarget)) {
                ChatUtil.send(ChatUtil.Prefix.WARNING, entityPlayer.getName() + " Might be Staff!");
                oldTarget = mc.thePlayer.getName();
            }
        }
    }
}
