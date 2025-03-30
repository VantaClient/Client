package today.vanta.client.screen;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
import today.vanta.Vanta;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.AccountComponent;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.client.network.MicrosoftUtil;
import today.vanta.util.client.network.account.Account;
import today.vanta.util.client.network.account.AccountSavingUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.render.font.CFonts;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AltLoginScreen extends GuiScreen {

    private final CFontRenderer smallTitle = CFonts.SFPT_SEMIBOLD_20;
    private final CFontRenderer buttonText = CFonts.SFPT_MEDIUM_18;

    private final List<Component> components = new ArrayList<>();

    public AltLoginScreen() {
        AccountSavingUtil.loadConfig();
    }

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;

        float buttonWidth = 140;

        components.clear();
        components.add(new ButtonComponent("Login with browser", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, buttonText));
        middleY += 14;
        if (!AccountSavingUtil.ACCOUNTS.isEmpty()) {
            for (Account account : AccountSavingUtil.ACCOUNTS) {
                components.add(new AccountComponent(account, middleX - buttonWidth / 2f, middleY, buttonWidth, 14, buttonText));
                middleY += 14;
            }
        }

        components.add(new ButtonComponent("Back", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, buttonText));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderUtil.rectangle(0, 0, width, height, new Color(20, 20, 20));

        buttonText.drawString("(Alt accounts) Left click to login, right click to delete.", 5, 5, new Color(125, 125, 125).getRGB());

        float middleX = width / 2f;
        float middleY = height / 2f;

        RenderUtil.rectangle(middleX - 143 / 2f, middleY - 16, 143, 14 * components.size() + 18, new Color(30, 30, 30));
        smallTitle.drawString(mc.session.getUsername(), middleX - 143 / 2f + 3, middleY - 18 + 4.5f, -1);

        components.forEach(c -> c.draw(mouseX, mouseY));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Component c : components) {
            if (c.click(mouseX, mouseY, mouseButton)) {
                switch (c.text) {
                    case "Login with browser":
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        MicrosoftUtil.acquireMSAuthCode(executor)
                                .thenComposeAsync(msAuthCode -> MicrosoftUtil.acquireMSAccessToken(msAuthCode, executor), executor)
                                .thenComposeAsync(msAccessToken -> MicrosoftUtil.acquireXboxAccessToken(msAccessToken, executor), executor)
                                .thenComposeAsync(xboxAccessToken -> MicrosoftUtil.acquireXboxXstsToken(xboxAccessToken, executor), executor)
                                .thenComposeAsync(xboxXstsData -> MicrosoftUtil.acquireMCAccessToken(xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor), executor)
                                .thenComposeAsync(mcToken -> MicrosoftUtil.login(mcToken, executor), executor)
                                .thenAccept(session -> {
                                    Account account = new Account(session.getUsername(), session.getPlayerID(), session.getToken());
                                    if (!AccountSavingUtil.ACCOUNTS.contains(account)) {
                                        AccountSavingUtil.ACCOUNTS.add(account);
                                    }

                                    mc.session = session;
                                    initGui();
                                    Vanta.instance.logger.info("Logged into {}! (microsoft)", session.getUsername());
                                    AccountSavingUtil.saveConfig();
                                })
                                .exceptionally(error -> {
                                    Vanta.instance.logger.error("Failed to login due to {}", error.getMessage());
                                    return null;
                                });
                        break;
                    case "Back":
                        mc.displayGuiScreen(new GuiMainMenu());
                        break;
                    default:
                        if (c instanceof AccountComponent) {
                            AccountComponent aC = (AccountComponent) c;
                            Iterator<Account> iterator = AccountSavingUtil.ACCOUNTS.iterator();
                            while (iterator.hasNext()) {
                                Account account = iterator.next();
                                if (aC.account.equals(account)) {
                                    if (mouseButton == 0) {
                                        AccountSavingUtil.CURRENT_ACCOUNT = account;
                                        if (account.isCracked()) {
                                            Vanta.instance.logger.info("Logged into {}! (cracked)", account.username);
                                            mc.session = new Session(account.username, "", "", "legacy");
                                        } else {
                                            Vanta.instance.logger.info("Logged into {}! (microsoft)", account.username);
                                            mc.session = new Session(account.username, account.password, account.token, "legacy");
                                        }
                                        aC.refresh();
                                    } else {
                                        Vanta.instance.logger.info("Removed {}!", account.username);
                                        iterator.remove();
                                        initGui();
                                    }
                                    AccountSavingUtil.saveConfig();
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}
