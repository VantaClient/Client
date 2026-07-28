package today.vanta.util.game.render.font;

import today.vanta.util.game.render.font.impl.GlyphFontRenderer;

import java.awt.*;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CFonts {
    private static final Map<String, Font> FONT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, GlyphFontRenderer> RENDERER_CACHE = new ConcurrentHashMap<>();

    // SF Pro Text
    public static final GlyphFontRenderer SFPT_MEDIUM_18 = getFont("SFPT-Medium", 18);
    public static final GlyphFontRenderer SFPT_SEMIBOLD_20 = getFont("SFPT-Semibold", 20);
    public static final GlyphFontRenderer SFPT_MEDIUM_24 = getFont("SFPT-Medium", 24);
    public static final GlyphFontRenderer SFPT_REGULAR_18 = getFont("SFPT-Regular", 18);
    public static final GlyphFontRenderer SFPT_REGULAR_24 = getFont("SFPT-Regular", 24);

    public static GlyphFontRenderer getFont(String fontName, float size) {
        return getFont(fontName, size, null);
    }

    public static GlyphFontRenderer getFont(String fontName, float size, char[] customChars) {
        String key;
        if (customChars != null && customChars.length > 0) {
            key = fontName + ":" + size + ":" + new String(customChars);
        } else {
            key = fontName + ":" + size;
        }

        return RENDERER_CACHE.computeIfAbsent(
                key,
                k -> new GlyphFontRenderer(getAwtFont(fontName + ".otf", size), customChars)
        );
    }

    public static char[] parseHexChars(String... hexCodes) {
        char[] chars = new char[hexCodes.length];
        for (int i = 0; i < hexCodes.length; i++) {
            chars[i] = (char) Integer.parseInt(hexCodes[i], 16);
        }
        return chars;
    }

    private static Font getAwtFont(String fontName, float size) {
        Font baseFont = FONT_CACHE.computeIfAbsent(fontName, name -> {
            for (String candidate : getFontCandidates(name)) {
                try (InputStream in = CFonts.class.getResourceAsStream("/assets/vanta/fonts/" + candidate)) {
                    if (in != null) {
                        return Font.createFont(Font.TRUETYPE_FONT, in);
                    }
                } catch (Exception ignored) {
                }
            }

            return new Font("SansSerif", Font.PLAIN, 12);
        });

        return baseFont.deriveFont(adjustSize(size));
    }

    private static float adjustSize(float size) {
        String vmVendor = System.getProperty("java.vm.vendor", "").toLowerCase();

        if (vmVendor.contains("oracle")) {
            return size / 2.0f;
        }

        if (vmVendor.contains("openj9")) {
            return size / 1.5f;
        }

        return size;
    }

    private static List<String> getFontCandidates(String name) {
        String lower = name.toLowerCase();

        if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
            String base = name.substring(0, name.length() - 4);

            return Arrays.asList(
                    name,
                    lower.endsWith(".ttf") ? base + ".otf" : base + ".ttf"
            );
        }

        return Arrays.asList(
                name,
                name + ".ttf",
                name + ".otf"
        );
    }
}