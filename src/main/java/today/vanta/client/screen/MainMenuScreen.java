package today.vanta.client.screen;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import today.vanta.Vanta;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.client.IClient;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.render.font.CFonts;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen extends GuiScreen {

    private final CFontRenderer roundedSemibold10 = CFonts.SFPT_SEMIBOLD_20;
    private final CFontRenderer roundedMedium9 = CFonts.SFPT_MEDIUM_18;

    private final List<Component> buttons = new ArrayList<>();

    private float handleX = -9999;

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;

        float buttonWidth = 140;

        buttons.clear();
        buttons.add(new ButtonComponent("Singleplayer", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, roundedMedium9));
        middleY += 14;
        buttons.add(new ButtonComponent("Multiplayer", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, roundedMedium9));
        middleY += 14;
        buttons.add(new ButtonComponent("Options", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, roundedMedium9));
        middleY += 14;
        buttons.add(new ButtonComponent("Alts", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, roundedMedium9));
        middleY += 14;
        buttons.add(new ButtonComponent("Exit", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, roundedMedium9));

        buttons.add(new ButtonComponent("Changelog", 5, 5, CFonts.getFont("SFH-Regular", 18)));

        if (handleX == -9999) {
            handleX = width - 5 - 115;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderUtil.rectangle(0, 0, width, height, new Color(20, 20, 20));

        float middleX = width / 2f;
        float middleY = height / 2f;

        RenderUtil.rectangle(middleX - 143 / 2f, middleY - 16, 143, 14 * (buttons.size() - 1) + 18, new Color(30, 30, 30));
        roundedSemibold10.drawString(IClient.CLIENT_NAME + " §7" + IClient.CLIENT_VERSION, middleX - 143 / 2f + 3, middleY - 18 + 4.5f, -1);

        buttons.forEach(but -> but.draw(mouseX, mouseY));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Component but : buttons) {
            if (but.click(mouseX, mouseY, 0)) {
                switch (but.text) {
                    case "Singleplayer":
                        mc.displayGuiScreen(new GuiSelectWorld(this));
                        break;
                    case "Multiplayer":
                        mc.displayGuiScreen(new GuiMultiplayer(this));
                        break;
                    case "Options":
                        mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                        break;
                    case "Alts":
                        mc.displayGuiScreen(Vanta.instance.screenStorage.getT(AltLoginScreen.class));
                        break;
                    case "Exit":
                        mc.shutdownMinecraftApplet();
                        break;

                    case "Changelog":
                        mc.displayGuiScreen(Vanta.instance.screenStorage.getT(ChangelogScreen.class));
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
