package today.vanta.client.screen;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
import today.vanta.Vanta;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClickGUI;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.client.screen.ScreenSavingUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.system.math.MathUtil;
import today.vanta.util.system.VantaFile;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGUIScreen extends GuiScreen {

    private final CFontRenderer medium = CFonts.getFont("SFPT-Semibold", 20);
    private final CFontRenderer regular = CFonts.getFont("SFPT-Medium", 18);
    private final CFontRenderer sett = CFonts.getFont("SFPT-Medium", 16);

    private final float panelWidth = 120;

    private boolean dragging = false;
    private float dragOffsetX = 0, dragOffsetY = 0;
    private Category draggedCategory = null;
    private Module listeningModule = null;

    public ClickGUIScreen() {
        if (!ScreenSavingUtil.loadConfig(VantaFile.getFile("clickgui.json"))) {
            float xOffset = 5;
            for (Category category : Category.values()) {
                category.position.set(xOffset, 5);
                xOffset += panelWidth + 5;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (Vanta.instance.moduleStorage.getT(ClickGUI.class).darkenBackground.getValue()) {
            RenderUtil.rectangle(0, 0, width, height, new Color(0,0,0,150));
        }

        Color color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        float panelHeight = 16;

        for (Category category : Category.values()) {
            Vector2f position = category.position;
            boolean hoverCat = RenderUtil.hovered(mouseX, mouseY, position.x, position.y, panelWidth, panelHeight);

            position = drag(position, mouseX, mouseY, category, hoverCat);

            RenderUtil.rectangle(position.x, position.y, panelWidth, panelHeight, new Color(30, 30, 30));
            medium.drawString(category.name, position.x + 3, position.y + 1.5f, Color.WHITE);

            float ignoreThis = 0;
            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                if (module.hideFromClickGui) {
                    continue;
                }

                ignoreThis += 14;
                if (module.isExpanded()) {
                    if (module.displayNames.length > 1 && !module.hideFromArraylist)
                        ignoreThis += 14;
                    if (!module.frozen)
                        ignoreThis += 14;
                    if (!module.frozen && !module.category.equals(Category.CLIENT))
                        ignoreThis += 14;
                    ignoreThis += 14;
                    if (module.getSuffix() != null && !module.hideFromArraylist)
                        ignoreThis += 14;

                    if (!module.settings.isEmpty()) {
                        for (Setting<?> setting : module.settings) {
                            if (setting.isHidden()) {
                                continue;
                            }

                            if (setting instanceof BooleanSetting) {
                                ignoreThis += 14;
                            } else if (setting instanceof NumberSetting) {
                                ignoreThis += 20;
                            } else if (setting instanceof StringSetting) {
                                ignoreThis += 14;
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting setting1 = (MultiStringSetting) setting;
                                if (setting1.expanded) {
                                    ignoreThis += setting1.allValues.length * 14;
                                } else {
                                    ignoreThis += 14;
                                }
                            }
                        }
                    }
                }
            }

            RenderUtil.rectangle(position.x, position.y + 14, panelWidth, ignoreThis + 2, new Color(30, 30, 30));

            float y = position.y + 14;
            float x = position.x;

            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                if (module.hideFromClickGui) {
                    continue;
                }

                boolean hoverMod = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 1, panelWidth - 3, 14);

                RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverMod ? new Color(40, 40, 40) : new Color(35, 35, 35));

                regular.drawString(module.name, x + 5, y + 2, module.isEnabled() ? color1 : Color.WHITE);
                regular.drawString(module.isExpanded() ? "-" : "+", x + panelWidth - regular.getStringWidth(module.isExpanded() ? "-" : "+") - 7, y + 1.5f, hoverMod ? Color.LIGHT_GRAY : Color.WHITE);

                y += 14;

                if (module.isExpanded()) {
                    if (module.displayNames.length > 1 && !module.hideFromArraylist) {
                        boolean hoverDisplayName = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14);
                        RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverDisplayName ? new Color(40, 40, 40) : new Color(35, 35, 35));
                        sett.drawString("Display name", x + 5, y + 2.5f, -1);

                        float bX = x + panelWidth - 5;
                        RenderUtil.rectangle(bX - sett.getStringWidth(module.displayName) - 2, y + 2.5, sett.getStringWidth(module.displayName) + 4, 9, new Color(45, 45, 45));
                        sett.drawString(module.displayName, bX - sett.getStringWidth(module.displayName), y + 2, -1);

                        y += 14;
                    }

                    if (!module.frozen) {
                        boolean hoverKeybind = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14);
                        RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverKeybind ? new Color(40, 40, 40) : new Color(35, 35, 35));
                        sett.drawString("Keybind", x + 5, y + 2.5f, -1);

                        float bXKey = x + panelWidth - 5;
                        String keyName = Keyboard.getKeyName(module.key);
                        if (listeningModule != null && listeningModule.equals(module)) {
                            keyName = "...";
                        }
                        RenderUtil.rectangle(bXKey - sett.getStringWidth(keyName) - 2, y + 2.5, sett.getStringWidth(keyName) + 4, 9, new Color(45, 45, 45));
                        sett.drawString(keyName, bXKey - sett.getStringWidth(keyName), y + 2, -1);

                        y += 14;
                    }

                    if (!module.frozen && !module.category.equals(Category.CLIENT)) {
                        boolean hoverHide = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14);
                        RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverHide ? new Color(40, 40, 40) : new Color(35, 35, 35));
                        sett.drawString("Hide on arraylist", x + 5, y + 2.5f, -1);

                        boolean hidden = module.hideFromArraylist;
                        float bXHidden = x + panelWidth - 5;
                        RenderUtil.rectangle(bXHidden - 17, y + 3.5f, 17, 7, hidden ? color1.brighter() : new Color(0xA3A3A3));
                        RenderUtil.rectangle(hidden ? bXHidden - 8 : bXHidden - 17 - 1, y + 2.5f, 9, 9, hidden ? color1 : new Color(0xFFFFFF));

                        y += 14;
                    }

                    boolean hoverSave = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14);
                    RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverSave ? new Color(40, 40, 40) : new Color(35, 35, 35));
                    sett.drawString("Save in config", x + 5, y + 2.5f, -1);

                    boolean save = module.addToConfig;
                    float bXSave = x + panelWidth - 5;
                    RenderUtil.rectangle(bXSave - 17, y + 3.5f, 17, 7, save ? color1.brighter() : new Color(0xA3A3A3));
                    RenderUtil.rectangle(save ? bXSave - 8 : bXSave - 17 - 1, y + 2.5f, 9, 9, save ? color1 : new Color(0xFFFFFF));

                    y += 14;

                    if (module.getSuffix() != null && !module.hideFromArraylist) {
                        boolean hoverSuffix = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14);
                        RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hoverSuffix ? new Color(40, 40, 40) : new Color(35, 35, 35));
                        sett.drawString("Show suffix", x + 5, y + 2.5f, -1);

                        boolean suffix = module.addSuffix;
                        float bXSuffix = x + panelWidth - 5;
                        RenderUtil.rectangle(bXSuffix - 17, y + 3.5f, 17, 7, suffix ? color1.brighter() : new Color(0xA3A3A3));
                        RenderUtil.rectangle(suffix ? bXSuffix - 8 : bXSuffix - 17 - 1, y + 2.5f, 9, 9, suffix ? color1 : new Color(0xFFFFFF));

                        y += 14;
                    }

                    if (!module.settings.isEmpty() || Vanta.instance.moduleStorage.getModulesByCategory(category).size() != 1)
                        RenderUtil.rectangle(x + 1.5, y - 1, panelWidth - 3, 1, new Color(45, 45, 45));

                    if (!module.settings.isEmpty()) {
                        for (Setting<?> setting : module.settings) {
                            if (setting.isHidden()) {
                                continue;
                            }

                            boolean hover = RenderUtil.hovered(mouseX, mouseY, position.x + 1.5f, y, panelWidth - 3, 14);

                            if (setting instanceof BooleanSetting) {
                                BooleanSetting toggle = (BooleanSetting) setting;
                                RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));

                                float bX = x + panelWidth - 5;
                                RenderUtil.rectangle(bX - 17, y + 3.5, 17, 7, toggle.getValue() ? color1.brighter() : new Color(0xA3A3A3));
                                RenderUtil.rectangle(toggle.getValue() ? bX - 8 : bX - 17 - 1, y + 2.5, 9, 9, toggle.getValue() ? color1 : new Color(0xFFFFFF));

                                sett.drawString(setting.name, x + 5, y + 2.5f, -1);
                                y += 14;
                            } else if (setting instanceof NumberSetting) {
                                NumberSetting slider = (NumberSetting) setting;

                                float value = slider.getValue().floatValue();
                                float min = slider.min.floatValue();
                                float max = slider.max.floatValue();
                                float width = Math.min(Math.max((value - min) / (max - min), 0), 1) * 111;

                                RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 20, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));

                                RenderUtil.rectangle(x + 5, y + 14, 111, 3, color1.darker());
                                RenderUtil.rectangle(x + 5, y + 14, width, 3, color1);

                                float handleX = x + 5 + width - 2;
                                if (width >= 111) {
                                    handleX = x + 5 + 108 - 2;
                                } else if (width <= 5) {
                                    handleX = x + 5;
                                }
                                RenderUtil.rectangle(handleX, y + 14 - 1, 5, 5, new Color(0xFFFFFF));

                                sett.drawString(setting.name, x + 5, y + 2.5f, -1);

                                String formattedValue = String.valueOf(value);
                                sett.drawString(formattedValue, x + panelWidth - 5 - sett.getStringWidth(formattedValue), y + 1.5f, -1);

                                if (RenderUtil.hovered(mouseX, mouseY, x + 5, y, 111, 18) && Mouse.isButtonDown(0)) {
                                    double normalizedX = (mouseX - (x + 5)) / 111.0;
                                    double newValue = min + normalizedX * (max - min);
                                    newValue = MathUtil.round(newValue, slider.places);
                                    newValue = Math.min(Math.max(newValue, min), max);
                                    slider.setValue(newValue);
                                }

                                y += 20;
                            } else if (setting instanceof StringSetting) {
                                StringSetting button = (StringSetting) setting;
                                RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14, hover ? new Color(40, 40, 40) : new Color(35, 35, 35));
                                sett.drawString(setting.name, x + 5, y + 2.5f, -1);

                                float bX = x + panelWidth - 5;
                                RenderUtil.rectangle(bX - sett.getStringWidth(button.getValue()) - 2, y + 2.5, sett.getStringWidth(button.getValue()) + 4, 9, new Color(45, 45, 45));
                                sett.drawString(button.getValue(), bX - sett.getStringWidth(button.getValue()), y + 2.5f, -1);

                                y += 14;
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting selector = (MultiStringSetting) setting;
                                boolean hover2 = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, panelWidth - 3, 14 + (selector.expanded ? selector.allValues.length * 14 : 0));
                                RenderUtil.rectangle(x + 1.5f, y, panelWidth - 3, 14 + (selector.expanded ? selector.allValues.length * 14 : 0), hover2 ? new Color(40, 40, 40) : new Color(35, 35, 35));
                                sett.drawString(setting.name, x + 5, y + 2, -1);
                                float bX = x + panelWidth - 14;
                                String enabled = selector.getValue().length + " Enabled";

                                RenderUtil.rectangle(bX - sett.getStringWidth(enabled) - 2, y + 2.5, sett.getStringWidth(enabled) + 4, 9 + (selector.expanded ? selector.allValues.length * 9 : 0), new Color(45, 45, 45));
                                sett.drawString(enabled, bX - sett.getStringWidth(enabled), y + 2.5f, -1);

                                sett.drawString(selector.expanded ? "-" : "+", bX + 4.5f, y + 2.5f, -1);

                                if (selector.expanded) {
                                    float yOffset = y + 9;
                                    for (String mode : selector.allValues) {
                                        boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - sett.getStringWidth(mode), yOffset + 2, sett.getStringWidth(enabled) + 1, 9);
                                        boolean enabledMode = selector.isEnabled(mode);
                                        sett.drawString(mode, bX - sett.getStringWidth(mode), yOffset + 2.5f, hoverMode ? enabledMode ? color1.darker() : Color.LIGHT_GRAY : enabledMode ? color1 : Color.WHITE);
                                        yOffset += 9;
                                    }

                                    y += 14 * selector.allValues.length;
                                } else {
                                    y += 14;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Category category : Category.values()) {
            Vector2f position = category.position;

            float y = position.y + 14;
            float x = position.x;

            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                if (module.hideFromClickGui) {
                    continue;
                }

                boolean hoverMod = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 1, panelWidth - 3, 14);

                if (hoverMod && mouseButton == 0) {
                    module.setEnabled(!module.isEnabled());
                } else if (hoverMod && mouseButton == 1) {
                    module.setExpanded(!module.isExpanded());
                }

                y += 14;

                if (module.isExpanded()) {
                    if (module.displayNames.length > 1 && !module.hideFromArraylist) {
                        float bXDisplayName = x + panelWidth - 5;
                        if (RenderUtil.hovered(mouseX, mouseY, bXDisplayName - sett.getStringWidth(module.displayName) - 2, y + 2.5f, sett.getStringWidth(module.displayName) + 4, 9)) {
                            switch (mouseButton) {
                                case 0:
                                    module.next();
                                    break;
                                case 1:
                                    module.previous();
                                    break;
                            }
                        }

                        y += 14;
                    }

                    if (!module.frozen) {
                        if (RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 2.5f, panelWidth - 3, 9)) {
                            if (listeningModule != null && listeningModule.equals(module))
                                listeningModule = null;
                            else
                                listeningModule = module;
                        }

                        y += 14;
                    }

                    if (!module.frozen && !module.category.equals(Category.CLIENT)) {
                        float bXHidden = x + panelWidth - 5;
                        boolean hoverHide = RenderUtil.hovered(mouseX, mouseY, bXHidden - 17, y + 3.5f, 17, 7);
                        if (hoverHide && mouseButton == 0) {
                            module.hideFromArraylist = !module.hideFromArraylist;
                        }

                        y += 14;
                    }

                    float bXSave = x + panelWidth - 5;
                    boolean hoverSave = RenderUtil.hovered(mouseX, mouseY, bXSave - 17, y + 3.5f, 17, 7);
                    if (hoverSave && mouseButton == 0) {
                        module.addToConfig = !module.addToConfig;
                    }

                    y += 14;

                    if (module.getSuffix() != null && !module.hideFromArraylist) {
                        float bXSuffix = x + panelWidth - 5;
                        boolean hoverSuffix = RenderUtil.hovered(mouseX, mouseY, bXSuffix - 17, y + 3.5f, 17, 7);
                        if (hoverSuffix && mouseButton == 0) {
                            module.addSuffix = !module.addSuffix;
                        }

                        y += 14;
                    }

                    if (!module.settings.isEmpty()) {
                        for (Setting<?> setting : module.settings) {
                            if (setting.isHidden()) {
                                continue;
                            }

                            if (setting instanceof BooleanSetting) {
                                BooleanSetting toggle = (BooleanSetting) setting;

                                float bX = x + panelWidth - 5;
                                if (RenderUtil.hovered(mouseX, mouseY, bX - 17, y + 3.5f, 17, 7) && mouseButton == 0) {
                                    toggle.setValue(!toggle.getValue());
                                }

                                y += 14;
                            } else if (setting instanceof NumberSetting) {
                                y += 20;
                            } else if (setting instanceof StringSetting) {
                                StringSetting button = (StringSetting) setting;

                                float bX = x + panelWidth - 5;
                                if (RenderUtil.hovered(mouseX, mouseY, bX - sett.getStringWidth(button.getValue()) - 2, y + 2.5f, sett.getStringWidth(button.getValue()) + 4, 9)) {
                                    switch (mouseButton) {
                                        case 0:
                                            button.next();
                                            break;
                                        case 1:
                                            button.previous();
                                            break;
                                    }
                                }

                                y += 14;
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting selector = (MultiStringSetting) setting;
                                boolean hover = RenderUtil.hovered(mouseX, mouseY, position.x + 1.5f, y, panelWidth - 3, 14);

                                if (hover && (mouseButton == 0 || mouseButton == 1)) {
                                    selector.expanded = !selector.expanded;
                                }

                                float bX = x + panelWidth - 14;
                                String enabled = selector.getValue().length + " Enabled";

                                if (selector.expanded) {
                                    float yOffset = y + 9;
                                    for (String mode : selector.allValues) {
                                        boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - sett.getStringWidth(mode), yOffset + 2, sett.getStringWidth(enabled) + 1, 9);
                                        if (hoverMode && mouseButton == 0) {
                                            List<String> values = new ArrayList<>(Arrays.asList(selector.getValue()));

                                            if (selector.isEnabled(mode)) {
                                                values.remove(mode);
                                            } else {
                                                values.add(mode);
                                            }

                                            selector.setValue(values.toArray(new String[0]));
                                        }
                                        yOffset += 9;
                                    }


                                    y += 14 * selector.allValues.length;
                                } else {
                                    y += 14;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningModule != null) {
            if (keyCode == 14) {
                listeningModule.key = 0;
            } else {
                String keyName = Keyboard.getKeyName(keyCode);
                if (keyName != null && !keyName.isEmpty()) {
                    listeningModule.key = keyCode;
                }
            }
            listeningModule = null;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private Vector2f drag(Vector2f position, int mouseX, int mouseY, Category category, boolean hoverCat) {
        boolean mouse = Mouse.isButtonDown(0);
        if (hoverCat && mouse && !dragging) {
            dragging = true;
            draggedCategory = category;
            dragOffsetX = mouseX - position.x;
            dragOffsetY = mouseY - position.y;
        }

        if (dragging && draggedCategory == category) {
            position.set(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        if (!mouse) {
            dragging = false;
            draggedCategory = null;
        }

        return position;
    }

    @Override
    public void onGuiClosed() {
        ScreenSavingUtil.saveConfig(VantaFile.getFile("clickgui.json"));
        Vanta.instance.configStorage.saveConfig(VantaFile.getFile("configs/default.json"));

        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return Vanta.instance.moduleStorage.getT(ClickGUI.class).pauseGame.getValue();
    }
}