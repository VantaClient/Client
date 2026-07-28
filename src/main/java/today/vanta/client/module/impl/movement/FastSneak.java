package today.vanta.client.module.impl.movement;

import net.minecraft.network.play.client.C0BPacketEntityAction;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.event.impl.game.player.SneakInputEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;

public class FastSneak extends Module {
    private final StringSetting mode = Setting.of("Mode", "Vanilla", "Vanilla", "Packet");
    public FastSneak() {
        super("FastSneak","Removes sneaking slowdown.", Category.MOVEMENT);
    }

    @EventListen
    private void onSneakInput(SneakInputEvent event) {
        event.cancelled = true;
    }

    @EventListen
    private void onSendPacket(SendPacketEvent event) {
        if (mc.thePlayer == null) return;
        if (mode.isValue("Packet")) {
            if (event.packet instanceof C0BPacketEntityAction) {
                if (((C0BPacketEntityAction) event.packet).getAction() == C0BPacketEntityAction.Action.START_SNEAKING) {
                    event.cancelled = true;
                }
            }
        }
    }
}