package today.vanta.client.screen.component;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;
import today.vanta.util.system.math.ColorUtil;

import java.awt.*;

public abstract class Component {
    public String text;
    public float x, y, width, height;
    public MsdfFontRenderer font;
    private Color hoverFirst = ColorUtil.getDarker(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0], 4);
    private Color hoverSecond = new Color(37, 37, 37);
    private Color standard1 = new Color(35, 35, 35);
    private Color standard2 = new Color(32, 32, 32);

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

    public Color getHoverFirst() {
        return hoverFirst;
    }

    public Color getHoverSecond() {
        return hoverSecond;
    }

    public Color getStandard1() {
        return standard1;
    }

    public Color getStandard2() {
        return standard2;
    }
}