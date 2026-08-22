package today.vanta.client.screen;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.entity.Render;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.impl.client.ClickGUI;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.client.Strings;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ImageUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.render.shape.impl.Triangle;
import today.vanta.util.system.math.ColorUtil;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MainMenuScreen extends VantaScreen {

    private final List<Component> buttons = new ArrayList<>();
    private int random;

    private int rotation = 0;
    private float rotTria = 0;
    private boolean lastState;
    private float mouseVal;
    private float middleX;

    @Override
    protected void initScreen() {
        random = ThreadLocalRandom.current().nextInt(0,Strings.TIPS.size());
        System.out.println(random);
        middleX = width / 2f;
        float middleY = height / 2f;

        float buttonWidth = 90;
        float plusFactor = 16;

        buttons.clear();
        buttons.add(new ButtonComponent("Singleplayer", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_REGULAR_18));
        middleY += plusFactor;
        buttons.add(new ButtonComponent("Multiplayer", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_REGULAR_18));
        middleY += plusFactor;
        buttons.add(new ButtonComponent("Options", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_REGULAR_18));
        middleY += plusFactor;
        buttons.add(new ButtonComponent("Alts", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_REGULAR_18));
        middleY += plusFactor;
        buttons.add(new ButtonComponent("Exit", middleX - buttonWidth / 2f, middleY, buttonWidth, 14, CFonts.SFPT_REGULAR_18));
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        mouseVal = (event.mouseX / 500);
        Rectangle
                .create(0, 0, width, height)
                .color(new Color(20, 20, 20))
                .push(event);


        ImageRectangle
                .create(0 - 10f + mouseVal, 0 - 10f, width + 20, height + 20, -1)
                .resource(ImageUtil.getTexture("test2.png"))
                .color(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0])
                .push(event);

        float middleY = 5;
        if (!Strings.CHANGELOG.isEmpty()) {
            float panelWidth = 0;
            for (String change : Strings.CHANGELOG) {
                panelWidth = Math.max(panelWidth, CFonts.SFPT_REGULAR_18.getStringWidth(change) + 10);
            }

            float boxHeight = 14 * Strings.CHANGELOG.size() + 18;

            Rectangle
                    .create(5, middleY, panelWidth, boxHeight)
                    .color(new Color(30, 30, 30))
                    .push(event);
            CFonts.getFont("SFPT-Medium", 20).drawString("Changelog", 5 + 3.5f, middleY + 3, -1);

            for (int i = 0; i < Strings.CHANGELOG.size(); i++) {
                String change = Strings.CHANGELOG.get(i);
                float y = middleY + 18 + i * 14 - 1.5f;

                Rectangle
                        .create(5 + 1.5f, y, (panelWidth - 3), 14)
                        .color(new Color(35, 35, 35))
                        .push(event);

                String formattedChange = formatChange(change);

                CFonts.SFPT_REGULAR_18.drawYCenteredString(formattedChange, 5 + 3.5f, y + 14 / 2f - 1, Color.WHITE, false);
            }
        }

        float middleX = width / 2f;
        middleY = height / 2f;

        Rectangle
                .create(middleX - 94 / 2f, middleY - 14, 94, 16 * (buttons.size()) + 15)
                .color(new Color(30, 30, 30))
                .push(event);
//        CFonts.getFont("SFPT-Medium", 20).drawString(Strings.CLIENT_NAME, middleX - ((float) CFonts.getFont("SFPT-Medium", 20).getStringWidth(Strings.CLIENT_NAME) / 2), middleY - 18 + 4.5f, -1);
        CFonts.getFont("SFPT-Medium", 20).drawHorizontalGradientString(Strings.CLIENT_NAME, middleX - ((float) CFonts.getFont("SFPT-Medium", 20).getStringWidth(Strings.CLIENT_NAME) / 2), middleY - 18 + 4.5f, Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0],Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[1],0.5,1);
        CFonts.SFPT_REGULAR_18.drawString(Strings.CLIENT_VERSION + " | " + Strings.DEVELOPERS, middleX * 2 - CFonts.SFPT_MEDIUM_18.getStringWidth(Strings.CLIENT_VERSION + " | " + Strings.DEVELOPERS) - 3, middleY * 2 - CFonts.SFPT_MEDIUM_18.getFontHeight() - 5.5f, new Color(200, 200, 200));

        if (rotation > 360) {
            rotation = 0;
        }

        ImageRectangle
                .create(width - 100 - 20, 20, 100, 100, -1)
                .resource(ImageUtil.getTexture("cousin.png"))
                .rotate(rotation)
                .push(event);

        buttons.forEach(but -> but.draw(event));

        CFonts.SFPT_MEDIUM_18.drawStringWithShadow(Strings.TIPS.get(random),middleX - ((float) CFonts.SFPT_MEDIUM_18.getStringWidth(Strings.TIPS.get(random)) / 2),middleY + 85,Color.white);

        rotation++;
    }

    private static String formatChange(String change) {
        String formattedChange;

        if (change.startsWith("[+]")) {
            formattedChange = "§a" + change;
        } else if (change.startsWith("[-]")) {
            formattedChange = "§c" + change;
        } else if (change.startsWith("[#]")) {
            formattedChange = "§e" + change;
        } else if (change.startsWith("[~]")) {
            formattedChange = "§9" + change;
        } else {
            formattedChange = "§7" + change;
        }

        return formattedChange;
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