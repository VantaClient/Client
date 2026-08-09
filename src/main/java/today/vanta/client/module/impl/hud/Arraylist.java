package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.module.impl.hud.arraylist.ArraylistRenderer;
import today.vanta.client.module.impl.hud.arraylist.BitMapRenderer;
import today.vanta.client.module.impl.hud.arraylist.GlyphRenderer;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.ColorUtil;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Arraylist extends Module {
    private static final Pattern SPACE_OUT_PATTERN_1 = Pattern.compile("(?<=[a-z])(?=[A-Z])");
    private static final Pattern SPACE_OUT_PATTERN_2 = Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Za-z])(?=\\d)");

    private final NumberSetting
            xOffset = Setting.of("X offset", 5, 0, 25),
            yOffset = Setting.of("Y offset", 5, 0, 25),
            animationLength = Setting.of("Animation length", 250, 0, 1000, "ms");

    private final StringSetting
            font = Setting.of("Font", "SFPT", "SFPT", "Tahoma", "IBM Plex Sans", "Roboto", "Geist", "Minecraft", "Exhibition"),
            fontStyle = Setting.of("Font style", "Medium", "Light", "Medium", "Semibold", "Bold", "Heavy", "Regular").hide(() -> !font.isValue("SFPT"));

    private final NumberSetting fontSize = Setting.of("Font size", 24, 10, 42, "px").hide(() -> !font.isValue("SFPT") && !font.isValue("Tahoma"));

    private final BooleanSetting
            fontShadow = Setting.of("Font shadow", true),
            suffixes = Setting.of("Show suffixes", true),
            background = Setting.of("Background", true),
            spaceOut = Setting.of("Space out", false);

    private final StringSetting line = Setting.of("Line", "Full", "Full", "Left", "Right", "Top", "Top+right", "None");

    private final NumberSetting backgroundAlpha = Setting.of("Background alpha", 100, 0, 255).hide(() -> !background.getValue());
    private final StringSetting
            moduleCase = Setting.of("Module case", "Default", "Default", "Lowercase", "Uppercase"),
            colorMode = Setting.of("Color mode", "Theme", "Theme", "Rainbow", "Random", "Category", "Fade");

    private final Map<Module, ArraylistEntry> entryMap = new HashMap<>();
    private final List<ArraylistEntry> entries = new ArrayList<>();

    public Arraylist() {
        super("Arraylist", "Draws an arraylist of modules.", Category.HUD);
        displayNames = new String[]{"Arraylist", "ArrayList", "ModuleList"};
        hideFromArraylist = true;
        setEnabled(true);

        font.addListener(((setting, oldValue, newValue) -> setFont()));
        fontStyle.addListener(((setting, oldValue, newValue) -> setFont()));
        fontSize.addListener(((setting, oldValue, newValue) -> setFont()));

        setFont();
    }

    private ArraylistRenderer arraylistFontRenderer = new GlyphRenderer(CFonts.SFPT_MEDIUM_24);

    private void setFont() {
        switch (font.getValue()) {
            case "Exhibition":
                arraylistFontRenderer = new BitMapRenderer(mc.exhiFontRendererObj);
                break;
            case "Minecraft":
                arraylistFontRenderer = new BitMapRenderer(mc.fontRendererObj);
                break;
            case "Tahoma":
                arraylistFontRenderer = new GlyphRenderer(CFonts.getFont("T-Regular", fontSize.getValue().intValue()));
                break;
            case "IBM Plex Sans":
                arraylistFontRenderer = new GlyphRenderer(CFonts.getFont("IBMPlexSans-Regular", fontSize.getValue().intValue()));
                break;
            case "Roboto":
                arraylistFontRenderer = new GlyphRenderer(CFonts.getFont("Roboto-Regular", fontSize.getValue().intValue()));
                break;
            case "Geist":
                arraylistFontRenderer = new GlyphRenderer(CFonts.getFont("Geist-Regular", fontSize.getValue().intValue()));
                break;
            default:
                arraylistFontRenderer = new GlyphRenderer(CFonts.getFont("SFPT-" + fontStyle.getValue(), fontSize.getValue().intValue()));
                break;
        }
    }

    private void updateEntries() {
        for (Module module : Vanta.instance.moduleStorage.list) {
            boolean desiredVisible = module.isEnabled() && !module.hideFromArraylist;
            ArraylistEntry entry = entryMap.get(module);

            if (desiredVisible) {
                if (entry == null) {
                    float width = getModuleWidth(module);
                    float offscreen = width + xOffset.getValue().floatValue() + 5;
                    entry = new ArraylistEntry(module, offscreen);
                    entryMap.put(module, entry);
                    entries.add(entry);
                    entry.visible = true;
                    entry.animateTo(0, animationLength.getValue().longValue());
                } else if (!entry.visible) {
                    entry.visible = true;
                    entry.animateTo(0, animationLength.getValue().longValue());
                }
            } else if (entry != null && entry.visible) {
                entry.visible = false;
                float width = getModuleWidth(module);
                float offscreen = width + xOffset.getValue().floatValue() + 5;
                entry.animateTo(offscreen, animationLength.getValue().longValue());
            }
        }

        entries.removeIf(entry -> {
            if (!entry.visible && entry.isAnimationFinished()) {
                entryMap.remove(entry.module);
                return true;
            }
            return false;
        });
    }

    private float getModuleWidth(Module module) {
        return arraylistFontRenderer.getStringWidth(getModuleName(module));
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        updateEntries();

        if (entries.isEmpty()) {
            return;
        }

        entries.sort((e1, e2) -> {
            int compare = Float.compare(getModuleWidth(e2.module), getModuleWidth(e1.module));
            if (compare != 0) {
                return compare;
            }
            return e1.module.name.compareTo(e2.module.name);
        });

        Color primaryColor = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        Color secondaryColor = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[1];
        Color backgroundColor = new Color(0, 0, 0, backgroundAlpha.getValue().intValue());

        float y = yOffset.getValue().floatValue();
        int counter = 0;
        for (ArraylistEntry entry : entries) {
            Module module = entry.module;
            String name = getModuleName(module);

            float modWidth = arraylistFontRenderer.getStringWidth(name);
            float x = event.scaledResolution.getScaledWidth() - modWidth - xOffset.getValue().floatValue() - 2.5f + entry.slideOffset;

            Color color = primaryColor;

            switch (colorMode.getValue()) {
                case "Category":
                    color = module.category.color;
                    break;
                case "Random":
                    color = module.color;
                    break;
                case "Rainbow":
                    color = new Color(ColorUtil.getRainbow(3000, (int) (counter * 150L)));
                    break;
                case "Fade":
                    color = new Color(ColorUtil.fadeBetween(primaryColor.getRGB(), secondaryColor.getRGB(), counter * 150L));
                    break;
            }

            float rectX = x - 2;
            float rectY = y;
            float rectWidth = modWidth + 5;
            float rectHeight = arraylistFontRenderer.getBoxHeight();

            if (background.getValue()) {
                Rectangle
                        .create(rectX, rectY, rectWidth, rectHeight)
                        .color(backgroundColor)
                        .push(event);
            }

            boolean first = counter == 0;
            boolean last = counter == entries.size() - 1;
            ArraylistEntry nextEntry = last ? null : entries.get(counter + 1);

            switch (line.getValue()) {
                case "Top+right":
                case "Top":
                    if (first) {
                        Rectangle
                                .create(rectX, rectY - 1, rectWidth, 1)
                                .color(color)
                                .push(event);
                    }

                    if (line.isValue("Top+right")) {
                        Rectangle
                                .create(rectX + rectWidth, rectY - 1, 1, rectHeight + 1)
                                .color(color)
                                .push(event);
                    }
                    break;
                case "Full":
                    if (first) {
                        Rectangle
                                .create(rectX, rectY - 1, rectWidth, 1)
                                .color(color)
                                .push(event);
                        Rectangle
                                .create(rectX - 1, rectY - 1, 1, 1)
                                .color(color)
                                .push(event);
                    }

                    if (last) {
                        Rectangle
                                .create(rectX, rectY + rectHeight, rectWidth, 1)
                                .color(color)
                                .push(event);
                    } else {
                        String nextName = getModuleName(nextEntry.module);
                        float nextModWidth = arraylistFontRenderer.getStringWidth(nextName);
                        float nextX = event.scaledResolution.getScaledWidth() - nextModWidth - xOffset.getValue().floatValue() - 2.5f + nextEntry.slideOffset;
                        float nextRectX = nextX - 2;
                        float widthToNext = nextRectX - rectX;

                        if (widthToNext > 0) {
                            Rectangle
                                    .create(rectX, rectY + rectHeight, widthToNext, 1)
                                    .color(color)
                                    .push(event);
                        }
                    }

                    Rectangle
                            .create(rectX - 1, rectY, 1, rectHeight)
                            .color(color)
                            .push(event);
                    Rectangle
                            .create(rectX - 1, rectY + rectHeight, 1, 1)
                            .color(color)
                            .push(event);
                    Rectangle
                            .create(rectX + rectWidth, rectY - 1, 1, rectHeight + 2)
                            .color(color)
                            .push(event);
                    break;

                case "Left":
                    Rectangle
                            .create(rectX - 1, rectY, 1, rectHeight)
                            .color(color)
                            .push(event);
                    break;

                case "Right":
                    Rectangle
                            .create(rectX + rectWidth, rectY, 1, rectHeight)
                            .color(color)
                            .push(event);
                    break;
            }

            arraylistFontRenderer.drawString(name, x + 1f, y + 1f, color, fontShadow.getValue());

            y += (background.getValue() || !line.isValue("None")) ? arraylistFontRenderer.getBoxHeight() : arraylistFontRenderer.getFontHeight() + 2;
            counter++;
        }
    }

    private String getModuleName(Module module) {
        String name = module.displayName;
        String suffix;

        if (module.getSuffix() != null && module.addSuffix && suffixes.getValue()) {
            suffix = module.getSuffix();
            if (spaceOut.getValue()) {
                suffix = SPACE_OUT_PATTERN_1.matcher(suffix).replaceAll(" ");
            }
            name += "§f" + suffix;
        }

        if (spaceOut.getValue()) {
            name = SPACE_OUT_PATTERN_2.matcher(name).replaceAll(" ");
        } else {
            name = name.replace("§", " §");
        }

        switch (moduleCase.getValue()) {
            case "Lowercase":
                name = name.toLowerCase();
                break;
            case "Uppercase":
                name = name.toUpperCase();
                name = name.replace("§F", "§f");
                break;
        }

        return name;
    }

    private static class ArraylistEntry {
        final Module module;
        float slideOffset;
        boolean visible;
        Animation animation;

        ArraylistEntry(Module module, float slideOffset) {
            this.module = module;
            this.slideOffset = slideOffset;
        }

        void animateTo(float target, long duration) {
            if (animation != null) {
                animation.stop();
            }

            animation = Animation.create(slideOffset, target, duration, Easing.EASE_OUT_QUART, value -> slideOffset = value);
            animation.start();
        }

        boolean isAnimationFinished() {
            return animation == null || animation.finished;
        }
    }
}
