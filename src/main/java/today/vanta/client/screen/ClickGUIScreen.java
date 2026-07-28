package today.vanta.client.screen;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
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
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.ColorUtil;
import today.vanta.util.system.math.MathUtil;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGUIScreen extends VantaScreen {
    private final GlyphFontRenderer sett = CFonts.getFont("SFPT-Medium", 16);

    private final Map<Object, Float> animationMap = new HashMap<>();
    private final Map<Object, Animation> activeAnimations = new HashMap<>();

    public static final float PANEL_WIDTH = 120;

    private boolean dragging = false;
    private float dragOffsetX = 0, dragOffsetY = 0;
    private Category draggedCategory = null;
    private Module listeningModule = null;

    private boolean closing = false;
    private Color textColor = Color.WHITE;

    @Override
    protected void initScreen() {
        this.closing = false;
        this.animationMap.put("global_open", 0f);
        this.activeAnimations.values().forEach(Animation::stop);
        this.activeAnimations.clear();
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        float mouseX = event.mouseX;
        float mouseY = event.mouseY;
        float globalAnim = getAnimationValue("global_open", closing ? 0f : 1f, 300, Easing.EASE_OUT_EXPO);

        if (closing && globalAnim <= 0.01f) {
            this.mc.displayGuiScreen(null);
            return;
        }


        int overlayAlpha = mc.theWorld != null ? (int) (150 * globalAnim) : (int) (255 * globalAnim);

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
            float imgWidth = 300 * globalAnim;
            float imgHeight = 300 * globalAnim;

            String texture = Vanta.instance.moduleStorage.getT(ClickGUI.class).mascot.getValue() + ".png";
            if (texture.startsWith("longboy")) {
                imgHeight = 400 * globalAnim;
            }

            ImageRectangle
                    .create(width - 175 - (imgWidth / 2), height - 210 - (imgHeight / 2), imgWidth, imgHeight, -1)
                    .resource(ImageUtil.getTexture(texture))
                    .push(event);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(0.8f + (0.2f * globalAnim), 0.8f + (0.2f * globalAnim), 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        float panelHeight = 16;
        for (Category category : Category.values()) {
            Vector2f position = category.position;
            boolean hoverCat = RenderUtil.hovered(mouseX, mouseY, position.x, position.y, PANEL_WIDTH, panelHeight);

            position = drag(position, (int) mouseX, (int) mouseY, category, hoverCat);

            Rectangle
                    .create(position.x, position.y, PANEL_WIDTH, panelHeight)
                    .color(new Color(30, 30, 30))
                    .push(event);

            CFonts.SFPT_SEMIBOLD_20.drawString(category.name, position.x + 3, position.y + 1, Color.WHITE);

            float ignoreThis = 0;
            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                float moduleAnim = getAnimationValue(module, module.isExpanded() ? 1f : 0f, 250, Easing.EASE_OUT_QUART);
                ignoreThis += 14;

                float moduleContentHeight = 0;
                if (!module.frozen)
                    moduleContentHeight += 14;

                if (!module.settings.isEmpty()) {
                    for (Setting<?> setting : module.settings) {
                        if (setting.isHidden()) {
                            continue;
                        }

                        if (setting instanceof BooleanSetting) {
                            moduleContentHeight += 14;
                        } else if (setting instanceof NumberSetting) {
                            moduleContentHeight += 20;
                        } else if (setting instanceof StringSetting) {
                            StringSetting selector = (StringSetting) setting;
                            float settingAnim = getAnimationValue(setting, selector.expanded ? 1f : 0f, 250, Easing.EASE_OUT_EXPO);
                            moduleContentHeight += 12 + (selector.allValues.length * 9 * settingAnim);
                        } else if (setting instanceof MultiStringSetting) {
                            MultiStringSetting selector = (MultiStringSetting) setting;
                            float settingAnim = getAnimationValue(setting, selector.expanded ? 1f : 0f, 250, Easing.EASE_OUT_EXPO);
                            moduleContentHeight += 12 + (selector.allValues.length * 9 * settingAnim);
                        }
                    }
                }
                ignoreThis += moduleContentHeight * moduleAnim;
            }

            Rectangle
                    .create(position.x, position.y + 14, PANEL_WIDTH, ignoreThis + 2)
                    .color(new Color(30, 30, 30))
                    .push(event);

            float y = position.y + 14;
            float x = position.x;

            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                boolean hoverMod = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 1, PANEL_WIDTH - 3, 14);
                if (Vanta.instance.moduleStorage.getT(Theme.class).theme.isValue("Monochrome")) {
                    color1 = new Color(200,200,200);
                    textColor = Color.WHITE;
                } else {
                    textColor = Color.white;
                    color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
                }

                Rectangle
                        .create(x + 1.5f, y, PANEL_WIDTH - 3, 14)
                        .color(hoverMod ? new Color(50, 50, 50) : new Color(40, 40, 40))
                        .push(event);

                CFonts.SFPT_MEDIUM_18.drawString(module.name, x + 3, y + 1, ColorUtil.interpolateColor(textColor, color1, getAnimationValue(module.name + "_enabled", module.isEnabled() ? 1f : 0f, 200, Easing.EASE_OUT_QUAD)));
                CFonts.SFPT_MEDIUM_18.drawString(module.isExpanded() ? "-" : "+", x + PANEL_WIDTH - CFonts.SFPT_MEDIUM_18.getStringWidth(module.isExpanded() ? "-" : "+") - 7, y + 1, hoverMod ? Color.LIGHT_GRAY : Color.WHITE);

                y += 14;

                float moduleAnim = getAnimationValue(module, module.isExpanded() ? 1f : 0f, 250, Easing.EASE_OUT_EXPO);

                if (moduleAnim > 0) {
                    if (!module.frozen) {
                        boolean hoverKeybind = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, PANEL_WIDTH - 3, 14 * moduleAnim);
                        Rectangle
                                .create(x + 1.5f, y, PANEL_WIDTH - 3, 14 * moduleAnim)
                                .color(hoverKeybind ? new Color(42, 42, 42) : new Color(38, 38, 38))
                                .push(event);

                        if (moduleAnim > 0.5f) {
                            sett.drawString("Keybind", x + 3, y + 2, -1);

                            String keyName = Keyboard.getKeyName(module.key);
                            float kbFade = getAnimationValue(module + "_kb_fade", (listeningModule != null && listeningModule.equals(module)) ? 1f : 0f, 200, Easing.LINEAR);
                            if (kbFade > 0) {
                                keyName = "...";
                            }

                            float targetKBWidth = sett.getStringWidth(keyName);
                            float animatedKBWidth = getAnimationValue(module + "_kb_width", targetKBWidth, 200, Easing.EASE_OUT_QUAD);

                            float bXKey = x + PANEL_WIDTH - 5;
                            Rectangle
                                    .create(bXKey - animatedKBWidth - 2, y + 2.5, animatedKBWidth + 4, 9)
                                    .color(new Color(45, 45, 45))
                                    .push(event);
                            sett.drawString(keyName, bXKey - animatedKBWidth - 1, y + 2, ColorUtil.interpolateColor(Color.WHITE, Color.GRAY, kbFade));
                        }

                        y += 14 * moduleAnim;
                    }

                    if (!module.settings.isEmpty()) {
                        for (Setting<?> setting : module.settings) {
                            if (setting.isHidden()) {
                                continue;
                            }

                            boolean hover = RenderUtil.hovered(mouseX, mouseY, position.x + 1.5f, y, PANEL_WIDTH - 3, 14 * moduleAnim);

                            if (setting instanceof BooleanSetting) {
                                BooleanSetting toggle = (BooleanSetting) setting;
                                Rectangle
                                        .create(x + 1.5f, y, PANEL_WIDTH - 3, 14 * moduleAnim)
                                        .color(hover ? new Color(40, 40, 40) : new Color(35, 35, 35))
                                        .push(event);

                                if (moduleAnim > 0.5f) {
                                    float toggleAnim = getAnimationValue(toggle, toggle.getValue() ? 1f : 0f, 200, Easing.EASE_OUT_QUAD);
                                    float bX = x + PANEL_WIDTH - 5;
                                    Rectangle
                                            .create(bX - 17, y + 3.5, 17, 7)
                                            .color(ColorUtil.interpolateColor(new Color(0xA3A3A3), color1.brighter(), toggleAnim))
                                            .push(event);

                                    Rectangle
                                            .create(bX - 17 - 1 + (9 * toggleAnim), y + 2.5, 9, 9)
                                            .color(ColorUtil.interpolateColor(Color.WHITE, color1, toggleAnim))
                                            .push(event);

                                    sett.drawString(setting.name, x + 3, y + 2, -1);
                                }
                                y += 14 * moduleAnim;
                            } else if (setting instanceof NumberSetting) {
                                NumberSetting slider = (NumberSetting) setting;

                                float value = slider.getValue().floatValue();
                                float animatedValue = getAnimationValue(slider, value, 100, Easing.LINEAR);
                                float min = slider.min.floatValue();
                                float max = slider.max.floatValue();
                                float width = Math.min(Math.max((animatedValue - min) / (max - min), 0), 1) * 111;

                                Rectangle
                                        .create(x + 1.5f, y, PANEL_WIDTH - 3, 20 * moduleAnim)
                                        .color(hover ? new Color(40, 40, 40) : new Color(35, 35, 35))
                                        .push(event);

                                if (moduleAnim > 0.5f) {
                                    Rectangle
                                            .create(x + 5, y + 14, 111, 3)
                                            .color(color1.darker())
                                            .push(event);
                                    Rectangle
                                            .create(x + 5, y + 14, width, 3)
                                            .color(color1)
                                            .push(event);

                                    float handleX = x + 5 + width - 2;
                                    if (width >= 111) {
                                        handleX = x + 5 + 108 - 2;
                                    } else if (width <= 5) {
                                        handleX = x + 5;
                                    }
                                    Rectangle
                                            .create(handleX, y + 14 - 1, 5, 5)
                                            .color(Color.WHITE)
                                            .push(event);

                                    sett.drawString(setting.name, x + 3, y + 2, -1);

                                    String format = "%." + slider.places + "f";
                                    String formattedValue = String.format(format, value) + slider.suffix;
                                    sett.drawString(formattedValue, x + PANEL_WIDTH - 5 - sett.getStringWidth(formattedValue), y + 2, -1);
                                }

                                if (RenderUtil.hovered(mouseX, mouseY, x + 5, y, 112, 18) && Mouse.isButtonDown(0)) {
                                    double normalizedX = (mouseX - (x + 5)) / 111.0;
                                    double newValue = min + normalizedX * (max - min);
                                    newValue = MathUtil.round(newValue, slider.places);
                                    newValue = Math.min(Math.max(newValue, min), max);
                                    slider.setValue(newValue);
                                }

                                y += 20 * moduleAnim;
                            } else if (setting instanceof StringSetting) {
                                StringSetting selector = (StringSetting) setting;
                                float settingAnim = getAnimationValue(setting, selector.expanded ? 1f : 0f, 250, Easing.EASE_OUT_EXPO);
                                float settingHeight = 12 + (selector.allValues.length * 9 * settingAnim);

                                boolean hover2 = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, PANEL_WIDTH - 3, settingHeight * moduleAnim);

                                Rectangle
                                        .create(x + 1.5f, y, PANEL_WIDTH - 3, settingHeight * moduleAnim)
                                        .color(hover2 ? new Color(40, 40, 40) : new Color(35, 35, 35))
                                        .push(event);

                                if (moduleAnim > 0.5f) {
                                    sett.drawString(setting.name, x + 3, y + 1, -1);
                                    float bX = x + PANEL_WIDTH - 14;

                                    float targetW = sett.getStringWidth(selector.getValue());
                                    if (selector.expanded) {
                                        for (String mode : selector.allValues) {
                                            targetW = Math.max(targetW, sett.getStringWidth(mode));
                                        }
                                    }
                                    float animatedWidth = getAnimationValue(setting + "_width", targetW, 200, Easing.EASE_OUT_QUAD);

                                    Rectangle
                                            .create(bX - animatedWidth - 2, y + 1.5f, animatedWidth + 4, 9 + (selector.allValues.length * 9 * settingAnim))
                                            .color(new Color(45, 45, 45))
                                            .push(event);

                                    sett.drawString(selector.getValue(), bX - sett.getStringWidth(selector.getValue()) - 1, y + 1, -1);
                                    sett.drawString(selector.expanded ? "-" : "+", bX + 3.5f, y + 0.7f, -1);

                                    if (settingAnim > 0.1f) {
                                        float yOffset = y + 8;
                                        for (String mode : selector.allValues) {
                                            if (yOffset + 9 > y + settingHeight) break;
                                            boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - animatedWidth - 2, yOffset + 2.5f, animatedWidth + 4, 9);
                                            boolean enabledMode = selector.getValue().equals(mode);
                                            sett.drawString(mode, bX - sett.getStringWidth(mode) - 0.5f, yOffset + 1, hoverMode ? enabledMode ? color1.darker() : Color.LIGHT_GRAY : enabledMode ? color1 : Color.WHITE);
                                            yOffset += 9;
                                        }
                                    }
                                }
                                y += settingHeight * moduleAnim;
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting selector = (MultiStringSetting) setting;
                                float settingAnim = getAnimationValue(setting, selector.expanded ? 1f : 0f, 250, Easing.EASE_OUT_EXPO);
                                float settingHeight = 12 + (selector.allValues.length * 9 * settingAnim);

                                boolean hover2 = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, PANEL_WIDTH - 3, settingHeight * moduleAnim);
                                Rectangle
                                        .create(x + 1.5f, y, PANEL_WIDTH - 3, settingHeight * moduleAnim)
                                        .color(hover2 ? new Color(40, 40, 40) : new Color(35, 35, 35))
                                        .push(event);

                                if (moduleAnim > 0.5f) {
                                    sett.drawString(setting.name, x + 3, y + 1, -1);
                                    float bX = x + PANEL_WIDTH - 14;
                                    String enabled = selector.getValue().length + " Enabled";

                                    float targetW = sett.getStringWidth(enabled);
                                    if (selector.expanded) {
                                        for (String mode : selector.allValues) {
                                            targetW = Math.max(targetW, sett.getStringWidth(mode));
                                        }
                                    }
                                    float animatedWidth = getAnimationValue(setting + "_width", targetW, 200, Easing.EASE_OUT_QUAD);
                                    Rectangle
                                            .create(bX - animatedWidth - 2, y + 1.5f, animatedWidth + 4, 9 + (selector.allValues.length * 9 * settingAnim))
                                            .color(new Color(45, 45, 45))
                                            .push(event);

                                    sett.drawString(enabled, bX - sett.getStringWidth(enabled) - 1, y + 1, -1);
                                    sett.drawString(selector.expanded ? "-" : "+", bX + 3.5f, y + 0.7f, -1);

                                    if (settingAnim > 0.1f) {
                                        float yOffset = y + 8;
                                        for (String mode : selector.allValues) {
                                            if (yOffset + 9 > y + settingHeight) break;
                                            boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - animatedWidth - 2, yOffset + 2.5f, animatedWidth + 4, 9);
                                            boolean enabledMode = selector.isEnabled(mode);
                                            sett.drawString(mode, bX - sett.getStringWidth(mode) - 0.5f, yOffset + 1, hoverMode ? enabledMode ? color1.darker() : Color.LIGHT_GRAY : enabledMode ? color1 : Color.WHITE);
                                            yOffset += 9;
                                        }
                                    }
                                }
                                y += settingHeight * moduleAnim;
                            }
                        }
                    }
                }
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (Category category : Category.values()) {
            Vector2f position = category.position;

            float y = position.y + 14;
            float x = position.x;

            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                boolean hoverMod = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 1, PANEL_WIDTH - 3, 14);

                if (hoverMod && mouseButton == 0) {
                    module.setEnabled(!module.isEnabled());
                } else if (hoverMod && mouseButton == 1) {
                    module.setExpanded(!module.isExpanded());
                }

                y += 14;

                float moduleAnim = animationMap.getOrDefault(module, module.isExpanded() ? 1f : 0f);

                if (moduleAnim > 0.5f) {
                    if (!module.frozen) {
                        if (RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y + 2.5f, PANEL_WIDTH - 3, 9)) {
                            if (listeningModule != null && listeningModule.equals(module))
                                listeningModule = null;
                            else
                                listeningModule = module;
                        }

                        y += 14 * moduleAnim;
                    }

                    if (!module.settings.isEmpty()) {
                        for (Setting<?> setting : module.settings) {
                            if (setting.isHidden()) {
                                continue;
                            }

                            if (setting instanceof BooleanSetting) {
                                BooleanSetting toggle = (BooleanSetting) setting;

                                float bX = x + PANEL_WIDTH - 5;
                                if (RenderUtil.hovered(mouseX, mouseY, bX - 17, y + 3.5f, 17, 7) && mouseButton == 0) {
                                    toggle.setValue(!toggle.getValue());
                                }

                                y += 14 * moduleAnim;
                            } else if (setting instanceof NumberSetting) {
                                y += 20 * moduleAnim;
                            } else if (setting instanceof StringSetting) {
                                StringSetting selector = (StringSetting) setting;
                                float settingAnim = animationMap.getOrDefault(setting, selector.expanded ? 1f : 0f);
                                float settingHeight = 12 + (selector.allValues.length * 9 * settingAnim);
                                boolean hover = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, PANEL_WIDTH - 3, 12);

                                if (hover && (mouseButton == 0 || mouseButton == 1)) {
                                    selector.expanded = !selector.expanded;
                                }

                                if (settingAnim > 0.5f) {
                                    float bX = x + PANEL_WIDTH - 14;

                                    float targetW = sett.getStringWidth(selector.getValue());
                                    if (selector.expanded) {
                                        for (String mode : selector.allValues) {
                                            targetW = Math.max(targetW, sett.getStringWidth(mode));
                                        }
                                    }
                                    float animatedWidth = animationMap.getOrDefault(setting + "_width", targetW);

                                    float yOffset = y + 8;
                                    for (String mode : selector.allValues) {
                                        if (yOffset + 9 > y + settingHeight) break;
                                        boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - animatedWidth - 2, yOffset + 2.5f, animatedWidth + 4, 9);
                                        if (hoverMode && mouseButton == 0) {
                                            selector.setValue(mode);
                                        }
                                        yOffset += 9;
                                    }
                                }
                                y += settingHeight * moduleAnim;
                            } else if (setting instanceof MultiStringSetting) {
                                MultiStringSetting selector = (MultiStringSetting) setting;
                                float settingAnim = animationMap.getOrDefault(setting, selector.expanded ? 1f : 0f);
                                float settingHeight = 12 + (selector.allValues.length * 9 * settingAnim);
                                boolean hover = RenderUtil.hovered(mouseX, mouseY, x + 1.5f, y, PANEL_WIDTH - 3, 12);

                                if (hover && (mouseButton == 0 || mouseButton == 1)) {
                                    selector.expanded = !selector.expanded;
                                }

                                if (settingAnim > 0.5f) {
                                    float bX = x + PANEL_WIDTH - 14;
                                    String enabled = selector.getValue().length + " Enabled";

                                    float targetW = sett.getStringWidth(enabled);
                                    if (selector.expanded) {
                                        for (String mode : selector.allValues) {
                                            targetW = Math.max(targetW, sett.getStringWidth(mode));
                                        }
                                    }
                                    float animatedWidth = animationMap.getOrDefault(setting + "_width", targetW);

                                    float yOffset = y + 8;
                                    for (String mode : selector.allValues) {
                                        if (yOffset + 9 > y + settingHeight) break;
                                        boolean hoverMode = RenderUtil.hovered(mouseX, mouseY, bX - animatedWidth - 2, yOffset + 2.5f, animatedWidth + 4, 9);
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
                                }
                                y += settingHeight * moduleAnim;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closing = true;
            return;
        }
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
    public boolean doesGuiPauseGame() {
        return Vanta.instance.moduleStorage.getT(ClickGUI.class).pauseGame.getValue();
    }

    private float getAnimationValue(Object key, float target, long duration, Easing easing) {
        if (!animationMap.containsKey(key)) {
            animationMap.put(key, target);
            return target;
        }

        float current = animationMap.get(key);
        Animation active = activeAnimations.get(key);

        if (active == null || (active.end != target)) {
            if (active != null) active.stop();
            Animation anim = Animation.create(current, target, duration, easing, v -> animationMap.put(key, v));
            activeAnimations.put(key, anim);
            anim.start();
        }

        return animationMap.get(key);
    }
}