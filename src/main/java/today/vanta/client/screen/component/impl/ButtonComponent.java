package today.vanta.client.screen.component.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.screen.component.Component;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.sound.Sounds;
import today.vanta.util.system.math.ColorUtil;
import today.vanta.util.system.math.MathUtil;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;

public class ButtonComponent extends Component {
    private Color hoverFirst = ColorUtil.getDarker(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0], 4);
    // new Color(40, 40, 40);
    private Color hoverSecond = new Color(37, 37, 37);
    private Color standard1 = new Color(35, 35, 35);
    private Color standard2 = new Color(32, 32, 32);
    private long duration = 75;
    private long timeOfChange;
    private float progress;
    private boolean didHover = false;
    private float scale;

    public ButtonComponent(String text, float x, float y, float width, float height, MsdfFontRenderer font) {
        super(text, x, y, width, height, font);
    }

    @Override
    public void draw(RenderScreenEvent event) {
        boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, x, y, width, height);
        if (hover) {
            scale = 0.99f;
        } else {
            scale = 1;
        }
        if (didHover && !hover) {
            timeOfChange = System.currentTimeMillis();
            didHover = false;
        }

        if (hover && !didHover) {
            timeOfChange = System.currentTimeMillis();
            Sounds.HOVER.play();
            didHover = true;
        }

        long elapsed = System.currentTimeMillis() - timeOfChange;
        progress = MathUtil.clamp(elapsed / (float) duration, 0f, 1f);
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0);
        GradientRectangle
                .create(x, y, width, height)
                .firstColor(new Color(20, 20, 20))
                .secondColor(new Color(25, 25, 25))
                .gradientMode(GradientMode.VERTICAL)
                .push(event);

        GradientRectangle
                .create(x + 0.5f, y + 0.5f, width - 1, height - 1)
                .firstColor(hover ? hoverFirst : ColorUtil.interpolateColor(hoverFirst, standard1, progress))
                .secondColor(hover ? hoverSecond : ColorUtil.interpolateColor(hoverSecond, standard2, progress))
                .gradientMode(GradientMode.VERTICAL)
                .push(event);

        // ColorUtil.interpolateColor(standard1, hoverFirst, progress)
        // ColorUtil.interpolateColor(standard2, hoverSecond, progress)

        font.drawStringWithShadow(text, x + width / 2 - ((float) font.getStringWidth(text) / 2), y + height / 2 - 5.5f, Color.WHITE);
        GlStateManager.popMatrix();

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