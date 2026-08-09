package today.vanta.client.module.impl.misc;

import net.minecraft.entity.player.EntityPlayer;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;

import java.util.Arrays;
import java.util.List;

public class StaffDetector extends Module {
    private static final List<String> MINIBLOX_STAFF = Arrays.asList(
            "joudaalt", "MineTrumps", "Bob"
    );

    private String oldTarget;

    public StaffDetector() {
        super("StaffDetector", "Detects ze staff.", Category.MISC);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        for (EntityPlayer player : TargetProcessor.getInstance().playerlist) {
            if (MINIBLOX_STAFF.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(player.getName()))
                    && !player.getName().equalsIgnoreCase(oldTarget)) {

                ChatUtil.send(ChatUtil.Prefix.WARNING,
                        player.getName() + " might be staff!");

                oldTarget = player.getName();
            }
        }
    }
}
