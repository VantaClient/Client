package today.vanta.client.module.impl.misc;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.network.PrintChatMessage;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.Counter;

import java.awt.*;

public class AutoPlay extends Module {
    private final StringSetting mode = Setting.of("Mode", "Miniblox", "Miniblox");
    private final Counter time = new Counter();
    private boolean canQueue;
    private boolean canReset;
    private float barWidth = 100f;
    private float bar = 100f;
    private float targetBar = 100f;
    private float animBarWidth = 100f;

    public AutoPlay() {
        super("AutoPlay", "Auto Queues.", Category.MISC);
    }

    @EventListen
    private void onPrintChatMessage(PrintChatMessage event) {
        String message = event.message;

        if (mode.isValue("Miniblox") && !canQueue) {
            if (message.contains("§6§lQueueing next game in 15 seconds (or click Next Game to queue immediately)...") || message.startsWith(mc.thePlayer.getName() + " has been eliminated by") || message.contains("§6§l§lQueueing next game in 15 seconds (or click Next Game to queue immediately)...")) {
                canQueue = true;
            }
        }
    }

    @EventListen
    public void onRenderOverlay(RenderOverlayEvent event) {
        float x = event.scaledResolution.getScaledWidth() / 2 - (barWidth / 2);
        float y = event.scaledResolution.getScaledHeight() / 2 + 70f;
        if (mc.thePlayer == null) {
            canQueue = false;
        }
        if (canQueue) {
            if (canReset) {
                time.reset();
                canReset = false;
            }
            bar = barWidth * ((float) time.getElapsedTime() / 3000);
            CFonts.SFPT_MEDIUM_24.drawStringWithShadow(String.valueOf(time.getElapsedTime()), 100, 100, Color.white);
            Rectangle
                    .create(x - 1,y - 1, barWidth + 2, 5f)
                            .color(Color.black)
                                    .push(event);
            Rectangle
                    .create(x, y, bar, 3f)
                    .color(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                    .push(event);
            if (time.getElapsedTime() > 3000) {
                mc.thePlayer.sendChatMessage("/play skywars");
                ChatUtil.send(ChatUtil.Prefix.INFO, "aaa");
                canQueue = false;
            }
        } else {
            canReset = true;
        }
    }

    @Override
    public void onEnable() {
        canReset = false;
    }
}
