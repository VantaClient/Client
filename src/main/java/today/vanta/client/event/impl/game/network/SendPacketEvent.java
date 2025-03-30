package today.vanta.client.event.impl.game.network;

import net.minecraft.network.Packet;
import today.vanta.client.event.Event;

public class SendPacketEvent extends Event {
    public Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
