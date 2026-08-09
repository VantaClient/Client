package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class TimerDisplay extends Module {
    public TimerDisplay() {
        super("TimerDisplay", "Timer display.", Category.HUD);
    }

    @EventListen
    private void onRender2D(RenderOverlayEvent event) {
        float height = 15f;
        float width = 20f;
        float x = event.scaledResolution.getScaledWidth() / 2;
        float y = event.scaledResolution.getScaledHeight() / 2;

        Rectangle.create(x - (width / 2),y - 35f, width,height)
                .color(new Color(10,10,10,190))
                .push(event);
        Rectangle.create(x - (width / 2) - 0.5f,y - 35f - 0.5f, width + 1,height + 1)
                .color(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0])
                .outline(true)
                .push(event);
        String timer = String.format("%.1f", mc.timer.timerSpeed);
        float length = CFonts.getFont("SFPT-Regular", 18).getStringWidth(timer) + 2.5f;
        float fontheight = CFonts.getFont("SFPT-Regular", 18).getFontHeight() + 5f;
        CFonts.getFont("SFPT-Regular", 18).drawStringWithShadow(timer, x - (length / 2),y - 35f + (height / 2) - (fontheight / 2),Color.white);
    }
}
