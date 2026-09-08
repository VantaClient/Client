package today.vanta.client.module.impl.hud;

import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.client.music.MediaTracker;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;

public class MediaInfo extends Module {
    public MediaInfo() {
        super("MediaInfo", "Displays playing media.", Category.HUD);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        MediaTracker mediaTracker = new MediaTracker();
        if (mediaTracker.getTrack() != null) {
            ChatUtil.send(ChatUtil.Prefix.INFO,  mediaTracker.getTrack().getTitle().isEmpty() ? "none" : mediaTracker.getTrack().getTitle());
        }
    }
}
