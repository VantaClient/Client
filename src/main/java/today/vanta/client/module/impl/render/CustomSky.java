package today.vanta.client.module.impl.render;

import today.vanta.Vanta;
import today.vanta.client.event.impl.game.world.SkyColorEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.util.game.events.EventListen;

import java.awt.*;

public class CustomSky extends Module {
    public CustomSky() {
        super("CustomSky", "Changes the sky color.", Category.RENDER);
    }

    @EventListen
    private void onSkyColor(SkyColorEvent event) {
        Color color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        event.red = color.getRed() / 255.0F;
        event.green = color.getGreen() / 255.0F;
        event.blue = color.getBlue() / 255.0F;
    }
}
