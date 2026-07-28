package today.vanta.client.screen.component.impl;

import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.screen.component.Component;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.sound.Sounds;

import java.awt.*;

public class ButtonComponent extends Component {
    private boolean didHover = false;

    public ButtonComponent(String text, float x, float y, float width, float height, GlyphFontRenderer font) {
        super(text, x, y, width, height, font);
    }

    @Override
    public void draw(RenderScreenEvent event) {
        boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, x, y, width, height);

        Rectangle
                .create(x, y, width, height)
                .color(hover ? new Color(40, 40, 40) : new Color(35, 35, 35))
                .push(event);

        font.drawYCenteredString(text, x + 3.5f, y + height / 2 - 2, Color.WHITE, false);

        if (didHover && !hover) {
            didHover = false;
        }

        if (hover && !didHover) {
            Sounds.HOVER.play();
            didHover = true;
        }
    }

    @Override
    public boolean click(float mouseX, float mouseY, int mouseButton) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (hover && mouseButton == 0) {
            Sounds.POP.play();
            return true;
        }
        return false;
    }
}