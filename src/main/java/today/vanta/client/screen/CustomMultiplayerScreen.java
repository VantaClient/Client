package today.vanta.client.screen;

import com.google.common.base.Charsets;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viamcp.ViaMCP;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class CustomMultiplayerScreen extends VantaScreen {
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_server.png");
    private final GlyphFontRenderer titleFont = CFonts.SFPT_SEMIBOLD_20;
    private final GlyphFontRenderer itemFont = CFonts.SFPT_MEDIUM_18;
    private final GlyphFontRenderer buttonFont = CFonts.SFPT_MEDIUM_18;
    private final GlyphFontRenderer labelFont = CFonts.SFPT_REGULAR_18;
    private final GlyphFontRenderer smallFont = CFonts.SFPT_REGULAR_18;
    private final GlyphFontRenderer inputFont = CFonts.SFPT_MEDIUM_18;

    private final GuiScreen parentScreen;

    private ServerList serverList;
    private int selectedIndex = -1;
    private int lastClickedIndex = -1;
    private final Set<Integer> selectedIndices = new HashSet<>();
    private float scrollAmount = 0;

    private final List<Component> buttons = new ArrayList<>();

    private enum Modal {
        NONE, ADD_SERVER, EDIT_SERVER, DIRECT_CONNECT, DELETE_CONFIRM
    }

    private Modal activeModal = Modal.NONE;
    private final List<Integer> pendingDeleteIndices = new ArrayList<>();

    private GuiTextField modalNameField;
    private GuiTextField modalIpField;
    private String modalTitle;
    private final List<Component> modalButtons = new ArrayList<>();

    private ServerData.ServerResourceMode pendingResourceMode = ServerData.ServerResourceMode.PROMPT;

    private boolean sliderDragging = false;

    private static final int SLOT_HEIGHT = 36;
    private static final int ICON_SIZE = 24;
    private static final int TEXT_FIELD_HEIGHT = 18;

    private float panelX, panelY, panelWidth, panelHeight;
    private float listTop, listBottom, listX, listWidth;
    private float btnY;

    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final Set<String> iconLoading = new HashSet<>();

    public CustomMultiplayerScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    protected void initScreen() {
        Keyboard.enableRepeatEvents(true);
        calculateLayout();
        loadServers();
        setupButtons();
        setupVersionSlider();
    }

    @Override
    public void updateScreen() {
        if (modalNameField != null) modalNameField.updateCursorCounter();
        if (modalIpField != null) modalIpField.updateCursorCounter();
    }

    private void calculateLayout() {
        panelWidth = Math.min(500, Math.max(360, width * 0.55f));
        panelHeight = Math.min(350, Math.max(200, height * 0.7f));
        panelX = (width - panelWidth) / 2f;
        panelY = (height - panelHeight) / 2f;

        listTop = panelY + 26;
        listBottom = panelY + panelHeight - 40;
        listX = panelX + 4;
        listWidth = panelWidth - 8;
        btnY = panelY + panelHeight - 30;
    }

    private void loadServers() {
        serverList = new ServerList(mc);
        selectedIndex = -1;
        lastClickedIndex = -1;
        selectedIndices.clear();
        scrollAmount = 0;
    }

    private void setupButtons() {
        buttons.clear();
        String[] labels = {"Join", "Direct", "Add", "Edit", "Delete", "Refresh", "Cancel"};
        int count = labels.length;
        float btnGap = 3;
        float btnHeight = 18;
        float totalBtnWidth = panelWidth - btnGap * (count + 1);
        float btnWidth = Math.min(58, totalBtnWidth / count);
        float totalWidth = count * btnWidth + (count - 1) * btnGap;
        float startX = panelX + (panelWidth - totalWidth) / 2;

        for (int i = 0; i < count; i++) {
            buttons.add(new ButtonComponent(labels[i], startX + i * (btnWidth + btnGap), btnY, btnWidth, btnHeight, buttonFont));
        }
    }

    private void setupVersionSlider() {
        ViaMCP.INSTANCE.initAsyncSlider();
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

        titleFont.drawString("Multiplayer", panelX + 6, panelY + 5, Color.WHITE);

        renderVersionSlider(event);

        int maxScroll = Math.max(0, serverList.countServers() * SLOT_HEIGHT - (int) (listBottom - listTop));
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        RenderUtil.scissor(listX, listTop, listWidth, listBottom - listTop, () -> {
            for (int i = 0; i < serverList.countServers(); i++) {
                ServerData server = serverList.getServerData(i);
                float slotY = listTop + i * SLOT_HEIGHT - scrollAmount;

                if (slotY + SLOT_HEIGHT < listTop || slotY > listBottom) continue;

                boolean selected = selectedIndices.contains(i);
                boolean hovered = event.mouseX >= listX && event.mouseX <= listX + listWidth
                        && event.mouseY >= slotY && event.mouseY <= slotY + SLOT_HEIGHT;

                if (selected) {
                    Rectangle.create(listX, slotY, listWidth, SLOT_HEIGHT)
                            .color(new Color(50, 50, 50)).push(event);
                } else if (hovered) {
                    Rectangle.create(listX, slotY, listWidth, SLOT_HEIGHT)
                            .color(new Color(40, 40, 40)).push(event);
                }

                float textX = listX + ICON_SIZE + 8;

                String name = server.serverName;
                String ip = server.serverIP;
                String motd = server.serverMOTD != null ? server.serverMOTD : "";

                drawServerIcon(server, listX + 4, slotY + (SLOT_HEIGHT - ICON_SIZE) / 2f);

                itemFont.drawString(name, textX, slotY + 4, Color.WHITE);

                String versionText = "1.8.x";
                if (server.version > 47) {
                    versionText = "1.9+";
                } else if (server.version < 47) {
                    versionText = "1.7-";
                }
                float nameWidth = itemFont.getStringWidth(name);
                smallFont.drawString("[" + versionText + "]", textX + nameWidth + 4, slotY + 6, new Color(120, 120, 120));

                smallFont.drawString(ip, textX, slotY + 16, new Color(160, 160, 160));

                if (server.pingToServer >= 0) {
                    String pingText = server.pingToServer + "ms";
                    if (server.populationInfo != null && !server.populationInfo.isEmpty()) {
                        pingText = server.populationInfo + " " + pingText;
                    }
                    float pingWidth = smallFont.getStringWidth(pingText);
                    smallFont.drawString(pingText, listX + listWidth - pingWidth - 2, slotY + 4, new Color(160, 160, 160));
                }

                if (!motd.isEmpty()) {
                    smallFont.drawString(motd, textX, slotY + 26, new Color(140, 140, 140));
                }
            }
        });

        if (serverList.countServers() == 0) {
            String noServers = "No servers found";
            float centerX = listX + listWidth / 2;
            float centerY = listTop + (listBottom - listTop) / 2;
            itemFont.drawString(noServers, centerX - (float) itemFont.getStringWidth(noServers) / 2, centerY - 6, new Color(120, 120, 120));
        }

        buttons.forEach(b -> b.draw(event));

        if (activeModal != Modal.NONE) {
            drawModal(event);
        }
    }

    private void renderVersionSlider(RenderScreenEvent event) {
        if (ViaMCP.INSTANCE.getAsyncVersionSlider() == null) return;

        String versionName = ViaLoadingBase.getInstance().getTargetVersion().getName();
        float sliderWidth = Math.min(130, panelWidth - 56);
        float sliderX = panelX + panelWidth - sliderWidth - 6;
        float sliderY = panelY + 5;

        Rectangle.create(sliderX - 2, sliderY, sliderWidth + 4, 20)
                .color(new Color(50, 50, 50)).push(event);

        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));
        float sliderValue = getSliderValue();
        int knobX = (int) (sliderX + 2 + sliderValue * (sliderWidth - 10));
        Gui.drawModalRectWithCustomSizedTexture(knobX, (int) sliderY, 0, 66, 4, 20, 256, 256);
        Gui.drawModalRectWithCustomSizedTexture(knobX + 4, (int) sliderY, 196, 66, 4, 20, 256, 256);

        smallFont.drawString(versionName, sliderX + 2, sliderY + 5, Color.WHITE);
    }

    private float getSliderValue() {
        List<ProtocolVersion> values = new ArrayList<>(ViaLoadingBase.PROTOCOLS);
        values.sort(Comparator.comparingInt(ProtocolVersion::getVersion));
        int index = values.indexOf(ViaLoadingBase.getInstance().getTargetVersion());
        if (index == -1) index = 0;
        return (float) index / Math.max(1, values.size() - 1);
    }

    private boolean isOverVersionSlider(int mouseX, int mouseY) {
        if (ViaMCP.INSTANCE.getAsyncVersionSlider() == null) return false;
        float sliderWidth = Math.min(130, panelWidth - 56);
        float sliderX = panelX + panelWidth - sliderWidth - 6;
        float sliderY = panelY + 5;
        return mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= sliderY && mouseY <= sliderY + 20;
    }

    private void applySliderPosition(int mouseX) {
        if (ViaMCP.INSTANCE.getAsyncVersionSlider() == null) return;
        float sliderWidth = Math.min(130, panelWidth - 56);
        float sliderX = panelX + panelWidth - sliderWidth - 6;

        List<ProtocolVersion> values = new ArrayList<>(ViaLoadingBase.PROTOCOLS);
        values.sort(Comparator.comparingInt(ProtocolVersion::getVersion));
        float sliderValue = (float) (mouseX - sliderX - 2) / (sliderWidth - 10);
        sliderValue = Math.max(0, Math.min(1, sliderValue));
        int idx = (int) Math.ceil(sliderValue * (values.size() - 1));
        ViaLoadingBase.getInstance().reload(values.get(idx));
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

    private void drawServerIcon(ServerData server, float x, float y) {
        ResourceLocation iconLocation = getOrCreateIcon(server);

        GlStateManager.enableBlend();
        if (iconLocation != null) {
            mc.getTextureManager().bindTexture(iconLocation);
            Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            mc.getTextureManager().bindTexture(UNKNOWN_SERVER);
            Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0, 0, ICON_SIZE, ICON_SIZE, 64, 64);
        }
        GlStateManager.disableBlend();
    }

    private ResourceLocation getOrCreateIcon(ServerData server) {
        String ip = server.serverIP;
        if (ip == null) return null;

        ResourceLocation cached = iconCache.get(ip);
        if (cached != null) return cached;

        String iconData = server.getBase64EncodedIconData();
        if (iconData == null || iconLoading.contains(ip)) return null;

        iconLoading.add(ip);
        ResourceLocation loc = new ResourceLocation("servers/" + ip + "/icon");

        try {
            ByteBuf bytebuf = Unpooled.copiedBuffer(iconData, Charsets.UTF_8);
            ByteBuf bytebuf1 = Base64.decode(bytebuf);
            BufferedImage bufferedimage = javax.imageio.ImageIO.read(new ByteBufInputStream(bytebuf1));
            bytebuf1.release();
            bytebuf.release();

            if (bufferedimage != null) {
                DynamicTexture dynTex = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), dynTex.getTextureData(), 0, bufferedimage.getWidth());
                dynTex.updateDynamicTexture();
                mc.getTextureManager().loadTexture(loc, dynTex);
                iconCache.put(ip, loc);
                return loc;
            }
        } catch (Exception ignored) {
        }

        iconLoading.remove(ip);
        return null;
    }

    private void drawModal(RenderScreenEvent event) {
        Rectangle.create(0, 0, width, height)
                .color(new Color(0, 0, 0, 150)).push(event);

        float dw, dh, dx, dy;

        if (activeModal == Modal.DELETE_CONFIRM) {
            dw = 200;
            dh = 80;
        } else if (activeModal == Modal.DIRECT_CONNECT) {
            dw = 260;
            dh = 100;
        } else {
            dw = 260;
            dh = 160;
        }

        dx = width / 2f - dw / 2;
        dy = height / 2f - dh / 2;

        Rectangle.create(dx, dy, dw, dh)
                .color(new Color(40, 40, 40)).push(event);
        titleFont.drawString(modalTitle, dx + 6, dy + 6, Color.WHITE);

        if (activeModal == Modal.DELETE_CONFIRM) {
            drawDeleteConfirmModal(dx, dy, dw, dh);
        } else {
            drawInputModal(dx, dy, dw, dh, event);
        }

        modalButtons.forEach(b -> b.draw(event));
    }

    private void drawDeleteConfirmModal(float dx, float dy, float dw, float dh) {
        String label;
        if (pendingDeleteIndices.size() == 1) {
            int idx = pendingDeleteIndices.get(0);
            label = idx >= 0 && idx < serverList.countServers()
                    ? "'" + serverList.getServerData(idx).serverName + "'" : "Unknown";
        } else {
            label = pendingDeleteIndices.size() + " servers";
        }
        smallFont.drawString(label, dx + 6, dy + 28, new Color(200, 200, 200));
    }

    private void drawInputModal(float dx, float dy, float dw, float dh, RenderScreenEvent event) {
        float fieldWidth = dw - 16;

        if (modalNameField != null) {
            labelFont.drawString("Server Name:", dx + 8, dy + 26, new Color(160, 160, 160));
            drawStyledTextField(modalNameField, dx + 8, dy + 40, fieldWidth, TEXT_FIELD_HEIGHT, event);
        }

        if (modalIpField != null) {
            float ipY = modalNameField != null ? dy + 62 : dy + 26;
            labelFont.drawString("Server Address:", dx + 8, ipY, new Color(160, 160, 160));
            drawStyledTextField(modalIpField, dx + 8, ipY + 14, fieldWidth, TEXT_FIELD_HEIGHT, event);
        }

        if (activeModal == Modal.ADD_SERVER || activeModal == Modal.EDIT_SERVER) {
            float rpY = dy + 98;
            labelFont.drawString("Resource Pack:", dx + 8, rpY, new Color(160, 160, 160));
            String rpLabel = "Prompt";
            if (pendingResourceMode == ServerData.ServerResourceMode.ENABLED) rpLabel = "Enabled";
            else if (pendingResourceMode == ServerData.ServerResourceMode.DISABLED) rpLabel = "Disabled";
            float rpLabelWidth = labelFont.getStringWidth(rpLabel);
            float rpBoxWidth = Math.max(60, rpLabelWidth + 12);
            float rpBoxX = dx + 8;
            float rpBoxY = rpY + 14;

            Rectangle.create(rpBoxX, rpBoxY, rpBoxWidth, TEXT_FIELD_HEIGHT)
                    .color(new Color(20, 20, 20)).push(event);
            Rectangle.create(rpBoxX, rpBoxY, rpBoxWidth, TEXT_FIELD_HEIGHT)
                    .color(new Color(50, 50, 50)).outline(true).push(event);
            float textY = rpBoxY + (TEXT_FIELD_HEIGHT - labelFont.getFontHeight() - 4) / 2f;
            labelFont.drawString(rpLabel, rpBoxX + (rpBoxWidth - rpLabelWidth) / 2f, textY, Color.WHITE);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (activeModal != Modal.NONE) {
            handleModalClick(mouseX, mouseY, mouseButton);
            return;
        }

        if (isOverVersionSlider(mouseX, mouseY)) {
            applySliderPosition(mouseX);
            sliderDragging = true;
            return;
        }

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            int clicked = (int) ((mouseY - listTop + scrollAmount) / SLOT_HEIGHT);
            if (clicked >= 0 && clicked < serverList.countServers()) {
                boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

                if (ctrl && shift && lastClickedIndex >= 0) {
                    int from = Math.min(lastClickedIndex, clicked);
                    int to = Math.max(lastClickedIndex, clicked);
                    for (int i = from; i <= to; i++) {
                        selectedIndices.add(i);
                    }
                    selectedIndex = clicked;
                } else if (ctrl) {
                    if (selectedIndices.contains(clicked)) {
                        selectedIndices.remove(clicked);
                    } else {
                        selectedIndices.add(clicked);
                    }
                    selectedIndex = clicked;
                } else if (clicked == selectedIndex && mouseButton == 0) {
                    joinServer(clicked);
                } else {
                    selectedIndices.clear();
                    selectedIndices.add(clicked);
                    selectedIndex = clicked;
                }
                lastClickedIndex = clicked;
            } else {
                selectedIndex = -1;
                lastClickedIndex = -1;
                selectedIndices.clear();
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

    private void handleModalClick(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (activeModal == Modal.ADD_SERVER || activeModal == Modal.EDIT_SERVER) {
            float dw = 260;
            float dh = 160;
            float dx = width / 2f - dw / 2;
            float dy = height / 2f - dh / 2;
            float rpBoxX = dx + 8;
            float rpBoxY = dy + 112;
            float rpBoxWidth = Math.max(60, labelFont.getStringWidth("Disabled") + 12);

            if (mouseX >= rpBoxX && mouseX <= rpBoxX + rpBoxWidth
                    && mouseY >= rpBoxY && mouseY <= rpBoxY + TEXT_FIELD_HEIGHT) {
                ServerData.ServerResourceMode[] modes = ServerData.ServerResourceMode.values();
                pendingResourceMode = modes[(pendingResourceMode.ordinal() + 1) % modes.length];
            }
        }

        if (modalNameField != null) modalNameField.mouseClicked(mouseX, mouseY, mouseButton);
        if (modalIpField != null) modalIpField.mouseClicked(mouseX, mouseY, mouseButton);

        String clickedText = null;
        for (Component btn : modalButtons) {
            if (btn.click(mouseX, mouseY, 0)) {
                clickedText = btn.text;
                break;
            }
        }
        if (clickedText != null) {
            handleModalButton(clickedText);
        }
    }

    private void handleModalButton(String text) {
        switch (text) {
            case "Done":
                if (activeModal == Modal.DELETE_CONFIRM) {
                    performDelete();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.ADD_SERVER) {
                    addServer();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.EDIT_SERVER) {
                    editServer();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.DIRECT_CONNECT) {
                    directConnect();
                    activeModal = Modal.NONE;
                }
                break;
            case "Cancel":
                activeModal = Modal.NONE;
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeModal != Modal.NONE) {
            if (modalNameField != null) modalNameField.textboxKeyTyped(typedChar, keyCode);
            if (modalIpField != null) modalIpField.textboxKeyTyped(typedChar, keyCode);

            if (keyCode == 1) {
                activeModal = Modal.NONE;
                return;
            }
            if (keyCode == 28 || keyCode == 156) {
                if (activeModal == Modal.ADD_SERVER) {
                    addServer();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.EDIT_SERVER) {
                    editServer();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.DIRECT_CONNECT) {
                    directConnect();
                    activeModal = Modal.NONE;
                } else if (activeModal == Modal.DELETE_CONFIRM) {
                    performDelete();
                    activeModal = Modal.NONE;
                }
                return;
            }
            if (keyCode == 15) {
                if (modalNameField != null && modalIpField != null) {
                    boolean nameFocused = modalNameField.isFocused();
                    modalNameField.setFocused(!nameFocused);
                    modalIpField.setFocused(nameFocused);
                }
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

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (sliderDragging) {
            applySliderPosition(mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        sliderDragging = false;
    }

    private String pendingName;
    private String pendingIp;

    private void openAddServerModal() {
        activeModal = Modal.ADD_SERVER;
        modalTitle = "Add Server";
        pendingName = "Minecraft Server";
        pendingIp = "";
        pendingResourceMode = ServerData.ServerResourceMode.PROMPT;
        layoutModalFields();
        setupModalButtons();
    }

    private void openEditServerModal() {
        if (selectedIndex < 0 || selectedIndex >= serverList.countServers()) return;
        ServerData original = serverList.getServerData(selectedIndex);
        activeModal = Modal.EDIT_SERVER;
        modalTitle = "Edit Server";
        pendingName = original.serverName;
        pendingIp = original.serverIP;
        pendingResourceMode = original.getResourceMode();
        layoutModalFields();
        setupModalButtons();
    }

    private void openDirectConnectModal() {
        activeModal = Modal.DIRECT_CONNECT;
        modalTitle = "Direct Connect";
        pendingName = null;
        pendingIp = mc.gameSettings.lastServer;
        layoutModalFields();
        setupModalButtons();
    }

    private void openDeleteConfirmModal() {
        activeModal = Modal.DELETE_CONFIRM;
        modalTitle = pendingDeleteIndices.size() == 1 ? "Delete Server?" : "Delete Servers?";
        pendingName = null;
        pendingIp = null;
        modalNameField = null;
        modalIpField = null;
        setupModalButtons();
    }

    private void layoutModalFields() {
        float dw, dh, dx, dy;
        if (activeModal == Modal.DELETE_CONFIRM) {
            dw = 200;
            dh = 80;
        } else if (activeModal == Modal.DIRECT_CONNECT) {
            dw = 260;
            dh = 100;
        } else {
            dw = 260;
            dh = 160;
        }
        dx = width / 2f - dw / 2;
        dy = height / 2f - dh / 2;

        int fieldWidth = (int) (dw - 16);

        if (activeModal == Modal.ADD_SERVER || activeModal == Modal.EDIT_SERVER) {
            modalNameField = new GuiTextField(0, mc.fontRendererObj, (int) (dx + 8), (int) (dy + 40), fieldWidth, TEXT_FIELD_HEIGHT);
            modalNameField.setMaxStringLength(128);
            modalNameField.setFocused(true);
            modalNameField.setText(pendingName != null ? pendingName : "Minecraft Server");
            modalNameField.setEnableBackgroundDrawing(false);

            modalIpField = new GuiTextField(1, mc.fontRendererObj, (int) (dx + 8), (int) (dy + 76), fieldWidth, TEXT_FIELD_HEIGHT);
            modalIpField.setMaxStringLength(128);
            modalIpField.setFocused(false);
            modalIpField.setText(pendingIp != null ? pendingIp : "");
            modalIpField.setEnableBackgroundDrawing(false);
        } else if (activeModal == Modal.DIRECT_CONNECT) {
            modalNameField = null;
            modalIpField = new GuiTextField(1, mc.fontRendererObj, (int) (dx + 8), (int) (dy + 40), fieldWidth, TEXT_FIELD_HEIGHT);
            modalIpField.setMaxStringLength(128);
            modalIpField.setFocused(true);
            modalIpField.setText(pendingIp != null ? pendingIp : "");
            modalIpField.setEnableBackgroundDrawing(false);
        }
    }

    private void setupModalButtons() {
        modalButtons.clear();
        float dw = activeModal == Modal.DELETE_CONFIRM ? 200 : 260;
        float dh;
        if (activeModal == Modal.DELETE_CONFIRM) dh = 80;
        else if (activeModal == Modal.DIRECT_CONNECT) dh = 100;
        else dh = 160;
        float dx = width / 2f - dw / 2;
        float dy = height / 2f - dh / 2;

        float btnWidth = 75;
        float btnHeight = 18;
        float btnY = dy + dh - 24;

        String doneLabel = "Done";
        Color doneColor = new Color(50, 50, 50);

        if (activeModal == Modal.DELETE_CONFIRM) {
            doneLabel = "Delete";
            doneColor = new Color(180, 50, 50);
        }

        ButtonComponent doneBtn = new ButtonComponent(doneLabel, dx + dw / 2 - btnWidth - 2, btnY, btnWidth, btnHeight, buttonFont);
        ButtonComponent cancelBtn = new ButtonComponent("Cancel", dx + dw / 2 + 2, btnY, btnWidth, btnHeight, buttonFont);

        modalButtons.add(doneBtn);
        modalButtons.add(cancelBtn);
    }

    private void addServer() {
        String name = modalNameField != null ? modalNameField.getText().trim() : "";
        String ip = modalIpField != null ? modalIpField.getText().trim() : "";
        if (name.isEmpty()) name = "Minecraft Server";
        if (ip.isEmpty()) return;

        ServerData server = new ServerData(name, ip, false);
        server.setResourceMode(pendingResourceMode);
        serverList.addServerData(server);
        serverList.saveServerList();
    }

    private void editServer() {
        if (selectedIndex < 0 || selectedIndex >= serverList.countServers()) return;
        String name = modalNameField != null ? modalNameField.getText().trim() : "";
        String ip = modalIpField != null ? modalIpField.getText().trim() : "";
        if (name.isEmpty()) name = "Minecraft Server";
        if (ip.isEmpty()) return;

        ServerData original = serverList.getServerData(selectedIndex);
        original.serverName = name;
        original.serverIP = ip;
        original.setResourceMode(pendingResourceMode);
        serverList.saveServerList();
    }

    private void directConnect() {
        String ip = modalIpField != null ? modalIpField.getText().trim() : "";
        if (ip.isEmpty()) return;

        mc.gameSettings.lastServer = ip;
        ServerData server = new ServerData("Minecraft Server", ip, false);
        mc.displayGuiScreen(new GuiConnecting(this, mc, server));
    }

    private void handleButton(String text) {
        switch (text) {
            case "Join":
                if (selectedIndex >= 0 && selectedIndex < serverList.countServers()) {
                    joinServer(selectedIndex);
                }
                break;
            case "Direct":
                openDirectConnectModal();
                break;
            case "Add":
                openAddServerModal();
                break;
            case "Edit":
                openEditServerModal();
                break;
            case "Delete":
                if (!selectedIndices.isEmpty()) {
                    pendingDeleteIndices.clear();
                    pendingDeleteIndices.addAll(selectedIndices);
                    openDeleteConfirmModal();
                }
                break;
            case "Refresh":
                loadServers();
                setupButtons();
                break;
            case "Cancel":
                mc.displayGuiScreen(parentScreen);
                break;
        }
    }

    private void joinServer(int index) {
        if (mc.theWorld != null) return;
        ServerData server = serverList.getServerData(index);
        mc.displayGuiScreen(new GuiConnecting(this, mc, server));
    }

    private void performDelete() {
        List<Integer> sorted = new ArrayList<>(pendingDeleteIndices);
        sorted.sort((a, b) -> Integer.compare(b, a));
        for (int idx : sorted) {
            if (idx >= 0 && idx < serverList.countServers()) {
                serverList.removeServerData(idx);
            }
        }
        serverList.saveServerList();
        selectedIndex = -1;
        lastClickedIndex = -1;
        selectedIndices.clear();
        loadServers();
        setupButtons();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
}
