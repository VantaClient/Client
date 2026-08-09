package today.vanta.client.event.impl.game.render;

import net.minecraft.client.renderer.ItemRenderer;
import today.vanta.client.event.Event;

public class PerformBlockEvent extends Event {
    public final ItemRenderer renderer;
    public final float partialTicks;
    public float equippedProgress, swingProgress, prevEquippedProgress;

    public PerformBlockEvent(ItemRenderer renderer, float partialTicks, float equippedProgress, float prevEquippedProgress, float swingProgress) {
        this.renderer = renderer;
        this.partialTicks = partialTicks;
        this.equippedProgress = equippedProgress;
        this.prevEquippedProgress = equippedProgress;
        this.swingProgress = swingProgress;
    }
}