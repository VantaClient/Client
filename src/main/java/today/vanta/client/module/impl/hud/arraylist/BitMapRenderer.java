package today.vanta.client.module.impl.hud.arraylist;

import today.vanta.util.game.render.font.IRenderer;

import java.awt.*;

public class BitMapRenderer extends ArraylistRenderer {
    public BitMapRenderer(IRenderer renderer) {
        super(renderer);
    }

    @Override
    public void drawString(String text, float x, float y, Color color, boolean shadow) {
        super.drawString(text, x - 0.5f, y + 1, color, shadow);
    }

    @Override
    public float getFontHeight() {
        return 9;
    }

    @Override
    public float getBoxHeight() {
        return getFontHeight() + 3;
    }
}
