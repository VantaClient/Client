package today.vanta.util.game.render.font;

import today.vanta.Vanta;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class CFonts {

    public static CFontRenderer HN_MEDIUM_24 = getFont("HN-Medium", 24);
    public static CFontRenderer HN_REGULAR_48 = getFont("HN-Regular", 48);

    public static CFontRenderer SFPT_MEDIUM_18 = getFont("SFPT-Medium", 18);
    public static CFontRenderer SFPT_SEMIBOLD_20 = getFont("SFPT-Semibold", 20);
    public static CFontRenderer SFPT_MEDIUM_24 = getFont("SFPT-Medium", 24);
    public static CFontRenderer SFPT_SEMIBOLD_42 = getFont("SFPT-Semibold", 42);

    public static CFontRenderer getFont(String fontName, float size) {
        return new CFontRenderer(getAwtFont(fontName + ".otf", size));
    }

    private static Font getAwtFont(String fontName, float size) {
        Font customFont = Font.getFont("SansSerif");
        try (InputStream fontStream = CFonts.class.getResourceAsStream("/assets/vanta/fonts/" + fontName)) {
            if (fontStream != null) {
               customFont = getAwtFont(fontStream, size);
            }
        } catch (Exception e) {
        }
        return customFont;
    }

    private static Font getAwtFont(InputStream inputStream, float size) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            return font.deriveFont(size);
        } catch (FontFormatException | IOException e) {
            Vanta.instance.logger.warn("Failed to get font", e);
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }

}
