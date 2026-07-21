package today.vanta.client.screen;

import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClickGUI;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ImageUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.Icons;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BoxyClickGUIScreen extends VantaScreen {
    private static final float SIDEBAR_WIDTH = 70;
    private static final float SIDEBAR_HEADER_HEIGHT = 22.5f;
    private static final float RESIZE_HANDLE_SIZE = 10;
    private static final float MIN_WIDTH = 250;
    private static final float MIN_HEIGHT = 190;

    private static final float SETTING_INNER_PAD = 8;
    private static final float SETTING_SPACING = 5;
    private static final float SETTING_LABEL_HEIGHT = 10;
    private static final float SETTING_LABEL_GAP = 2;

    private static final float CHECKBOX_SIZE = 8;
    private static final float CHECKBOX_FILL_SIZE = 4;

    private static final float SLIDER_CONTROL_HEIGHT = 12;
    private static final float SLIDER_TRACK_HEIGHT = 4;
    private static final float SLIDER_THUMB_WIDTH = 4;
    private static final float SLIDER_THUMB_HEIGHT = 8;
    private static final float SLIDER_VALUE_WIDTH = 30;

    private static final float DROPDOWN_HEADER_HEIGHT = 14;
    private static final float DROPDOWN_ITEM_HEIGHT = 14;
    private static final float MULTI_CHECK_SIZE = 6;

    private static final Color GRAY_20 = new Color(20, 20, 20);
    private static final Color GRAY_30 = new Color(30, 30, 30);
    private static final Color GRAY_3C = new Color(0x3c3c3c);
    private static final Color GRAY_60 = new Color(0x606060);
    private static final Color TEXT_MAIN = new Color(0xe0e0e0);
    private static final Color TEXT_MUTED = new Color(0x888888);

    private static final GlyphFontRenderer ICONS_16 = CFonts.getFont("Icons", 16, Icons.CHARS);
    private static final GlyphFontRenderer ICONS_12 = CFonts.getFont("Icons", 12, Icons.CHARS);

    public float sWidth = 250, sHeight = 190;
    public float x = -999, y = -999;
    private Category selectedCat = Category.COMBAT;
    private float moduleScroll;

    private boolean dragging, resizing;
    private float dragOffsetX, dragOffsetY;

    private NumberSetting draggingSlider;
    private float draggingTrackX, draggingTrackWidth;

    @Override
    protected void initScreen() {
        if (x == -999 || y == -999) {
            x = width / 2f - sWidth / 2;
            y = height / 2f - sHeight / 2;
        }
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        int overlayAlpha = mc.theWorld != null ? 150 : 255;
        if (Vanta.instance.moduleStorage.getT(ClickGUI.class).darkenBackground.getValue()) {
            Rectangle.create(0, 0, width, height)
                    .color(new Color(0, 0, 0, overlayAlpha))
                    .push(event);
        }

        Color color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];

        if (Vanta.instance.moduleStorage.getT(ClickGUI.class).gradientBackground.getValue()) {
            GradientRectangle.create(0, 0, width, height)
                    .firstColor(new Color(0, 0, 0, overlayAlpha))
                    .secondColor(new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), overlayAlpha))
                    .gradientMode(GradientMode.VERTICAL)
                    .push(event);
        }

        if (Vanta.instance.moduleStorage.getT(ClickGUI.class).image.getValue()) {
            float imgWidth = 300;
            float imgHeight = 300;

            String texture = Vanta.instance.moduleStorage.getT(ClickGUI.class).mascot.getValue() + ".png";
            if (texture.startsWith("longboy")) {
                imgHeight = 400;
            }

            ImageRectangle
                    .create(width - 175 - (imgWidth / 2), height - 210 - (imgHeight / 2), imgWidth, imgHeight, -1)
                    .resource(ImageUtil.getTexture(texture))
                    .push(event);
        }

        drawBox(event);
        drawResizeHandle(event);
        RenderUtil.scissor(x, y, sWidth, sHeight, () -> {
            drawSidebar(event);
            drawCategories(event);
            if (selectedCat != null)
                drawModules(event);
        });

        if (dragging) {
            x = event.mouseX - dragOffsetX;
            y = event.mouseY - dragOffsetY;
        }

        if (resizing) {
            sWidth = Math.max(MIN_WIDTH, event.mouseX - x);
            sHeight = Math.max(MIN_HEIGHT, event.mouseY - y);
        }

        if (draggingSlider != null) {
            updateSliderValue(draggingSlider, event.mouseX, draggingTrackX, draggingTrackWidth);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!RenderUtil.hovered(mouseX, mouseY, x, y, sWidth, sHeight)) return;

        if (mouseButton == 0 && RenderUtil.hovered(mouseX, mouseY, x + sWidth - RESIZE_HANDLE_SIZE, y + sHeight - RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)) {
            resizing = true;
            return;
        }

        if (mouseButton == 0 && RenderUtil.hovered(mouseX, mouseY, x, y, SIDEBAR_WIDTH, SIDEBAR_HEADER_HEIGHT)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return;
        }

        clickCategory(mouseX, mouseY, mouseButton);
        clickModules(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        resizing = false;
        draggingSlider = null;
        draggingTrackX = 0;
        draggingTrackWidth = 0;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            moduleScroll += wheel / 15f;
            moduleScroll = MathHelper.clamp_float(moduleScroll, getModuleListMinScroll(), 0);
        }
    }

    private void drawBox(RenderScreenEvent event) {
        Rectangle
                .create(x - 0.5, y - 0.5, sWidth + 1, sHeight + 1)
                .color(GRAY_3C)
                .push(event);

        Rectangle
                .create(x, y, sWidth, sHeight)
                .color(GRAY_30)
                .push(event);
    }

    private void drawResizeHandle(RenderScreenEvent event) {
        float gripSize = 6;
        float gx = x + sWidth - gripSize - 2.5f;
        float gy = y + sHeight - gripSize - 2.5f;

        Rectangle.create(gx + gripSize - 4, gy + gripSize - 0.5f, 4, 0.5f).color(GRAY_60).push(event);
        Rectangle.create(gx + gripSize - 0.5f, gy, 0.5f, gripSize).color(GRAY_60).push(event);
    }

    private void drawSidebar(RenderScreenEvent event) {
        Rectangle
                .create(x, y, SIDEBAR_WIDTH, sHeight)
                .color(GRAY_20)
                .push(event);

        Rectangle
                .create(x + SIDEBAR_WIDTH, y, 0.5, sHeight)
                .color(GRAY_3C)
                .push(event);

        CFonts.getFont("SFPT-Semibold", 12).drawString("CLICKGUI", x + 7.5f, y + 7.5f, Color.WHITE);
    }

    private void drawCategories(RenderScreenEvent event) {
        float yOffset = y + 7.5f + 7.5f + 7.5f;
        for (Category cat : Category.values()) {
            boolean over = RenderUtil.hovered(event.mouseX, event.mouseY, x, yOffset, SIDEBAR_WIDTH, 20);
            boolean selected = cat.equals(selectedCat);

            Rectangle
                    .create(x, yOffset, SIDEBAR_WIDTH, 20)
                    .color(over ? GRAY_30 : selected ? GRAY_30 : GRAY_20)
                    .push(event);

            Rectangle
                    .create(x, yOffset, SIDEBAR_WIDTH, 0.5)
                    .color(GRAY_3C)
                    .push(event);

            Rectangle
                    .create(x, yOffset + 20, SIDEBAR_WIDTH, 0.5)
                    .color(GRAY_3C)
                    .push(event);

            if (selected) {
                Rectangle
                        .create(x, yOffset, 1.5, 20)
                        .color(GRAY_60)
                        .push(event);
            }

            float textX = x + 6.5f;

            CFonts.getFont("SFPT-Regular", 12).drawString(cat.name, textX + 13, yOffset + 6.5f, selected ? Color.WHITE : over ? TEXT_MAIN : TEXT_MUTED);
            ICONS_16.drawString(cat.icon + "", textX, yOffset + 6.5f, selected ? Color.WHITE : over ? TEXT_MAIN : TEXT_MUTED);

            yOffset += 20;
        }
    }

    private void drawModules(RenderScreenEvent event) {
        moduleScroll = MathHelper.clamp_float(moduleScroll, getModuleListMinScroll(), 0);

        float xOffset = x + SIDEBAR_WIDTH + 10;
        float yOffset = y + 9.5f + moduleScroll;
        float moduleWidth = sWidth - SIDEBAR_WIDTH - 10 * 2;

        for (Module mod : Vanta.instance.moduleStorage.getModulesByCategory(selectedCat)) {
            float moduleHeight = 18;

            boolean over = RenderUtil.hovered(event.mouseX, event.mouseY, xOffset, yOffset, moduleWidth, moduleHeight);

            Rectangle
                    .create(xOffset, yOffset, moduleWidth, moduleHeight)
                    .color(over ? GRAY_3C : GRAY_20)
                    .push(event);

            Rectangle
                    .create(xOffset, yOffset, moduleWidth, 0.5)
                    .color(GRAY_3C)
                    .push(event);

            Rectangle
                    .create(xOffset, yOffset + moduleHeight, moduleWidth, 0.5)
                    .color(GRAY_3C)
                    .push(event);

            Rectangle
                    .create(xOffset, yOffset, 0.5, moduleHeight)
                    .color(GRAY_3C)
                    .push(event);

            Rectangle
                    .create(xOffset + moduleWidth, yOffset, 0.5, moduleHeight + 0.5)
                    .color(GRAY_3C)
                    .push(event);

            CFonts.getFont("SFPT-Semibold", 12).drawString(mod.name.toUpperCase(), xOffset + 7.5f, yOffset + 5.5f, over ? Color.WHITE : TEXT_MAIN);
            ICONS_12.drawString((mod.isExpanded() ? Icons.CARET_DOWN : Icons.CARET_UP) + "", xOffset + moduleWidth - 12, yOffset + 6, over ? Color.WHITE : TEXT_MAIN);

            float appendHeight = 0;
            if (mod.isExpanded()) {
                float settX = xOffset;
                float settY = yOffset + moduleHeight;
                float settYOffset = settY + 8;

                for (Setting<?> sett : mod.settings) {
                    if (sett.isHidden()) continue;

                    settYOffset += drawSetting(sett, settX, settYOffset, moduleWidth, event);
                    settYOffset += SETTING_SPACING;
                }

                appendHeight = getModuleAppendHeight(mod);

                Rectangle
                        .create(settX, settY + appendHeight, moduleWidth, 0.5)
                        .color(GRAY_3C)
                        .push(event);

                Rectangle
                        .create(settX, settY, 0.5, appendHeight)
                        .color(GRAY_3C)
                        .push(event);

                Rectangle
                        .create(settX + moduleWidth, settY, 0.5, appendHeight + 0.5)
                        .color(GRAY_3C)
                        .push(event);
            }

            yOffset += moduleHeight + 10 + appendHeight;
        }
    }

    private float drawSetting(Setting<?> setting, float x, float y, float width, RenderScreenEvent event) {
        float height = getSettingHeight(setting);
        if (height <= 0) return 0;

        if (setting instanceof BooleanSetting) {
            drawBooleanSetting((BooleanSetting) setting, x, y, width, event);
        } else if (setting instanceof NumberSetting) {
            drawNumberSetting((NumberSetting) setting, x, y, width, event);
        } else if (setting instanceof StringSetting) {
            drawStringSetting((StringSetting) setting, x, y, width, event);
        } else if (setting instanceof MultiStringSetting) {
            drawMultiStringSetting((MultiStringSetting) setting, x, y, width, event);
        }

        return height;
    }

    private void drawBooleanSetting(BooleanSetting setting, float x, float y, float width, RenderScreenEvent event) {
        float controlX = x + SETTING_INNER_PAD;
        float boxY = y + (15 - CHECKBOX_SIZE) / 2f;

        boolean boxHover = RenderUtil.hovered(event.mouseX, event.mouseY, controlX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE);
        Color outline = (boxHover || setting.getValue()) ? GRAY_60 : GRAY_3C;

        Rectangle
                .create(controlX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE)
                .color(GRAY_20)
                .push(event);
        drawOutline(controlX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE, outline, event);

        if (setting.getValue()) {
            Rectangle
                    .create(controlX + 0.5f, boxY + 0.5f, CHECKBOX_SIZE - 1, CHECKBOX_SIZE - 1)
                    .color(GRAY_3C)
                    .push(event);

            Rectangle
                    .create(controlX + (CHECKBOX_SIZE - CHECKBOX_FILL_SIZE) / 2f, boxY + (CHECKBOX_SIZE - CHECKBOX_FILL_SIZE) / 2f, CHECKBOX_FILL_SIZE, CHECKBOX_FILL_SIZE)
                    .color(Color.WHITE)
                    .push(event);
        }

        CFonts.getFont("SFPT-Regular", 12).drawString(setting.name.toUpperCase(), controlX + CHECKBOX_SIZE + 5, boxY, TEXT_MUTED);
    }

    private void drawNumberSetting(NumberSetting setting, float x, float y, float width, RenderScreenEvent event) {
        float controlX = x + SETTING_INNER_PAD;
        float controlWidth = width - SETTING_INNER_PAD * 2;
        float controlY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;

        double min = setting.min.doubleValue();
        double max = setting.max.doubleValue();
        double value = setting.getValue().doubleValue();
        double pct = (value - min) / (max - min);

        float trackX = controlX;
        float trackWidth = controlWidth - SLIDER_VALUE_WIDTH;
        float trackY = controlY + (SLIDER_CONTROL_HEIGHT - SLIDER_TRACK_HEIGHT) / 2f;
        float thumbX = trackX + (float) (pct * trackWidth) - SLIDER_THUMB_WIDTH / 2f;
        float thumbY = controlY + (SLIDER_CONTROL_HEIGHT - SLIDER_THUMB_HEIGHT) / 2f;

        boolean trackHover = RenderUtil.hovered(event.mouseX, event.mouseY, trackX, controlY, trackWidth, SLIDER_CONTROL_HEIGHT);
        boolean thumbHover = RenderUtil.hovered(event.mouseX, event.mouseY, thumbX, controlY, SLIDER_THUMB_WIDTH, SLIDER_CONTROL_HEIGHT);
        Color thumbColor = (trackHover || thumbHover) ? Color.WHITE : GRAY_60;

        Rectangle
                .create(trackX, trackY, trackWidth, SLIDER_TRACK_HEIGHT)
                .color(GRAY_20)
                .push(event);
        drawOutline(trackX, trackY, trackWidth, SLIDER_TRACK_HEIGHT, GRAY_3C, event);

        Rectangle
                .create(thumbX, thumbY, SLIDER_THUMB_WIDTH, SLIDER_THUMB_HEIGHT)
                .color(thumbColor)
                .push(event);

        CFonts.getFont("SFPT-Regular", 12).drawString(setting.name.toUpperCase(), controlX, y + 1, TEXT_MUTED);
        CFonts.getFont("SFPT-Regular", 12).drawString(formatNumber(setting), trackX + trackWidth + 4, controlY + 2, TEXT_MAIN);
    }

    private void drawStringSetting(StringSetting setting, float x, float y, float width, RenderScreenEvent event) {
        float controlX = x + SETTING_INNER_PAD;
        float controlWidth = width - SETTING_INNER_PAD * 2;
        float headerY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;

        boolean headerHover = RenderUtil.hovered(event.mouseX, event.mouseY, controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT);
        Color headerOutline = headerHover ? GRAY_60 : GRAY_3C;

        Rectangle
                .create(controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT)
                .color(GRAY_20)
                .push(event);
        drawOutline(controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT, headerOutline, event);

        CFonts.getFont("SFPT-Regular", 12).drawString(setting.name.toUpperCase(), controlX, y + 1, TEXT_MUTED);
        CFonts.getFont("SFPT-Regular", 12).drawString(setting.getValue(), controlX + 4, headerY + 3, TEXT_MAIN);
        ICONS_12.drawString(Icons.CARET_DOWN + "", controlX + controlWidth - 10, headerY + 3, TEXT_MAIN);

        if (setting.expanded) {
            float listY = headerY + DROPDOWN_HEADER_HEIGHT;
            float listHeight = setting.allValues.length * DROPDOWN_ITEM_HEIGHT;

            Rectangle
                    .create(controlX, listY, controlWidth, listHeight)
                    .color(GRAY_20)
                    .push(event);
            drawOutline(controlX, listY, controlWidth, listHeight, GRAY_3C, event);

            for (int i = 0; i < setting.allValues.length; i++) {
                float itemY = listY + i * DROPDOWN_ITEM_HEIGHT;
                String value = setting.allValues[i];
                boolean selected = value.equals(setting.getValue());
                boolean itemHover = RenderUtil.hovered(event.mouseX, event.mouseY, controlX, itemY, controlWidth, DROPDOWN_ITEM_HEIGHT);

                if (itemHover) {
                    Rectangle
                            .create(controlX + 0.5f, itemY + 0.5f, controlWidth - 1, DROPDOWN_ITEM_HEIGHT - 1)
                            .color(GRAY_3C)
                            .push(event);
                } else if (selected) {
                    Rectangle
                            .create(controlX + 0.5f, itemY + 0.5f, controlWidth - 1, DROPDOWN_ITEM_HEIGHT - 1)
                            .color(GRAY_30)
                            .push(event);
                }

                if (selected) {
                    Rectangle
                            .create(controlX, itemY, 1, DROPDOWN_ITEM_HEIGHT)
                            .color(GRAY_60)
                            .push(event);
                }

                CFonts.getFont("SFPT-Regular", 12).drawString(value, controlX + 4, itemY + 3, selected || itemHover ? Color.WHITE : TEXT_MAIN);
            }
        }
    }

    private void drawMultiStringSetting(MultiStringSetting setting, float x, float y, float width, RenderScreenEvent event) {
        float controlX = x + SETTING_INNER_PAD;
        float controlWidth = width - SETTING_INNER_PAD * 2;
        float headerY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;

        boolean headerHover = RenderUtil.hovered(event.mouseX, event.mouseY, controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT);
        Color headerOutline = headerHover ? GRAY_60 : GRAY_3C;

        Rectangle
                .create(controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT)
                .color(GRAY_20)
                .push(event);
        drawOutline(controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT, headerOutline, event);

        int selectedCount = setting.getValue().length;
        String headerText = selectedCount == 0 ? "None Selected" : selectedCount + " Selected";

        CFonts.getFont("SFPT-Regular", 12).drawString(setting.name.toUpperCase(), controlX, y + 1, TEXT_MUTED);
        CFonts.getFont("SFPT-Regular", 12).drawString(headerText, controlX + 4, headerY + 3, TEXT_MAIN);
        ICONS_12.drawString(Icons.CARET_DOWN + "", controlX + controlWidth - 10, headerY + 3, TEXT_MAIN);

        if (setting.expanded) {
            float listY = headerY + DROPDOWN_HEADER_HEIGHT;
            float listHeight = setting.allValues.length * DROPDOWN_ITEM_HEIGHT;

            Rectangle
                    .create(controlX, listY, controlWidth, listHeight)
                    .color(GRAY_20)
                    .push(event);
            drawOutline(controlX, listY, controlWidth, listHeight, GRAY_3C, event);

            for (int i = 0; i < setting.allValues.length; i++) {
                float itemY = listY + i * DROPDOWN_ITEM_HEIGHT;
                String value = setting.allValues[i];
                boolean selected = isMultiSelected(setting, value);
                boolean itemHover = RenderUtil.hovered(event.mouseX, event.mouseY, controlX, itemY, controlWidth, DROPDOWN_ITEM_HEIGHT);

                if (itemHover) {
                    Rectangle
                            .create(controlX + 0.5f, itemY + 0.5f, controlWidth - 1, DROPDOWN_ITEM_HEIGHT - 1)
                            .color(GRAY_3C)
                            .push(event);
                } else if (selected) {
                    Rectangle
                            .create(controlX + 0.5f, itemY + 0.5f, controlWidth - 1, DROPDOWN_ITEM_HEIGHT - 1)
                            .color(GRAY_30)
                            .push(event);
                }

                if (selected) {
                    Rectangle
                            .create(controlX, itemY, 1, DROPDOWN_ITEM_HEIGHT)
                            .color(GRAY_60)
                            .push(event);
                }

                float checkX = controlX + 4;
                float checkY = itemY + (DROPDOWN_ITEM_HEIGHT - MULTI_CHECK_SIZE) / 2f;

                Rectangle
                        .create(checkX, checkY, MULTI_CHECK_SIZE, MULTI_CHECK_SIZE)
                        .color(GRAY_20)
                        .push(event);
                drawOutline(checkX, checkY, MULTI_CHECK_SIZE, MULTI_CHECK_SIZE, GRAY_3C, event);

                if (selected) {
                    Rectangle
                            .create(checkX + 0.5f, checkY + 0.5f, MULTI_CHECK_SIZE - 1, MULTI_CHECK_SIZE - 1)
                            .color(GRAY_60)
                            .push(event);
                }

                CFonts.getFont("SFPT-Regular", 12).drawString(value, checkX + MULTI_CHECK_SIZE + 4, itemY + 3, selected || itemHover ? Color.WHITE : TEXT_MAIN);
            }
        }
    }

    private float getSettingHeight(Setting<?> setting) {
        if (setting.isHidden()) return 0;

        if (setting instanceof BooleanSetting) {
            return 15;
        } else if (setting instanceof NumberSetting) {
            return SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP + SLIDER_CONTROL_HEIGHT;
        } else if (setting instanceof StringSetting) {
            StringSetting s = (StringSetting) setting;
            float h = SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP + DROPDOWN_HEADER_HEIGHT;
            if (s.expanded) h += s.allValues.length * DROPDOWN_ITEM_HEIGHT;
            return h;
        } else if (setting instanceof MultiStringSetting) {
            MultiStringSetting s = (MultiStringSetting) setting;
            float h = SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP + DROPDOWN_HEADER_HEIGHT;
            if (s.expanded) h += s.allValues.length * DROPDOWN_ITEM_HEIGHT;
            return h;
        }

        return 0;
    }

    private float getModuleAppendHeight(Module mod) {
        if (!mod.isExpanded()) return 0;

        float height = 16;
        int count = 0;

        for (Setting<?> sett : mod.settings) {
            if (sett.isHidden()) continue;
            height += getSettingHeight(sett);
            count++;
        }

        if (count > 0) height += (count - 1) * SETTING_SPACING;
        return height;
    }

    private float getModuleListMinScroll() {
        float contentHeight = 0;
        for (Module mod : Vanta.instance.moduleStorage.getModulesByCategory(selectedCat)) {
            contentHeight += 18 + 10 + getModuleAppendHeight(mod);
        }

        float viewHeight = sHeight - 9.5f;
        float hidden = contentHeight - viewHeight;
        return hidden > 0 ? -hidden : 0;
    }

    private void clickCategory(int mouseX, int mouseY, int mouseButton) {
        float yOffset = y + 7.5f + 7.5f + 7.5f;
        for (Category cat : Category.values()) {
            if (RenderUtil.hovered(mouseX, mouseY, x, yOffset, SIDEBAR_WIDTH, 20) && mouseButton == 0) {
                closeAllDropdowns();
                selectedCat = cat;
                moduleScroll = 0;
            }
            yOffset += 20;
        }
    }

    private void clickModules(int mouseX, int mouseY, int mouseButton) {
        float xOffset = x + SIDEBAR_WIDTH + 10;
        float yOffset = y + 9.5f + moduleScroll;
        float moduleWidth = sWidth - SIDEBAR_WIDTH - 10 * 2;
        boolean hitDropdown = false;

        for (Module mod : Vanta.instance.moduleStorage.getModulesByCategory(selectedCat)) {
            float moduleHeight = 18;
            boolean headerHover = RenderUtil.hovered(mouseX, mouseY, xOffset, yOffset, moduleWidth, moduleHeight);

            if (headerHover && mouseButton == 0) {
                mod.setEnabled(!mod.isEnabled());
            } else if (headerHover && mouseButton == 1) {
                mod.setExpanded(!mod.isExpanded());
                closeAllDropdowns();
            }

            if (mod.isExpanded()) {
                float settX = xOffset;
                float settYOffset = yOffset + moduleHeight + 8;

                for (Setting<?> sett : mod.settings) {
                    if (sett.isHidden()) continue;

                    if (clickSetting(sett, settX, settYOffset, moduleWidth, mouseX, mouseY, mouseButton)) {
                        if (sett instanceof StringSetting || sett instanceof MultiStringSetting) {
                            hitDropdown = true;
                        }
                    }

                    settYOffset += getSettingHeight(sett) + SETTING_SPACING;
                }
            }

            yOffset += moduleHeight + 10 + getModuleAppendHeight(mod);
        }

        if (!hitDropdown && mouseButton == 0) {
            closeAllDropdowns();
        }
    }

    private boolean clickSetting(Setting<?> setting, float x, float y, float width, float mouseX, float mouseY, int mouseButton) {
        float height = getSettingHeight(setting);
        if (height <= 0) return false;
        if (!RenderUtil.hovered(mouseX, mouseY, x, y, width, height)) return false;

        float controlX = x + SETTING_INNER_PAD;
        float controlWidth = width - SETTING_INNER_PAD * 2;

        if (setting instanceof BooleanSetting && mouseButton == 0) {
            BooleanSetting s = (BooleanSetting) setting;
            s.setValue(!s.getValue());
            return true;
        }

        if (setting instanceof NumberSetting && mouseButton == 0) {
            NumberSetting s = (NumberSetting) setting;
            float controlY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;
            float trackX = controlX;
            float trackWidth = controlWidth - SLIDER_VALUE_WIDTH;

            if (RenderUtil.hovered(mouseX, mouseY, trackX, controlY, trackWidth, SLIDER_CONTROL_HEIGHT)) {
                draggingSlider = s;
                draggingTrackX = trackX;
                draggingTrackWidth = trackWidth;
                updateSliderValue(s, mouseX, trackX, trackWidth);
            }
            return true;
        }

        if (setting instanceof StringSetting) {
            StringSetting s = (StringSetting) setting;
            float headerY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;
            float listY = headerY + DROPDOWN_HEADER_HEIGHT;

            if (s.expanded && mouseButton == 0) {
                for (int i = 0; i < s.allValues.length; i++) {
                    if (RenderUtil.hovered(mouseX, mouseY, controlX, listY + i * DROPDOWN_ITEM_HEIGHT, controlWidth, DROPDOWN_ITEM_HEIGHT)) {
                        s.setValue(s.allValues[i]);
                        s.expanded = false;
                        return true;
                    }
                }
            }

            if (RenderUtil.hovered(mouseX, mouseY, controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT) && mouseButton == 0) {
                closeAllDropdownsExcept(s);
                s.expanded = !s.expanded;
                return true;
            }
            return true;
        }

        if (setting instanceof MultiStringSetting) {
            MultiStringSetting s = (MultiStringSetting) setting;
            float headerY = y + SETTING_LABEL_HEIGHT + SETTING_LABEL_GAP;
            float listY = headerY + DROPDOWN_HEADER_HEIGHT;

            if (s.expanded && mouseButton == 0) {
                for (int i = 0; i < s.allValues.length; i++) {
                    if (RenderUtil.hovered(mouseX, mouseY, controlX, listY + i * DROPDOWN_ITEM_HEIGHT, controlWidth, DROPDOWN_ITEM_HEIGHT)) {
                        toggleMultiValue(s, s.allValues[i]);
                        return true;
                    }
                }
            }

            if (RenderUtil.hovered(mouseX, mouseY, controlX, headerY, controlWidth, DROPDOWN_HEADER_HEIGHT) && mouseButton == 0) {
                closeAllDropdownsExcept(s);
                s.expanded = !s.expanded;
                return true;
            }
            return true;
        }

        return false;
    }

    private void drawOutline(float x, float y, float width, float height, Color color, RenderScreenEvent event) {
        Rectangle.create(x, y, width, 0.5f).color(color).push(event);
        Rectangle.create(x, y + height - 0.5f, width, 0.5f).color(color).push(event);
        Rectangle.create(x, y + 0.5f, 0.5f, height - 1).color(color).push(event);
        Rectangle.create(x + width - 0.5f, y + 0.5f, 0.5f, height - 1).color(color).push(event);
    }

    private void updateSliderValue(NumberSetting setting, float mouseX, float trackX, float trackWidth) {
        double min = setting.min.doubleValue();
        double max = setting.max.doubleValue();
        double pct = MathHelper.clamp_double((mouseX - trackX) / trackWidth, 0, 1);
        double raw = min + pct * (max - min);

        double value;
        if (setting.places <= 0) {
            value = Math.round(raw);
        } else {
            double factor = Math.pow(10, setting.places);
            value = Math.round(raw * factor) / factor;
        }

        value = MathHelper.clamp_double(value, min, max);
        setting.setValue(value);
    }

    private String formatNumber(NumberSetting setting) {
        double value = setting.getValue().doubleValue();
        if (setting.places <= 0) {
            return (int) Math.round(value) + setting.suffix;
        }
        return String.format(Locale.US, "%." + setting.places + "f%s", value, setting.suffix);
    }

    private boolean isMultiSelected(MultiStringSetting setting, String value) {
        for (String s : setting.getValue()) {
            if (s.equals(value)) return true;
        }
        return false;
    }

    private void toggleMultiValue(MultiStringSetting setting, String value) {
        List<String> selected = new ArrayList<>();
        for (String val : setting.allValues) {
            if (isMultiSelected(setting, val)) selected.add(val);
        }

        if (selected.contains(value)) {
            selected.remove(value);
        } else {
            selected.add(value);
        }

        setting.setValue(selected.toArray(new String[0]));
    }

    private void closeAllDropdowns() {
        for (Module mod : Vanta.instance.moduleStorage.list) {
            for (Setting<?> sett : mod.settings) {
                if (sett instanceof StringSetting) {
                    ((StringSetting) sett).expanded = false;
                } else if (sett instanceof MultiStringSetting) {
                    ((MultiStringSetting) sett).expanded = false;
                }
            }
        }
    }

    private void closeAllDropdownsExcept(Setting<?> current) {
        for (Module mod : Vanta.instance.moduleStorage.list) {
            for (Setting<?> sett : mod.settings) {
                if (sett == current) continue;

                if (sett instanceof StringSetting) {
                    ((StringSetting) sett).expanded = false;
                } else if (sett instanceof MultiStringSetting) {
                    ((MultiStringSetting) sett).expanded = false;
                }
            }
        }
    }
}
