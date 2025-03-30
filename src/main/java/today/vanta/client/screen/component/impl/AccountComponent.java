package today.vanta.client.screen.component.impl;

import today.vanta.Vanta;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.screen.component.Component;
import today.vanta.util.client.network.NetworkUtil;
import today.vanta.util.client.network.account.Account;
import today.vanta.util.client.network.account.AccountSavingUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.sound.Sounds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AccountComponent extends Component {
    public Account account;
    private BufferedImage bufferedImage;

    public AccountComponent(Account account, float x, float y, float width, float height, CFontRenderer font) {
        super(account.username, x, y, width, height, font);
        this.account = account;

        this.bufferedImage = RenderUtil.base64ToBufferedImage(account.skin);
    }

    @Override
    public void draw(float mouseX, float mouseY) {
        boolean hover = RenderUtil.hovered(mouseX, mouseY, x, y, width, height);
        boolean currentAccount = account.equals(AccountSavingUtil.CURRENT_ACCOUNT);
        Color color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        RenderUtil.rectangle(x, y, width, height, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));
        font.drawYCenteredString(text, x + height - 4 + 3.5f, y + height / 2 - 2, currentAccount ? color1 : Color.WHITE, false);
        RenderUtil.image(RenderUtil.bindBufferedImage(bufferedImage), (int) x + 2, (int) y + 2, (int) height - 4, (int) height - 4);
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

    public void refresh() {
        if (account.isEmail()) {
            try {
                account.skin = NetworkUtil.getBase64EncodedImage(NetworkUtil.getHead(account.password, 512));
            } catch (IOException ignored) {
            }
        }
        this.bufferedImage = RenderUtil.base64ToBufferedImage(account.skin);
    }
}
