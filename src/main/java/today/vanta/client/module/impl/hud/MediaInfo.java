package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
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
    public MediaInfo() {
        super("MediaInfo", "Displays playing media.", Category.HUD);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        float bar = (float) ((width - 4) * MediaGrabber.getMillisPosition()) / MediaGrabber.getMillisLength();
        height = 30;
        Color[] color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors;
        Rectangle
                .create(x,y,width,height)
                .color(new Color(10,10,10,190))
                .push(event);
        int coverTextureId = RenderUtil.getCoverArtTextureId(MediaGrabber.getCoverBytes());

        ImageRectangle.create(x, y, 32, 32, coverTextureId)
                .push(event);

        CFonts.SFPT_REGULAR_18.drawStringWithShadow(MediaGrabber.getTitle(),x + 2,y + 2,Color.white);
        CFonts.getFont("SFPT-Regular", 16).drawStringWithShadow(MediaGrabber.getArtist(),x + 2,y + 11,new Color(200,200,200,255));

        Rectangle.create(x + 2,y + height - progressBarHeight - 2,width - 4,progressBarHeight)
                .color(new Color(20,20,20,255))
                .push(event);
        Rectangle.create(x + 2,y + height - progressBarHeight - 2,bar,progressBarHeight)
                .color(color[0])
                .push(event);
    }
}
