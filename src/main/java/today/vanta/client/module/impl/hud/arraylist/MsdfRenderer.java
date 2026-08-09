package today.vanta.client.module.impl.hud.arraylist;

import today.vanta.util.game.render.font.IRenderer;

import java.awt.*;

public class MsdfRenderer extends ArraylistRenderer {
    public MsdfRenderer(IRenderer renderer) {
        super(renderer);
    }

    @Override
    public void drawString(String text, float x, float y, Color color, boolean shadow) {
        super.drawString(text, x - 1, y - 1, color, shadow);
    }

    @Override
    public float getBoxHeight() {
        return getFontHeight() + 1;
    }

    @Override
    public float getFontHeight() {
        return renderer.getFontHeight();
    }
}
