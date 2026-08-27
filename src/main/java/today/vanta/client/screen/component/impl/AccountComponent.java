package today.vanta.client.screen.component.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.screen.component.Component;
import today.vanta.util.client.cache.TextureCache;
import today.vanta.util.client.network.NetworkUtil;
import today.vanta.util.client.network.account.Account;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.sound.Sounds;
import today.vanta.util.system.math.ColorUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AccountComponent extends Component {
    public Account account;
    private int skinTextureId;
    private long duration = 75;
    private long timeOfChange;
    private float progress;
    private boolean didHover = false;
    private float scale;
    public AccountComponent(Account account, float x, float y, float width, float height, MsdfFontRenderer font) {
        super(account.username, x, y, width, height, font);
        this.account = account;
        refresh();
    }

    @Override
    public void draw(RenderScreenEvent event) {
        boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, x, y, width, height);
        boolean currentAccount = account.equals(Vanta.instance.accountStorage.currentAccount);
        Color color1 = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        if (hover) {
            scale = 0.975f;
        } else {
            scale = 1;
        }
        if (didHover && !hover) {
            timeOfChange = System.currentTimeMillis();
            didHover = false;
        }

        if (hover && !didHover) {
            timeOfChange = System.currentTimeMillis();
            Sounds.HOVER2.play();
            didHover = true;
        }

        long elapsed = System.currentTimeMillis() - timeOfChange;
        progress = MathHelper.clamp_float(elapsed / (float) duration, 0f, 1f);
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
                .firstColor(hover ? getHoverFirst() : ColorUtil.interpolateColor(getHoverFirst(), getStandard1(), progress))
                .secondColor(hover ? getHoverSecond() : ColorUtil.interpolateColor(getHoverSecond(), getStandard2(), progress))
                .gradientMode(GradientMode.VERTICAL)
                .push(event);
        font.drawYCenteredString(text, x + height - 4 + 6, y + height / 2, currentAccount ? color1 : Color.WHITE, false);
        ImageRectangle
                .create(x + 4, y + 2, height - 4, height - 4, skinTextureId)
                .push(event);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean click(float mouseX, float mouseY, int mouseButton) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (hover && mouseButton != -1) {
            Sounds.DUNG.play();
            refresh();
            return true;
        }
        return false;
    }

    public void refresh() {
        if (!account.isCracked()) {
            try {
                account.skin = NetworkUtil.getBase64EncodedImage(NetworkUtil.getHead(account.uuid, 512));
            } catch (IOException ignored) {
            }
        }

        BufferedImage bufferedImage = RenderUtil.base64ToBufferedImage(account.skin);
        this.skinTextureId = TextureCache.getTexture(account.uuid, bufferedImage);
    }
}