package today.vanta.client.event.impl.game.render;

import net.minecraft.client.gui.GuiScreen;
import today.vanta.client.event.Event;

public class DisplayGuiScreenEvent extends Event {
    public GuiScreen screen;

    public DisplayGuiScreenEvent(GuiScreen screen) {
        this.screen = screen;
    }
}
