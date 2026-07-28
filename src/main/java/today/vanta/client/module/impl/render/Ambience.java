package today.vanta.client.module.impl.render;

import net.minecraft.network.play.server.S03PacketTimeUpdate;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;

public class Ambience extends Module {
    private final NumberSetting time = Setting.of("Time", 1000, 0, 24000);
    private final BooleanSetting fullbright = Setting.of("Fullbright", false);

    private float oldGamma = 1.0f;

    public Ambience() {
        super("Ambience", "Modifies the game's time & visibility.", Category.RENDER);

        fullbright.addListener((fb, oldV, newV) -> {
            if (newV == false)
                mc.gameSettings.gammaSetting = oldGamma;
        });
    }

    @EventListen
    private void onReceivePacket(ReceivePacketEvent event) {
        if (mc.theWorld == null) return;
        if (event.packet instanceof S03PacketTimeUpdate) {
            event.cancelled = true;
        }
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        mc.theWorld.setWorldTime(time.getValue().longValue());

        if (fullbright.getValue()) {
            mc.gameSettings.gammaSetting = 100;
        }
    }

    @Override
    public void onEnable() {
        oldGamma = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = oldGamma;
    }
}