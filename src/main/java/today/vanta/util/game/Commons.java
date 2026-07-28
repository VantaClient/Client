package today.vanta.util.game;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import today.vanta.util.game.player.ChatUtil;

public interface Commons {
    Minecraft mc = Minecraft.getMinecraft();

    default void sendPacket(Packet<?> packet) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendQueue.addToSendQueue(packet);
        }
    }

    default void send(String message, Object... args) {
        ChatUtil.info(message, args);
    }
}