package today.vanta.client.screen;

import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveFormatComparator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.screen.component.Component;
import today.vanta.client.screen.component.impl.ButtonComponent;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.os.Enhancements;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CustomSingleplayerScreen extends VantaScreen {
    private static final Logger logger = LogManager.getLogger();
    private final GlyphFontRenderer titleFont = CFonts.SFPT_SEMIBOLD_20;
    private final GlyphFontRenderer itemFont = CFonts.SFPT_MEDIUM_18;
    private final GlyphFontRenderer buttonFont = CFonts.SFPT_MEDIUM_18;
    private final GlyphFontRenderer smallFont = CFonts.SFPT_REGULAR_18;
    private final GlyphFontRenderer inputFont = CFonts.SFPT_MEDIUM_18;
    private final GlyphFontRenderer labelFont = CFonts.SFPT_REGULAR_18;

    private final GuiScreen parentScreen;
    private final DateFormat dateFormat = new SimpleDateFormat();

    private List<SaveFormatComparator> worlds = new ArrayList<>();
    private int selectedIndex = -1;
    private float scrollAmount = 0;

    private final List<Component> buttons = new ArrayList<>();

    private boolean confirmDelete = false;
    private int pendingDeleteIndex = -1;
    private boolean loadingWorld = false;

    private static final int SLOT_HEIGHT = 36;
    private static final int TEXT_FIELD_HEIGHT = 18;

    private float panelX, panelY, panelWidth, panelHeight;
    private float listTop, listBottom, listX, listWidth;
    private float btnY;

    private enum Modal { NONE, RENAME }
    private Modal activeModal = Modal.NONE;
    private String modalTitle;
    private GuiTextField renameField;
    private final List<Component> modalButtons = new ArrayList<>();

    public CustomSingleplayerScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    protected void initScreen() {
        Keyboard.enableRepeatEvents(true);
        loadingWorld = false;
        calculateLayout();
        loadWorlds();
        setupButtons();
        if (activeModal != Modal.NONE) {
            layoutModalField();
            setupModalButtons();
        }
    }

    private void calculateLayout() {
        panelWidth = Math.min(400, Math.max(300, width * 0.5f));
        panelHeight = Math.min(350, Math.max(200, height * 0.7f));
        panelX = (width - panelWidth) / 2f;
        panelY = (height - panelHeight) / 2f;

        listTop = panelY + 26;
        listBottom = panelY + panelHeight - 40;
        listX = panelX + 4;
        listWidth = panelWidth - 8;
        btnY = panelY + panelHeight - 30;
    }

    private void loadWorlds() {
        try {
            ISaveFormat saveFormat = mc.getSaveLoader();
            worlds = new ArrayList<>(saveFormat.getSaveList());
            Collections.sort(worlds);
            selectedIndex = -1;
            scrollAmount = 0;
        } catch (AnvilConverterException e) {
            logger.error("Couldn't load level list", e);
            mc.displayGuiScreen(new GuiErrorScreen("Unable to load worlds", e.getMessage()));
        }
    }

    private void setupButtons() {
        buttons.clear();
        String[] labels = {"Play", "Create", "Rename", "Delete", "Cancel"};
        int count = labels.length;
        float btnGap = 4;
        float btnHeight = 18;
        float totalBtnWidth = panelWidth - btnGap * (count + 1);
        float btnWidth = Math.min(65, totalBtnWidth / count);
        float totalWidth = count * btnWidth + (count - 1) * btnGap;
        float startX = panelX + (panelWidth - totalWidth) / 2;

        for (int i = 0; i < count; i++) {
            buttons.add(new ButtonComponent(labels[i], startX + i * (btnWidth + btnGap), btnY, btnWidth, btnHeight, buttonFont));
        }
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

        Rectangle
                .create(panelX, panelY, panelWidth, panelHeight)
                .color(new Color(30, 30, 30))
                .push(event);

        titleFont.drawString("Select World", panelX + 6, panelY + 5, Color.WHITE);

        int maxScroll = Math.max(0, worlds.size() * SLOT_HEIGHT - (int) (listBottom - listTop));
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        RenderUtil.scissor(listX, listTop, listWidth, listBottom - listTop, () -> {
            for (int i = 0; i < worlds.size(); i++) {
                SaveFormatComparator world = worlds.get(i);
                float slotY = listTop + i * SLOT_HEIGHT - scrollAmount;

                if (slotY + SLOT_HEIGHT < listTop || slotY > listBottom) continue;

                boolean selected = i == selectedIndex;
                boolean hovered = event.mouseX >= listX && event.mouseX <= listX + listWidth
                        && event.mouseY >= slotY && event.mouseY <= slotY + SLOT_HEIGHT;

                if (selected) {
                    Rectangle.create(listX, slotY, listWidth, SLOT_HEIGHT)
                            .color(new Color(50, 50, 50)).push(event);
                } else if (hovered) {
                    Rectangle.create(listX, slotY, listWidth, SLOT_HEIGHT)
                            .color(new Color(40, 40, 40)).push(event);
                }

                String displayName = world.getDisplayName();
                if (StringUtils.isEmpty(displayName)) {
                    displayName = "World " + (i + 1);
                }

                String info = world.getFileName() + " (" + dateFormat.format(new Date(world.getLastTimePlayed())) + ")";
                String gameMode;

                if (world.requiresConversion()) {
                    gameMode = "Needs conversion";
                } else {
                    gameMode = world.getEnumGameType().getName();
                    if (world.isHardcoreModeEnabled()) {
                        gameMode = EnumChatFormatting.DARK_RED + "Hardcore" + EnumChatFormatting.RESET;
                    }
                    if (world.getCheatsEnabled()) {
                        gameMode += ", Cheats";
                    }
                }

                itemFont.drawString(displayName, listX + 4, slotY + 4, Color.WHITE);
                smallFont.drawString(info, listX + 4, slotY + 16, new Color(160, 160, 160));
                smallFont.drawString(gameMode, listX + 4, slotY + 26, new Color(140, 140, 140));
            }
        });

        if (worlds.isEmpty()) {
            String noWorlds = "No worlds found";
            float centerX = listX + listWidth / 2;
            float centerY = listTop + (listBottom - listTop) / 2;
            itemFont.drawString(noWorlds, centerX - itemFont.getStringWidth(noWorlds) / 2, centerY - 6, new Color(120, 120, 120));
        }

        buttons.forEach(b -> b.draw(event));

        if (confirmDelete) {
            drawConfirmDialog(event);
        }

        if (activeModal != Modal.NONE) {
            drawModal(event);
        }
    }

    private void drawConfirmDialog(RenderScreenEvent event) {
        Rectangle.create(0, 0, width, height)
                .color(new Color(0, 0, 0, 150)).push(event);

        float dw = 200, dh = 80;
        float dx = width / 2f - dw / 2, dy = height / 2f - dh / 2;

        Rectangle.create(dx, dy, dw, dh)
                .color(new Color(40, 40, 40)).push(event);
        titleFont.drawString("Delete World?", dx + 6, dy + 6, Color.WHITE);

        String name = pendingDeleteIndex >= 0 && pendingDeleteIndex < worlds.size()
                ? worlds.get(pendingDeleteIndex).getDisplayName() : "Unknown";
        smallFont.drawString("'" + name + "'", dx + 6, dy + 28, new Color(200, 200, 200));

        float cbx = dx + 10, cby = dy + dh - 26;
        Rectangle.create(cbx, cby, 75, 18)
                .color(new Color(180, 50, 50)).push(event);
        buttonFont.drawString("Delete", cbx + 18, cby + 2, Color.WHITE);

        float xbx = dx + dw - 85;
        Rectangle.create(xbx, cby, 75, 18)
                .color(new Color(60, 60, 60)).push(event);
        buttonFont.drawString("Cancel", xbx + 12, cby + 2, Color.WHITE);
    }

    private void drawModal(RenderScreenEvent event) {
        Rectangle.create(0, 0, width, height)
                .color(new Color(0, 0, 0, 150)).push(event);

        float dw = 260, dh = 100;
        float dx = width / 2f - dw / 2, dy = height / 2f - dh / 2;

        Rectangle.create(dx, dy, dw, dh)
                .color(new Color(40, 40, 40)).push(event);
        titleFont.drawString(modalTitle, dx + 6, dy + 6, Color.WHITE);

        float fieldWidth = dw - 16;

        if (renameField != null) {
            labelFont.drawString("World Name:", dx + 8, dy + 26, new Color(160, 160, 160));
            drawStyledTextField(renameField, dx + 8, dy + 40, fieldWidth, TEXT_FIELD_HEIGHT, event);
        }

        modalButtons.forEach(b -> b.draw(event));
    }

    private void drawStyledTextField(GuiTextField field, float x, float y, float w, float h, RenderScreenEvent event) {
        Rectangle.create(x, y, w, h)
                .color(new Color(20, 20, 20)).push(event);
        Rectangle.create(x, y, w, h)
                .color(new Color(50, 50, 50)).outline(true).push(event);

        String text = field.getText();
        int cursorPos = field.getCursorPosition();
        int selStart = field.getSelectionEnd();
        boolean focused = field.isFocused();

        float textY = y + (h - inputFont.getFontHeight() - 4) / 2f;

        if (focused && selStart != cursorPos) {
            int minSel = Math.min(cursorPos, selStart);
            int maxSel = Math.max(cursorPos, selStart);
            String before = text.substring(0, Math.min(minSel, text.length()));
            String selected = text.substring(Math.min(minSel, text.length()), Math.min(maxSel, text.length()));
            float selX = x + 4 + inputFont.getStringWidth(before);
            float selW = inputFont.getStringWidth(selected);
            Rectangle.create(selX, textY, selW, inputFont.getFontHeight() + 4)
                    .color(new Color(80, 120, 200)).push(event);
        }

        inputFont.drawString(text, x + 4, textY, Color.WHITE);

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            String beforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
            float cursorX = x + 4 + inputFont.getStringWidth(beforeCursor);
            int actualRenderedHeight = inputFont.getFontHeight() + 4;
            Rectangle.create(cursorX, textY, 1, actualRenderedHeight)
                    .color(Color.WHITE).push(event);
        }
    }

    private void layoutModalField() {
        float dw = 260, dh = 100;
        float dx = width / 2f - dw / 2, dy = height / 2f - dh / 2;
        int fieldWidth = (int) (dw - 16);

        if (activeModal == Modal.RENAME) {
            renameField = new GuiTextField(0, mc.fontRendererObj, (int) (dx + 8), (int) (dy + 40), fieldWidth, TEXT_FIELD_HEIGHT);
            renameField.setMaxStringLength(128);
            renameField.setFocused(true);
            renameField.setText(worlds.get(selectedIndex).getDisplayName());
            renameField.setEnableBackgroundDrawing(false);
        }
    }

    private void setupModalButtons() {
        modalButtons.clear();
        float dw = 260, dh = 100;
        float dx = width / 2f - dw / 2, dy = height / 2f - dh / 2;

        float btnWidth = 75;
        float btnHeight = 18;
        float btnY = dy + dh - 24;

        ButtonComponent doneBtn = new ButtonComponent("Done", dx + dw / 2 - btnWidth - 2, btnY, btnWidth, btnHeight, buttonFont);
        ButtonComponent cancelBtn = new ButtonComponent("Cancel", dx + dw / 2 + 2, btnY, btnWidth, btnHeight, buttonFont);

        modalButtons.add(doneBtn);
        modalButtons.add(cancelBtn);
    }

    private void openRenameModal() {
        if (selectedIndex < 0 || selectedIndex >= worlds.size()) return;
        activeModal = Modal.RENAME;
        modalTitle = "Rename World";
        layoutModalField();
        setupModalButtons();
    }

    private void renameWorld() {
        if (selectedIndex < 0 || selectedIndex >= worlds.size() || renameField == null) return;
        String newName = renameField.getText().trim();
        if (newName.isEmpty()) return;

        ISaveFormat saveFormat = mc.getSaveLoader();
        saveFormat.renameWorld(worlds.get(selectedIndex).getFileName(), newName);
        loadWorlds();
        setupButtons();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (activeModal != Modal.NONE) {
            handleModalClick(mouseX, mouseY, mouseButton);
            return;
        }

        if (confirmDelete) {
            float dw = 200, dh = 80;
            float dx = width / 2f - dw / 2, dy = height / 2f - dh / 2;
            float cbx = dx + 10, cby = dy + dh - 26;
            float xbx = dx + dw - 85;

            if (RenderUtil.hovered(mouseX, mouseY, cbx, cby, 75, 18)) {
                performDelete(pendingDeleteIndex);
                confirmDelete = false;
                return;
            }
            if (RenderUtil.hovered(mouseX, mouseY, xbx, cby, 75, 18)) {
                confirmDelete = false;
                return;
            }
            return;
        }

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            int clicked = (int) ((mouseY - listTop + scrollAmount) / SLOT_HEIGHT);
            if (clicked >= 0 && clicked < worlds.size()) {
                if (clicked == selectedIndex && mouseButton == 0) {
                    playWorld(clicked);
                } else {
                    selectedIndex = clicked;
                }
            } else {
                selectedIndex = -1;
            }
        }

        String clickedText = null;
        for (Component btn : buttons) {
            if (btn.click(mouseX, mouseY, 0)) {
                clickedText = btn.text;
                break;
            }
        }
        if (clickedText != null) {
            handleButton(clickedText);
        }
    }

    private void handleModalClick(int mouseX, int mouseY, int mouseButton) {
        if (renameField != null) renameField.mouseClicked(mouseX, mouseY, mouseButton);

        String clickedText = null;
        for (Component btn : modalButtons) {
            if (btn.click(mouseX, mouseY, 0)) {
                clickedText = btn.text;
                break;
            }
        }

        if (clickedText != null) {
            switch (clickedText) {
                case "Done":
                    renameWorld();
                    activeModal = Modal.NONE;
                    break;
                case "Cancel":
                    activeModal = Modal.NONE;
                    break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeModal != Modal.NONE) {
            if (renameField != null) renameField.textboxKeyTyped(typedChar, keyCode);

            if (keyCode == 1) {
                activeModal = Modal.NONE;
                return;
            }
            if (keyCode == 28 || keyCode == 156) {
                renameWorld();
                activeModal = Modal.NONE;
                return;
            }
            return;
        }

        if (keyCode == 1) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            scrollAmount -= dWheel * 6;
        }
    }

    private void handleButton(String text) {
        switch (text) {
            case "Play":
                if (selectedIndex >= 0 && selectedIndex < worlds.size()) {
                    playWorld(selectedIndex);
                }
                break;
            case "Create":
                mc.displayGuiScreen(new GuiCreateWorld(this));
                break;
            case "Rename":
                openRenameModal();
                break;
            case "Delete":
                if (selectedIndex >= 0 && selectedIndex < worlds.size()) {
                    confirmDelete = true;
                    pendingDeleteIndex = selectedIndex;
                }
                break;
            case "Cancel":
                mc.displayGuiScreen(parentScreen);
                break;
        }
    }

    private void playWorld(int index) {
        if (loadingWorld) return;

        SaveFormatComparator world = worlds.get(index);
        String fileName = world.getFileName();
        String displayName = world.getDisplayName();
        if (fileName == null) fileName = "World" + index;
        if (displayName == null) displayName = "World" + index;

        if (!mc.getSaveLoader().canLoadWorld(fileName)) {
            return;
        }

        loadingWorld = true;
        onGuiClosed();
        mc.currentScreen = null;
        mc.launchIntegratedServer(fileName, displayName, null);
    }

    private void performDelete(int index) {
        if (index >= 0 && index < worlds.size()) {
            ISaveFormat saveFormat = mc.getSaveLoader();
            saveFormat.flushCache();
            saveFormat.deleteWorldDirectory(worlds.get(index).getFileName());
            loadWorlds();
            setupButtons();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
}
