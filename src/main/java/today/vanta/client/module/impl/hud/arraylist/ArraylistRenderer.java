package today.vanta.client.module.impl.hud.arraylist;

import today.vanta.util.game.render.font.IRenderer;

import java.awt.*;

public abstract class ArraylistRenderer {
    public final IRenderer renderer;

    public ArraylistRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }

    public void drawString(String text, float x, float y, Color color, boolean shadow) {
        if (shadow) {
            renderer.drawStringWithShadow(text, x, y, color);
        } else {
            renderer.drawString(text, x, y, color);
        }
    }

    public float getStringWidth(String text) {
        return renderer.getStringWidth(text);
    }

    public abstract float getFontHeight();

    public float getBoxHeight() {
        return getFontHeight() + 5;
    }
}
