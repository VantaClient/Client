package today.vanta.client.module.impl.combat;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;

public class Velocity extends Module {
    private final NumberSetting
            horizontal = Setting.of("Horizontal", 0, 0, 100, "%"),
            vertical = Setting.of("Vertical", 0, 0, 100, "%");
    private final BooleanSetting
            staffSafe = Setting.of("Anti staff check", false),
            stop = Setting.of("Stop motion", false);

    public Velocity() {
        super("Velocity", "Reduces knockback.", Category.COMBAT);
        displayNames = new String[]{"Velocity", "AntiKnockback", "AntiKB"};
    }

    @EventListen
    private void onReceivePacket(ReceivePacketEvent event) {
        if (mc.thePlayer == null) return;
        if (staffSafe.getValue() && TargetProcessor.getInstance().target == null) return;
        if (event.packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity veloPacket = (S12PacketEntityVelocity) event.packet;

            if (veloPacket.getEntityID() == mc.thePlayer.getEntityId()) {
                if (horizontal.getValue().doubleValue() == 0 && vertical.getValue().doubleValue() == 0) {
                    if (stop.getValue() ) {
                        MovementUtil.stop();
                    }
                    event.cancelled = true;
                }

                veloPacket.setMotionX((int) (veloPacket.getMotionX() * (horizontal.getValue().doubleValue() / 100D)));
                veloPacket.setMotionY((int) (veloPacket.getMotionY() * (vertical.getValue().doubleValue() / 100D)));
                veloPacket.setMotionZ((int) (veloPacket.getMotionZ() * (horizontal.getValue().doubleValue() / 100D)));
            }
        }
    }

    @Override
    public String getSuffix() {
        return horizontal.getValue().intValue() + "% " + vertical.getValue().intValue() + "%";
    }
}