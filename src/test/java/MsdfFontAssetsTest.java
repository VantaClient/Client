import com.google.gson.Gson;
import today.vanta.Vanta;
import today.vanta.util.game.render.font.Icons;
import today.vanta.util.game.render.font.msdf.MsdfFontData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class MsdfFontAssetsTest {
    private static final String[] FONT_NAMES = new String[]{
            "badabb",
            "hn-medium",
            "hn-regular",
            "icons",
            "ocr-b",
            "roboto-regular",
            "rusticroadway",
            "sfpt-bold",
            "sfpt-heavy",
            "sfpt-light",
            "sfpt-medium",
            "sfpt-regular",
            "sfpt-semibold",
            "t-regular"
    };

    private MsdfFontAssetsTest() {
        /* w */
    }

    public static void main(final String[] args) throws Exception {
        for (final String fontName : FONT_NAMES)
            validate(fontName);
        Vanta.instance.logger.info("Validated {} MSDF fonts.", FONT_NAMES.length);
    }

    private static void validate(final String fontName) throws Exception {
        final String resourceRoot = "/assets/vanta/msdf/" + fontName;
        try (
                InputStream dataStream = MsdfFontAssetsTest.class.getResourceAsStream(resourceRoot + ".json");
                InputStream atlasStream = MsdfFontAssetsTest.class.getResourceAsStream(resourceRoot + ".png")
        ) {
            if (dataStream == null || atlasStream == null)
                throw new AssertionError("Missing MSDF assets for " + fontName);

            final MsdfFontData data = new Gson().fromJson(
                    new InputStreamReader(dataStream, StandardCharsets.UTF_8),
                    MsdfFontData.class
            );
            final BufferedImage atlas = ImageIO.read(atlasStream);
            if (data == null || data.getAtlas() == null || data.getMetrics() == null || atlas == null)
                throw new AssertionError("Invalid MSDF assets for " + fontName);
            if (atlas.getWidth() != data.getAtlas().getWidth() || atlas.getHeight() != data.getAtlas().getHeight())
                throw new AssertionError("Atlas dimensions do not match metadata for " + fontName);

            final Set<Integer> codePoints = new HashSet<>();
            for (final MsdfFontData.GlyphData glyph : data.getGlyphs())
                codePoints.add(glyph.getUnicode());

            if (fontName.equals("icons")) {
                for (final char character : Icons.CHARS)
                    if (!codePoints.contains((int) character))
                        throw new AssertionError("Icons atlas is missing U+" + Integer.toHexString(character).toUpperCase());
            } else if (!codePoints.contains((int) '?'))
                throw new AssertionError(fontName + " has no fallback glyph");
        }
    }
}
