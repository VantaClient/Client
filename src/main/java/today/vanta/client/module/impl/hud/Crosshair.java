package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.render.RenderCrosshairEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class Crosshair extends Module {
    private final NumberSetting length = Setting.of("Length", 7, 4, 10, 0);
    private final NumberSetting width = Setting.of("Width", 0.5f, 0.5f, 2, 1);
    private final NumberSetting space = Setting.of("Static space", 5, 0, 15);
    private final NumberSetting spaceMove = Setting.of("Moving space", 7, 0, 16);
    private final StringSetting colorMode = Setting.of("Crosshair color", "White", "Theme", "White");
    private final BooleanSetting outline = Setting.of("Outline", true);
    private final BooleanSetting renderInThirdPerson = Setting.of("Third person render", true);

    private float animatedSpacing;

    public Crosshair() {
        super("Crosshair", "Looks like CSGO.", Category.HUD);
        hideFromArraylist = true;
    }

    @EventListen
    private void onRenderCrosshair(RenderCrosshairEvent event) {
        event.cancelled = true;
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        if (mc.gameSettings.thirdPersonView != 0 && !renderInThirdPerson.getValue()) {
            return;
        }

        float targetSpacing = MovementUtil.isMoving()
                ? spaceMove.getValue().floatValue()
                : space.getValue().floatValue();

        animatedSpacing += (targetSpacing - animatedSpacing) * 0.2f;

        Color color = Color.WHITE;

        if (colorMode.isValue("Theme")) {
            color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        }

        float y = (float) event.scaledResolution.getScaledHeight() / 2;
        float x = (float) event.scaledResolution.getScaledWidth() / 2;
        float w = width.getValue().floatValue();
        double len = length.getValue().doubleValue();

        if (outline.getValue()) {
            Rectangle
                    .create(x + animatedSpacing - 1, y - (w / 2) - 1, len + 2, w + 2)
                    .color(Color.BLACK)
                    .push(event);

            Rectangle
                    .create(x - animatedSpacing - len - 1, y - (w / 2) - 1, len + 2, w + 2)
                    .color(Color.BLACK)
                    .push(event);

            Rectangle
                    .create(x - (w / 2) - 1, y + animatedSpacing - 1, w + 2, len + 2)
                    .color(Color.BLACK)
                    .push(event);

            Rectangle
                    .create(x - (w / 2) - 1, y - animatedSpacing - len - 1, w + 2, len + 2)
                    .color(Color.BLACK)
                    .push(event);
        }

        // Main part
        Rectangle
                .create(x + animatedSpacing, y - (w / 2), len, w)
                .color(color)
                .push(event);

        Rectangle
                .create(x - animatedSpacing - len, y - (w / 2), len, w)
                .color(color)
                .push(event);

        Rectangle
                .create(x - (w / 2), y + animatedSpacing, w, len)
                .color(color)
                .push(event);

        Rectangle
                .create(x - (w / 2), y - animatedSpacing - len, w, len)
                .color(color)
                .push(event);
    }
}
