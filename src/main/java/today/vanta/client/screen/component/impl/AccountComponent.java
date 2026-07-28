package today.vanta.client.screen.component.impl;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.screen.component.Component;
import today.vanta.util.client.cache.TextureCache;
import today.vanta.util.client.network.NetworkUtil;
import today.vanta.util.client.network.account.Account;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.sound.Sounds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AccountComponent extends Component {
    public Account account;
    private int skinTextureId;

    public AccountComponent(Account account, float x, float y, float width, float height, GlyphFontRenderer font) {
        super(account.username, x, y, width, height, font);
        this.account = account;
        refresh();
    }

    @Override
    public void draw(RenderScreenEvent event) {
        boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, x, y, width, height);
        boolean currentAccount = account.equals(Vanta.instance.accountStorage.currentAccount);
        Color color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        Rectangle
                .create(x, y, width, height)
                .color(hover ? new Color(40, 40, 40) : new Color(35, 35, 35))
                .push(event);
        font.drawYCenteredString(text, x + height - 4 + 6, y + height / 2 - 2, currentAccount ? color1 : Color.WHITE, false);
        ImageRectangle
                .create(x + 4, y + 2, height - 4, height - 4, skinTextureId)
                .push(event);
    }

    @Override
    public boolean click(float mouseX, float mouseY, int mouseButton) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        if (hover && mouseButton != -1) {
            Sounds.POP.play();
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