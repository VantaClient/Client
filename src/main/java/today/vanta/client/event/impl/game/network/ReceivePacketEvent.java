package today.vanta.client.event.impl.game.network;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import today.vanta.client.event.Event;

public class ReceivePacketEvent extends Event {
    public Packet<?> packet;
    public INetHandler iNetHandler;
    public final EnumPacketDirection direction;

    public ReceivePacketEvent(Packet<?> packet, INetHandler iNetHandler, EnumPacketDirection direction) {
        this.packet = packet;
        this.iNetHandler = iNetHandler;
        this.direction = direction;
    }
}
