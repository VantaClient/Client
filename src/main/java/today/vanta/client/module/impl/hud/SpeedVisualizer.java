package today.vanta.client.module.impl.hud;

import net.minecraft.util.MathHelper;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

public class SpeedVisualizer extends Module {
    private static final float X = 20, Y = 200, WIDTH = 100, HEIGHT = 50;
    private static final long UPDATE_MS = 5;
    private static final double MAX_BPS = 20.0;
    private static final int MAX_SAMPLES = (int) WIDTH - 4;
    private static double lastY;

    private final Deque<Double> samples = new ArrayDeque<>();
    private long lastUpdate = 0;

    public SpeedVisualizer() {
        super("SpeedVisualizer", "Visualises speed with a graph.", Category.HUD);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate >= UPDATE_MS) {
            lastUpdate = now;
            samples.addLast(MovementUtil.getBPS());
            while (samples.size() > MAX_SAMPLES) samples.removeFirst();
        }
        Rectangle.create(X - 1, Y - 1,WIDTH + 2, HEIGHT + 2).color(new Color(60,60,60,255)).push(event);
        Rectangle.create(X, Y, WIDTH, HEIGHT).color(new Color(20, 20, 20, 190)).push(event);

        int i = 0;
        for (double bps : samples) {
            double vf = MathHelper.clamp_double(bps / MAX_BPS, 0, 1);
            double py = Y + (HEIGHT - 1) * (1 - vf);

            double top = py;
            double height = 1;
            if (i > 0) {
                top = Math.min(py, lastY);              // higher of the two points (smaller Y)
                height = Math.abs(py - lastY) + 1;      // span between them, +1 to include the pixel
            }

            Rectangle.create(X + 2 + i, top, 1, height).color(Color.white).push(event);

            lastY = py; // save the real point, not the adjusted top
            i++;
        }
    }
}