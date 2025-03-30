package today.vanta.client.event.impl.game.render;

import net.minecraft.item.EnumAction;
import today.vanta.client.event.Event;

public class SwordItemActionEvent extends Event {
    public EnumAction enumAction;

    public SwordItemActionEvent(EnumAction enumAction) {
        this.enumAction = enumAction;
    }
}
