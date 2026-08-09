package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.render.RenderCrosshairEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.render.shape.impl.Triangle;

import java.awt.*;

public class Crosshair extends Module {
    private final StringSetting mode = Setting.of("Mode", "Rect", "Rect", "Triangle");
    private final NumberSetting size = Setting.of("Size", 1,1,3,"x").hide(() -> !mode.isValue("Triangle"));
    private final NumberSetting length = Setting.of("Length", 7, 4, 10, 0).hide(() -> !mode.isValue("Rect"));
    private final NumberSetting width = Setting.of("Width", 0.5f, 0.5f, 2, 1).hide(() -> !mode.isValue("Rect"));
    private final NumberSetting space = Setting.of("Static space", 5, 0, 15).hide(() -> !mode.isValue("Rect"));
    private final NumberSetting spaceMove = Setting.of("Moving space", 7, 0, 16).hide(() -> !mode.isValue("Rect"));
    private final StringSetting colorMode = Setting.of("Crosshair color", "White", "Theme", "White");
    private final BooleanSetting outline = Setting.of("Outline", true).hide(() -> !mode.isValue("Rect"));
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

        float y = (float) event.scaledResolution.getScaledHeight() / 2;
        float x = (float) event.scaledResolution.getScaledWidth() / 2;

        Color color = Color.WHITE;

        if (colorMode.isValue("Theme")) {
            color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        }
        if (mode.isValue("Rect")) {
            float targetSpacing = MovementUtil.isMoving()
                    ? spaceMove.getValue().floatValue()
                    : space.getValue().floatValue();

            animatedSpacing += (targetSpacing - animatedSpacing) * 0.2f;
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

        if (mode.isValue("Triangle")) {
            float width = 10 * size.getValue().floatValue();
            float height = 5 * size.getValue().floatValue();
            Triangle.create(x - (width / 2),y - (height / 2),width,height).outline(true).outlineWidth(2f).color(color).push(event);
        }
    }
}
