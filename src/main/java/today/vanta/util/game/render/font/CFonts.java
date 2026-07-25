package today.vanta.util.game.render.font;

import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.font.msdf.MsdfFont;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CFonts {
    private static final Map<String, MsdfFont> FONT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, GlyphFontRenderer> RENDERER_CACHE = new ConcurrentHashMap<>();

    public static final GlyphFontRenderer SFPT_MEDIUM_18 = getFont("SFPT-Medium", 18);
    public static final GlyphFontRenderer SFPT_SEMIBOLD_20 = getFont("SFPT-Semibold", 20);
    public static final GlyphFontRenderer SFPT_MEDIUM_24 = getFont("SFPT-Medium", 24);
    public static final GlyphFontRenderer SFPT_REGULAR_18 = getFont("SFPT-Regular", 18);
    public static final GlyphFontRenderer SFPT_REGULAR_24 = getFont("SFPT-Regular", 24);

    private CFonts() {
        /* w */
    }

    public static GlyphFontRenderer getFont(final String fontName, final float size) {
        return getFont(fontName, size, null);
    }

    public static GlyphFontRenderer getFont(final String fontName, final float size, final char[] requiredCharacters) {
        final String resourceName = normalizeResourceName(fontName);
        final String key = resourceName + ':' + size + (requiredCharacters == null ? "" : ':' + new String(requiredCharacters));
        return RENDERER_CACHE.computeIfAbsent(
                key,
                ignored -> new GlyphFontRenderer(
                        FONT_CACHE.computeIfAbsent(resourceName, MsdfFont::load),
                        size,
                        requiredCharacters
                )
        );
    }

    public static char[] parseHexChars(final String... hexCodes) {
        final char[] characters = new char[hexCodes.length];
        for (int index = 0; index < hexCodes.length; index++)
            characters[index] = (char) Integer.parseInt(hexCodes[index], 16);
        return characters;
    }

    private static String normalizeResourceName(final String fontName) {
        final String lowerCaseName = fontName.toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(".ttf") || lowerCaseName.endsWith(".otf"))
            return lowerCaseName.substring(0, lowerCaseName.length() - 4);
        return lowerCaseName;
    }
}
