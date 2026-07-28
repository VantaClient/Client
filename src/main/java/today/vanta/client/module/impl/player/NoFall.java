package today.vanta.client.module.impl.player;

import net.minecraft.network.play.client.C03PacketPlayer;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;

public class NoFall extends Module {
    private final StringSetting mode = Setting.of("Mode", "Packet", "Packet", "Set ground", "No ground");
    private final BooleanSetting forceDistance = Setting.of("Force distance", false);

    public NoFall() {
        super("NoFall", "No fall damage.", Category.PLAYER);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.fallDistance < 3) return;

        switch (mode.getValue()) {
            case "Packet":
                sendPacket(new C03PacketPlayer(true));
                break;
            case "Set ground":
                mc.thePlayer.onGround = true;
                break;
            case "No ground":
                mc.thePlayer.onGround = false;
                break;
        }

        if (forceDistance.getValue()) {
            mc.thePlayer.fallDistance = 0;
        }
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}