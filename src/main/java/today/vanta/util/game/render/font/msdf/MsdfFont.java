package today.vanta.util.game.render.font.msdf;

import com.google.gson.Gson;
import net.minecraft.client.renderer.texture.DynamicTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MsdfFont {
    private final String name;
    private final DynamicTexture texture;
    private final MsdfFontData.AtlasData atlas;
    private final MsdfFontData.MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Long, Float> kernings;

    private MsdfFont(
            final String name,
            final DynamicTexture texture,
            final MsdfFontData.AtlasData atlas,
            final MsdfFontData.MetricsData metrics,
            final Map<Integer, MsdfGlyph> glyphs,
            final Map<Long, Float> kernings
    ) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    public static MsdfFont load(final String resourceName) {
        final String resourceRoot = "/assets/vanta/msdf/" + resourceName;
        try (
                InputStream dataStream = MsdfFont.class.getResourceAsStream(resourceRoot + ".json");
                InputStream atlasStream = MsdfFont.class.getResourceAsStream(resourceRoot + ".png")
        ) {
            if (dataStream == null || atlasStream == null)
                throw new IllegalArgumentException("Missing MSDF resources for font " + resourceName);

            final MsdfFontData data = new Gson().fromJson(
                    new InputStreamReader(dataStream, StandardCharsets.UTF_8),
                    MsdfFontData.class
            );
            final BufferedImage atlasImage = ImageIO.read(atlasStream);
            if (data == null || data.getAtlas() == null || data.getMetrics() == null || atlasImage == null)
                throw new IllegalArgumentException("Invalid MSDF resources for font " + resourceName);

            final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
            for (final MsdfFontData.GlyphData glyph : data.getGlyphs())
                glyphs.put(glyph.getUnicode(), new MsdfGlyph(glyph, data.getAtlas().getWidth(), data.getAtlas().getHeight()));

            final Map<Long, Float> kernings = new HashMap<>();
            final List<MsdfFontData.KerningData> kerningData = data.getKernings();
            if (kerningData != null)
                for (final MsdfFontData.KerningData kerning : kerningData)
                    kernings.put(kerningKey(kerning.getLeftCharacter(), kerning.getRightCharacter()), kerning.getAdvance());

            final DynamicTexture texture = new DynamicTexture(atlasImage);
            texture.setBlurMipmapDirect(true, false);
            return new MsdfFont(resourceName, texture, data.getAtlas(), data.getMetrics(), glyphs, kernings);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to load MSDF font " + resourceName, exception);
        }
    }

    public MsdfGlyph getGlyph(final int codePoint) {
        final MsdfGlyph glyph = glyphs.get(codePoint);
        return glyph != null ? glyph : glyphs.get((int) '?');
    }

    public boolean hasGlyph(final int codePoint) {
        return glyphs.containsKey(codePoint);
    }

    public float getKerning(final int leftCodePoint, final int rightCodePoint) {
        final Float kerning = kernings.get(kerningKey(leftCodePoint, rightCodePoint));
        return kerning == null ? 0.0F : kerning;
    }

    public String getName() {
        return name;
    }

    public int getTextureId() {
        return texture.getGlTextureId();
    }

    public MsdfFontData.AtlasData getAtlas() {
        return atlas;
    }

    public MsdfFontData.MetricsData getMetrics() {
        return metrics;
    }

    private static long kerningKey(final int leftCodePoint, final int rightCodePoint) {
        return (long) leftCodePoint << 32 | rightCodePoint & 0xFFFFFFFFL;
    }
}
