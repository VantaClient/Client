package today.vanta.client.screen;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import today.vanta.Vanta;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.render.font.CFonts;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangelogScreen extends GuiScreen {
    private final CFontRenderer smallTitle = CFonts.SFPT_SEMIBOLD_20;
    private final CFontRenderer changesFont = CFonts.SFPT_MEDIUM_18;

    private final List<Component> components = new ArrayList<>();

    @Override
    public void initGui() {
        super.initGui();
        components.clear();
        components.add(new ButtonComponent("Back", 5, 5, CFonts.getFont("SFH-Regular", 18)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        RenderUtil.rectangle(0, 0, width, height, new Color(20, 20, 20));

        float panelWidth = 0;
        for (String change : Vanta.instance.moduleStorage.changelog) {
            panelWidth = Math.max(panelWidth, changesFont.getStringWidth(change) + 10);
        }

        float boxHeight = 14 * Vanta.instance.moduleStorage.changelog.size() + 18;
        float middleY = height / 2f - boxHeight / 2f;

        RenderUtil.rectangle(width / 2f - panelWidth / 2f, middleY, panelWidth, boxHeight, new Color(30, 30, 30));
        smallTitle.drawString("Changelog", width / 2f - panelWidth / 2f + 3, middleY + 4.5f - 1, -1);

        for (int i = 0; i < Vanta.instance.moduleStorage.changelog.size(); i++) {
            String change = Vanta.instance.moduleStorage.changelog.get(i);
            float y = middleY + 18 + i * 14;

            boolean hoverChange = RenderUtil.hovered(mouseX, mouseY, width / 2f - (panelWidth - 3) / 2f, y, (panelWidth - 3), 14);
            RenderUtil.rectangle(width / 2f - (panelWidth - 3) / 2f, y, (panelWidth - 3), 14, hoverChange ? new Color(40, 40, 40) : new Color(35, 35, 35));

            String formattedChange;
            if (change.startsWith("[+]")) {
                formattedChange = "§a" + change;
            } else if (change.startsWith("[-]")) {
                formattedChange = "§c" + change;
            } else if (change.startsWith("[#]")) {
                formattedChange = "§e" + change;
            } else {
                formattedChange = "§7" + change;
            }

            changesFont.drawYCenteredString(formattedChange, width / 2f - (panelWidth - 3) / 2f + 3.5f, y + 14 / 2f - 2, Color.WHITE, false);
        }

        components.forEach(but -> but.draw(mouseX, mouseY));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Component but : components) {
            if (but.text.equals("Back") && but.click(mouseX, mouseY, 0)) {
                mc.displayGuiScreen(new GuiMainMenu());
            }
        }
    }
}
