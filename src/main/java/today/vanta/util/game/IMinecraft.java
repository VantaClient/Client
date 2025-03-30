package today.vanta.util.game;

import net.minecraft.client.Minecraft;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.util.game.player.constructors.Rotation;

public interface IMinecraft {
    Minecraft mc = Minecraft.getMinecraft();

    default void setRotations(Rotation rotations, MotionEvent event) {
        event.yaw = rotations.yaw;
        event.pitch = rotations.pitch;
        mc.thePlayer.rotationYawHead = event.yaw;
        mc.thePlayer.renderYawOffset = event.yaw;
        mc.thePlayer.rotationPitchHead = event.pitch;
    }
}
