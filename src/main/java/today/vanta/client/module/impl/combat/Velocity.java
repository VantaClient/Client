package today.vanta.client.module.impl.combat;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;

public class Velocity extends Module {
    public Velocity() {
        super("Velocity", "Reduces knockback.", Category.COMBAT);
        displayNames = new String[] {"Velocity", "AntiKnockback", "AntiKB", "Anti Knockback", "Anti KB"};
    }

    private final NumberSetting
    horizontal = NumberSetting.builder()
            .name("Horizontal")
            .value(0)
            .max(100)
            .min(0)
            .build(),

    vertical = NumberSetting.builder()
            .name("Vertical")
            .value(0)
            .max(100)
            .min(0)
            .build();

    @EventListen
    private void onPacket(ReceivePacketEvent event) {
        if (event.packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity veloPacket = (S12PacketEntityVelocity) event.packet;

            if (veloPacket.getEntityID() == mc.thePlayer.getEntityId()) {
                if (horizontal.getValue().doubleValue() == 0 && vertical.getValue().doubleValue() == 0) {
                    event.cancelled = true;
                }

                veloPacket.setMotionX((int) (veloPacket.getMotionX() * (horizontal.getValue().doubleValue() / 100D)));
                veloPacket.setMotionY((int) (veloPacket.getMotionY() * (vertical.getValue().doubleValue() / 100D)));
                veloPacket.setMotionZ((int) (veloPacket.getMotionZ() * (horizontal.getValue().doubleValue() / 100D)));
            }
        }
    }
}
