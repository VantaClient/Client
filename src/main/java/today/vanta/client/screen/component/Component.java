package today.vanta.client.screen.component;

import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;

public abstract class Component {
    public String text;
    public float x, y, width, height;
    public MsdfFontRenderer font;

    public Component(String text, float x, float y, float width, float height, MsdfFontRenderer font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
    }

    public abstract void draw(RenderScreenEvent event);

    public abstract boolean click(float mouseX, float mouseY, int mouseButton);
}