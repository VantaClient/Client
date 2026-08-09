package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.ModuleDisableEvent;
import today.vanta.client.event.impl.client.ModuleEnableEvent;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.util.client.ui.NotificationUtil;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class Notifications extends Module {
    // genuinely worst notification system but only thing that came to mind
    private float height = 21f;
    private float width = 130f;
    private int lastNumber;
    public Notifications() {
        super("Notifications", "Gives you notifications.", Category.HUD);
    }

    @EventListen
    private void onModuleEnable(ModuleEnableEvent event) {
        NotificationUtil.registerNotification("AAA", event.module.name + " Has Been Enabled", 3000L);
    }

    @EventListen
    private void onModuleDisabled(ModuleDisableEvent event) {
        NotificationUtil.registerNotification("AAA", event.module.name + " Has Been Disabled", 3000L);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float yAddition = 5;
        MsdfFontRenderer font = CFonts.SFPT_REGULAR_18;
        for (int i = 0; i < NotificationUtil.notifTitle.size(); i++) {
            if (NotificationUtil.notifTime.get(i) + 3000 <= System.currentTimeMillis()) {
                NotificationUtil.notifTitle.remove(i);
                NotificationUtil.notifMessage.remove(i);
                NotificationUtil.notifLifetime.remove(i);
                NotificationUtil.notifTime.remove(i);
                yAddition -= height + 5;
                return;
            }
            String message = NotificationUtil.notifMessage.get(i);
            float width = font.getStringWidth(message) + 4;
            float x = event.scaledResolution.getScaledWidth() - width - 5;
            float y = event.scaledResolution.getScaledHeight() - height - yAddition - 10;
            Long lifetime = NotificationUtil.notifLifetime.get(i);
            float totalBarWidth = width - 4;
            long elapsed = System.currentTimeMillis() - NotificationUtil.notifTime.get(i);
            long lifetimea = NotificationUtil.notifLifetime.get(i);
            float remainingFraction = 1f - ((float) elapsed / lifetimea);
            float barWidth = totalBarWidth * remainingFraction;
            Rectangle.create(x,y,width,height).color(new Color(10,10,10,190)).push(event);
            GradientRectangle.create(x,y,width,1f).firstColor(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0]).secondColor(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[1]).push(event);
            Rectangle.create( x + 2,y + height - 6f,totalBarWidth,3f).color(new Color(10,10,10,255)).push(event);
            Rectangle.create( x + 2,y + height - 6f,barWidth,3f).color(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0]).push(event);

            font.drawStringWithShadow(message,x + 2,y + 2,Color.white);
            yAddition += height + 5;
        }
    }


}
