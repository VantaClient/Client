package today.vanta.client.screen.component.impl;

import today.vanta.client.screen.component.Component;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.sound.Sounds;

import java.awt.*;

public class LabelComponent extends Component {
    private boolean onlyText = false;
    public LabelComponent(String text, float x, float y, float width, float height, CFontRenderer font) {
        super(text, x, y, width, height, font);
    }

    public LabelComponent(String text, float x, float y, CFontRenderer font) {
        super(text, x, y, font.getStringWidth(text), font.getFontHeight(), font);
        onlyText = true;
    }

    @Override
    public void draw(float mouseX, float mouseY) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (!onlyText) {
            RenderUtil.rectangle(x, y, width, height, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));
            font.drawYCenteredString(text, x + 3.5f, y + height / 2 - 2, Color.WHITE, false);
        } else {
            font.drawString(text, x, y - 2, hover ? Color.LIGHT_GRAY : Color.WHITE);
        }
    }

    @Override
    public boolean click(float mouseX, float mouseY, int mouseButton) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (hover && mouseButton != -1) {
            Sounds.POP.play();
            return true;
        }
        return false;
    }
}
