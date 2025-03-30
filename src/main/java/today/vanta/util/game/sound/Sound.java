package today.vanta.util.game.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class Sound {
    private final ResourceLocation resourceLocation;

    public Sound(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public void play() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(resourceLocation, 1.0F));
    }

    public void play(float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(resourceLocation, pitch));
    }
}
