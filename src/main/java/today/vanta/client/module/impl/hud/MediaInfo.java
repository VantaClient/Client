package today.vanta.client.module.impl.hud;

import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.client.music.MediaGrabber;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class MediaInfo extends Module {
    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000);
    private int width = 120;
    private int height = 50;
    private int progressBarHeight = 3;
    private boolean dragging;
    private float dragX;
    private float dragY;
    public MediaInfo() {
        super("MediaInfo", "Displays playing media.", Category.HUD);
    }
    private void handleDragging(float mouseX, float mouseY) {
        if (Mouse.isButtonDown(0)) {
            if (!dragging && RenderUtil.hovered(mouseX, mouseY, x.getValue().floatValue(), y.getValue().floatValue(), width, height)) {
                dragging = true;
                dragX = mouseX - x.getValue().floatValue();
                dragY = mouseY - y.getValue().floatValue();
            }

            if (dragging) {
                x.setValue(mouseX - dragX);
                y.setValue(mouseY - dragY);
            }
        } else {
            dragging = false;
        }
    }

    @EventListen
    private void onRenderScreen(RenderScreenEvent event) {
        handleDragging(event.mouseX,event.mouseY);
    }


    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        float bar = (float) ((width - 2 -  height) * MediaGrabber.getMillisPosition()) / MediaGrabber.getMillisLength();
        height = 35;
        width = 150;
        Color[] color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors;
        Rectangle
                .create(x,y,width,height)
                .color(new Color(10,10,10,190))
                .push(event);
        int coverTextureId = RenderUtil.getCoverArtTextureId(MediaGrabber.getCoverBytes());

        Rectangle
                .create(x + 1, y + 1, height - 2,height - 2)
                        .color(new Color(40,40,40,255))
                                .push(event);
        ImageRectangle.create(x + 2, y + 2, height - 4, height - 4, coverTextureId)
                .push(event);

        CFonts.getFont("SFPT-Regular", 20).drawStringWithShadow(MediaGrabber.getTitle(),x + 3 + height - 2,y + 2,Color.white);
        CFonts.getFont("SFPT-Regular", 18).drawStringWithShadow(MediaGrabber.getArtist(),x + 3 + height - 2,y + 13,new Color(200,200,200,255));

        Rectangle.create(x + 1 + height,y + height - progressBarHeight - 2,width - 4 - height,progressBarHeight)
                .color(new Color(20,20,20,255))
                .push(event);
        Rectangle.create(x + 1 + height,y + height - progressBarHeight - 2,bar,progressBarHeight)
                .color(color[0])
                .push(event);
    }
}
