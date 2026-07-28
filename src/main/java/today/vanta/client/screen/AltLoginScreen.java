package today.vanta.client.screen;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Session;
import org.lwjgl.opengl.GL11;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.AccountComponent;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.client.network.MicrosoftUtil;
import today.vanta.util.client.network.account.Account;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.os.Enhancements;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AltLoginScreen extends VantaScreen {
    private final List<Component> components = new ArrayList<>();

    @Override
    protected void initScreen() {
        float middleX = width / 2f;
        float middleY = height / 2f;

        float buttonWidth = 140;

        components.clear();
        components.add(new ButtonComponent("Login with browser", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_MEDIUM_18));
        middleY += 14;

        for (Account account : Vanta.instance.accountStorage.list) {
            components.add(new AccountComponent(account, middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_MEDIUM_18));
            middleY += 14;
        }

        components.add(new ButtonComponent("Back", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_MEDIUM_18));
    }

    private void addOrUpdateAccount(Account account) {
        for (Account existing : Vanta.instance.accountStorage.list) {
            if (existing.username.equalsIgnoreCase(account.username)) {
                existing.uuid = account.uuid;
                existing.token = account.token;
                existing.refreshToken = account.refreshToken;
                saveAccounts();
                return;
            }
        }

        Vanta.instance.accountStorage.list.add(account);
        saveAccounts();
    }

    private void saveAccounts() {
        Vanta.instance.fileStorage.accountsFile.save();
    }

    private CompletableFuture<MicrosoftUtil.LoginResult> loginWithAccount(Account account, ExecutorService executor) {
        return MicrosoftUtil.login(account.token, account.refreshToken, executor).handle((result, error) -> {
            if (error == null) {
                return CompletableFuture.completedFuture(result);
            }

            if (!account.refreshToken.isEmpty()) {
                return MicrosoftUtil.acquireMSTokensFromRefreshToken(account.refreshToken, executor)
                        .thenComposeAsync(ms -> MicrosoftUtil.acquireXboxAccessToken(ms.accessToken, executor)
                                .thenComposeAsync(xbox -> MicrosoftUtil.acquireXboxXstsToken(xbox, executor), executor)
                                .thenComposeAsync(xsts -> MicrosoftUtil.acquireMCAccessToken(xsts.get("Token"), xsts.get("uhs"), executor), executor)
                                .thenComposeAsync(mc -> MicrosoftUtil.login(mc, ms.refreshToken, executor), executor), executor);
            }

            CompletableFuture<MicrosoftUtil.LoginResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(error);
            return failed;
        }).thenCompose(future -> future);
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        if (Enhancements.supportsWindowBlur()) {
            GlStateManager.clearColor(0, 0, 0, 0);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        } else {
            Rectangle
                    .create(0, 0, width, height)
                    .color(new Color(20, 20, 20))
                    .push(event);
        }

        CFonts.SFPT_MEDIUM_18.drawString("(Alt accounts) Left click to login, right click to delete.", 5, 5, new Color(125, 125, 125).getRGB());

        float middleX = width / 2f;
        float middleY = height / 2f;

        Rectangle
                .create(middleX - 143 / 2f, middleY - 16, 143, 14 * components.size() + 18)
                .color(new Color(30, 30, 30))
                .push(event);
        CFonts.SFPT_SEMIBOLD_20.drawString(mc.session.getUsername(), middleX - 143 / 2f + 3, middleY - 18 + 4.5f, -1);

        components.forEach(c -> c.draw(event));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Component c : components) {
            if (c.click(mouseX, mouseY, mouseButton)) {
                switch (c.text) {
                    case "Login with browser":
                        ExecutorService authExecutor = Executors.newSingleThreadExecutor();
                        MicrosoftUtil.acquireMSAuthCode(authExecutor)
                                .thenComposeAsync(result -> MicrosoftUtil.acquireMSTokensFromAuthCode(result.code, result.redirectUri, authExecutor), authExecutor)
                                .thenComposeAsync(ms -> MicrosoftUtil.acquireXboxAccessToken(ms.accessToken, authExecutor)
                                        .thenComposeAsync(xbox -> MicrosoftUtil.acquireXboxXstsToken(xbox, authExecutor), authExecutor)
                                        .thenComposeAsync(xsts -> MicrosoftUtil.acquireMCAccessToken(xsts.get("Token"), xsts.get("uhs"), authExecutor), authExecutor)
                                        .thenComposeAsync(mc -> MicrosoftUtil.login(mc, ms.refreshToken, authExecutor), authExecutor), authExecutor)
                                .thenAccept(result -> {
                                    Account account = new Account(result.session.getUsername(), result.session.getPlayerID(), result.session.getToken(), result.refreshToken);
                                    addOrUpdateAccount(account);
                                    Vanta.instance.accountStorage.currentAccount = account;
                                    mc.session = result.session;
                                    Vanta.instance.logger.info("Logged into {}! (microsoft)", result.session.getUsername());
                                    scheduleInitGui();
                                })
                                .exceptionally(error -> {
                                    Vanta.instance.logger.error("Failed to login due to {}", error.getMessage());
                                    return null;
                                })
                                .whenComplete((result, error) -> authExecutor.shutdown());
                        break;
                    case "Back":
                        mc.displayGuiScreen(new GuiMainMenu());
                        break;
                    default:
                        if (c instanceof AccountComponent) {
                            AccountComponent aC = (AccountComponent) c;
                            Iterator<Account> iterator = Vanta.instance.accountStorage.list.iterator();
                            while (iterator.hasNext()) {
                                Account account = iterator.next();
                                if (aC.account == account) {
                                    if (mouseButton == 0) {
                                        if (account.isCracked()) {
                                            Vanta.instance.logger.info("Logged into {}! (cracked)", account.username);
                                            Vanta.instance.accountStorage.currentAccount = account;
                                            mc.session = new Session(account.username, "", "", "legacy");
                                        } else {
                                            ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
                                            loginWithAccount(account, refreshExecutor)
                                                    .thenAccept(result -> {
                                                        account.token = result.session.getToken();
                                                        if (result.refreshToken != null) {
                                                            account.refreshToken = result.refreshToken;
                                                        }

                                                        Vanta.instance.accountStorage.currentAccount = account;
                                                        mc.session = result.session;
                                                        Vanta.instance.logger.info("Logged into {}! (microsoft)", account.username);
                                                        saveAccounts();

                                                        scheduleInitGui();
                                                    })
                                                    .exceptionally(error -> {
                                                        Vanta.instance.logger.error("Failed to login due to {}", error.getMessage());
                                                        return null;
                                                    })
                                                    .whenComplete((result, error) -> refreshExecutor.shutdown());
                                        }
                                    } else if (mouseButton == 1) {
                                        Vanta.instance.logger.info("Removed {}!", account.username);
                                        iterator.remove();
                                        saveAccounts();
                                        scheduleInitGui();
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        }
    }
}