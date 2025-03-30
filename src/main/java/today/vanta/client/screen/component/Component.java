package today.vanta.client.screen.component;

import today.vanta.util.game.render.font.CFontRenderer;

public abstract class Component {
    public String text;
    public float x, y, width, height;
    public CFontRenderer font;

    public Component(String text, float x, float y, float width, float height, CFontRenderer font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
    }

    public abstract void draw(float mouseX, float mouseY);
    public abstract boolean click(float mouseX, float mouseY, int mouseButton);
}
