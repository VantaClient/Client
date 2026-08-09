package today.vanta.client.module.impl.render;

import today.vanta.Vanta;
import today.vanta.client.event.impl.game.render.FogColorEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.util.game.events.EventListen;

import java.awt.*;

public class CustomFog extends Module {
    public CustomFog() {
        super("CustomFog", "Changes the fog color.", Category.RENDER);
    }

    @EventListen
    private void onFogColor(FogColorEvent event) {
        Color color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        event.red = color.getRed() / 255.0F;
        event.green = color.getGreen() / 255.0F;
        event.blue = color.getBlue() / 255.0F;
    }
}
