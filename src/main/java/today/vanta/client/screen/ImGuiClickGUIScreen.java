package today.vanta.client.screen;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import org.lwjgl.input.Keyboard;
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
import today.vanta.util.client.Strings;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ImageUtil;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.lwjgl.imgui.ImGuiImpl;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ImGuiClickGUIScreen extends VantaScreen implements Strings {
    private Category currentCategory = Category.COMBAT;
    private final Map<Category, Module> lastModulePerCategory = new EnumMap<>(Category.class);
    private Module currentModule;
    private Module listeningModule = null;

    @EventListen
    private void onRender(RenderScreenEvent event) {
        if (Vanta.instance.moduleStorage.getT(ClickGUI.class).image.getValue()) {
            float imgWidth = 300;
            float imgHeight = 300;

            String texture = Vanta.instance.moduleStorage.getT(ClickGUI.class).mascot.getValue() + ".png";
            if (texture.startsWith("longboy")) {
                imgHeight = 400 ;
            }

            ImageRectangle
                    .create(width - 175 - (imgWidth / 2), height - 210 - (imgHeight / 2), imgWidth, imgHeight, -1)
                    .resource(ImageUtil.getTexture(texture))
                    .push(event);
        }

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

        ImGuiImpl.draw(() -> {
            ImGui.setNextWindowSize(600, 425, ImGuiCond.Once);
            if (ImGui.begin(CLIENT_NAME)) {
                if (ImGui.beginChild("categories", 150, 0, true)) {
                    float fullWidth = ImGui.getContentRegionAvailX();

                    for (Category category : Category.values()) {
                        if (ImGui.button(category.name, fullWidth, 20)) {
                            if (currentModule != null) {
                                lastModulePerCategory.put(currentCategory, currentModule);
                            }

                            currentCategory = category;

                            currentModule = lastModulePerCategory.getOrDefault(category, null);
                        }
                    }

                    ImGui.endChild();
                }

                ImGui.sameLine();

                if (ImGui.beginChild("modules", 150, 0, true)) {
                    float fullWidth = ImGui.getContentRegionAvailX();
                    for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(currentCategory)) {
                        if (ImGui.button(module.name, fullWidth, 20)) {
                            currentModule = module;
                            lastModulePerCategory.put(currentCategory, module);
                        }
                    }
                    ImGui.endChild();
                }

                ImGui.sameLine();

                if (currentModule != null) {
                    if (ImGui.beginChild("settings", 0, 0, true)) {
                        float fullWidth = ImGui.getContentRegionAvailX();

                        ImGui.text(currentModule.displayName);

                        float checkboxWidth = ImGui.getFrameHeight();
                        float rightEdge = ImGui.getContentRegionAvailX();

                        ImGui.sameLine(ImGui.getCursorPosX() + rightEdge - checkboxWidth);
                        if (ImGui.checkbox("##Toggle", currentModule.isEnabled())) {
                            currentModule.setEnabled(!currentModule.isEnabled());
                        }
                        ImGui.separator();

                        if (!currentModule.frozen) {
                            String keyName = Keyboard.getKeyName(currentModule.key);
                            if (listeningModule != null && listeningModule.equals(currentModule)) {
                                keyName = "...";
                            }
                            if (ImGui.button("Keybind: " + keyName, fullWidth, 20)) {
                                listeningModule = currentModule;
                            }
                        }

                        if (!currentModule.settings.isEmpty())
                            ImGui.separator();

                        for (Setting<?> setting : currentModule.settings) {
                            if (setting.isHidden()) continue;
                            if (setting instanceof BooleanSetting) {
                                BooleanSetting boolSet = (BooleanSetting) setting;
                                String label = boolSet.name + ": " + (boolSet.getValue() ? "enabled" : "disabled");

                                if (ImGui.button(label, fullWidth, 20)) {
                                    boolSet.setValue(!boolSet.getValue());
                                }
                            } else if (setting instanceof NumberSetting) {
                                NumberSetting numSet = (NumberSetting) setting;
                                ImGui.text(numSet.name);
                                ImGui.sameLine();
                                ImGui.pushItemWidth(ImGui.getContentRegionAvailX());

                                String suffix = numSet.suffix;
                                if (suffix != null && suffix.contains("%")) {
                                    suffix = suffix.replace("%", "%%");
                                }

                                boolean isInt = numSet.getValue() instanceof Integer;

                                String format;
                                if (isInt) {
                                    format = "%d";
                                } else {
                                    format = "%." + numSet.places + "f";
                                }

                                if (suffix != null && !suffix.isEmpty()) {
                                    format += suffix;
                                }

                                if (isInt) {
                                    int[] val = {numSet.getValue().intValue()};

                                    if (ImGui.sliderInt(
                                            "##" + numSet.name,
                                            val,
                                            numSet.min.intValue(),
                                            numSet.max.intValue(),
                                            format)) {
                                        numSet.setValue(val[0]);
                                    }

                                } else {
                                    float[] val = {numSet.getValue().floatValue()};

                                    if (ImGui.sliderFloat(
                                            "##" + numSet.name,
                                            val,
                                            numSet.min.floatValue(),
                                            numSet.max.floatValue(),
                                            format)) {
                                        numSet.setValue(val[0]);
                                    }
                                }

                                ImGui.popItemWidth();
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting multiSet = (MultiStringSetting) setting;

                                String label = multiSet.name + " (" + multiSet.getValue().length + " enabled)";

                                ImGui.pushItemWidth(fullWidth);

                                if (ImGui.beginCombo("##22" + label, label)) {
                                    for (String val : multiSet.allValues) {
                                        boolean isSelected = multiSet.isEnabled(val);
                                        if (ImGui.selectable(val, isSelected)) {
                                            List<String> list = new ArrayList<>(Arrays.asList(multiSet.getValue()));
                                            if (isSelected) {
                                                list.remove(val);
                                            } else {
                                                list.add(val);
                                            }
                                            multiSet.setValue(list.toArray(new String[0]));
                                        }
                                        if (isSelected) {
                                            ImGui.setItemDefaultFocus();
                                        }
                                    }
                                    ImGui.endCombo();
                                }

                                ImGui.popItemWidth();
                            } else if (setting instanceof StringSetting) {
                                StringSetting strSet = (StringSetting) setting;

                                ImGui.text(strSet.name);
                                ImGui.sameLine();
                                float avail = ImGui.getContentRegionAvailX();

                                ImGui.pushItemWidth(avail);

                                String[] items = strSet.allValues;

                                int currentIndex = strSet.index();
                                int[] selected = {currentIndex};
                                if (ImGui.beginCombo(strSet.name, items[currentIndex])) {
                                    for (int i = 0; i < items.length; i++) {
                                        boolean isSelected = (i == selected[0]);
                                        if (ImGui.selectable(items[i], isSelected)) {
                                            selected[0] = i;
                                            strSet.setValue(items[i]);
                                        }
                                        if (isSelected) {
                                            ImGui.setItemDefaultFocus();
                                        }
                                    }
                                    ImGui.endCombo();
                                }

                                ImGui.popItemWidth();
                            }
                        }

                        ImGui.endChild();
                    }
                }
            }
            ImGui.end();
        });
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
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
}