package today.vanta.util.game.render.font.impl;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import today.vanta.util.game.render.font.IRenderer;
import today.vanta.util.game.render.font.msdf.MsdfFont;
import today.vanta.util.game.render.font.msdf.MsdfGlyph;
import today.vanta.util.game.render.font.msdf.MsdfShader;
import today.vanta.util.system.math.ColorUtil;

import java.awt.Color;

public final class GlyphFontRenderer implements IRenderer {
    private final MsdfFont font;
    private final float size;
    private final int[] colorCodes = new int[32];

    public GlyphFontRenderer(final MsdfFont font, final float size) {
        this(font, size, null);
    }

    public GlyphFontRenderer(final MsdfFont font, final float size, final char[] requiredCharacters) {
        this.font = font;
        this.size = size / 2.0F;
        setupMinecraftColorCodes();

        if (requiredCharacters != null)
            for (final char character : requiredCharacters)
                if (!font.hasGlyph(character))
                    throw new IllegalArgumentException(font.getName() + " MSDF atlas is missing U+" + Integer.toHexString(character).toUpperCase());
    }

    @Override
    public float drawString(final String text, final float x, final float y, final int color, final boolean shadow) {
        return draw(text, x, y, color, shadow, null);
    }

    @Override
    public float drawStringWithShadow(final String text, final float x, final float y, final int color) {
        return Math.max(
                drawString(text, x + 1.0F, y + 1.0F, color, true),
                drawString(text, x, y, color, false)
        );
    }

    @Override
    public float drawHorizontalGradientString(
            final String text,
            final float x,
            final float y,
            final Color startColor,
            final Color endColor,
            final double speed,
            final int spacing
    ) {
        return draw(text, x, y, -1, false, new Gradient(startColor, endColor, speed, spacing));
    }

    @Override
    public int getFontHeight() {
        return Math.round(font.getMetrics().getLineHeight() * size);
    }

    @Override
    public int getStringWidth(final String text) {
        if (text == null) return 0;

        float width = 0.0F;
        int previousCodePoint = -1;
        for (int index = 0; index < text.length(); index++) {
            final char character = text.charAt(index);
            if (character == '§' && index + 1 < text.length()) {
                index++;
                continue;
            }

            final MsdfGlyph glyph = font.getGlyph(character);
            if (glyph == null) continue;

            if (previousCodePoint >= 0)
                width += font.getKerning(previousCodePoint, glyph.getCodePoint()) * size;
            width += glyph.getAdvance() * size;
            previousCodePoint = glyph.getCodePoint();
        }
        return Math.round(width);
    }

    private float draw(
            final String text,
            final float startX,
            final float y,
            final int color,
            final boolean shadow,
            final Gradient gradient
    ) {
        if (text == null) return 0.0F;

        final int alpha = (color >>> 24) < 4 ? 255 : color >>> 24;
        final int baseColor = shadow
                ? alpha << 24 | (color >> 16 & 0xFF) / 4 << 16 | (color >> 8 & 0xFF) / 4 << 8 | (color & 0xFF) / 4
                : alpha << 24 | color & 0xFFFFFF;
        final int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final MsdfShader shader = MsdfShader.getInstance();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        final int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.bindTexture(font.getTextureId());
        final int previousProgram = shader.use(font);

        float x = startX;
        int previousCodePoint = -1;
        int currentColor = baseColor;
        boolean useGradient = gradient != null;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;

        for (int index = 0; index < text.length(); index++) {
            final char character = text.charAt(index);
            if (character == '§' && index + 1 < text.length()) {
                final int formatIndex = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(++index)));
                if (formatIndex >= 0 && formatIndex < 16) {
                    bold = false;
                    italic = false;
                    strikethrough = false;
                    underline = false;
                    useGradient = false;
                    currentColor = alpha << 24 | colorCodes[shadow ? formatIndex + 16 : formatIndex];
                } else if (formatIndex == 17)
                    bold = true;
                else if (formatIndex == 18)
                    strikethrough = true;
                else if (formatIndex == 19)
                    underline = true;
                else if (formatIndex == 20)
                    italic = true;
                else if (formatIndex == 21) {
                    bold = false;
                    italic = false;
                    strikethrough = false;
                    underline = false;
                    useGradient = gradient != null;
                    currentColor = baseColor;
                }
                continue;
            }

            final MsdfGlyph glyph = font.getGlyph(character);
            if (glyph == null) continue;

            if (previousCodePoint >= 0)
                x += font.getKerning(previousCodePoint, glyph.getCodePoint()) * size;

            if (useGradient) {
                final double offset = (System.currentTimeMillis() * gradient.speed + index * gradient.spacing) % 2000.0D / 2000.0D;
                currentColor = ColorUtil.getGradientColor(gradient.startColor, gradient.endColor, Math.abs(Math.sin(offset * Math.PI)));
            }

            GlStateManager.color(
                    (currentColor >> 16 & 0xFF) / 255.0F,
                    (currentColor >> 8 & 0xFF) / 255.0F,
                    (currentColor & 0xFF) / 255.0F,
                    ((currentColor >>> 24) < 4 ? 255 : currentColor >>> 24) / 255.0F
            );
            shader.setThickness(bold ? 0.09F : 0.05F);
            GL11.glBegin(GL11.GL_QUADS);
            glyph.draw(x, y + font.getMetrics().getBaselineHeight() * size, size, italic);
            GL11.glEnd();

            if (strikethrough)
                drawLine(shader, x, y + getFontHeight() / 2.0F, x + glyph.getAdvance() * size, currentColor);
            if (underline)
                drawLine(shader, x, y + getFontHeight() - 1.0F, x + glyph.getAdvance() * size, currentColor);

            x += glyph.getAdvance() * size;
            previousCodePoint = glyph.getCodePoint();
        }

        shader.restore(previousProgram);
        GlStateManager.bindTexture(previousTexture);
        GlStateManager.setActiveTexture(previousActiveTexture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        return x;
    }

    private void drawLine(
            final MsdfShader shader,
            final float startX,
            final float y,
            final float endX,
            final int color
    ) {
        shader.restore(0);
        GlStateManager.disableTexture2D();
        GlStateManager.color(
                (color >> 16 & 0xFF) / 255.0F,
                (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                ((color >>> 24) < 4 ? 255 : color >>> 24) / 255.0F
        );
        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(startX, y);
        GL11.glVertex2f(endX, y);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        shader.use(font);
    }

    private void setupMinecraftColorCodes() {
        for (int index = 0; index < 32; index++) {
            final int adjustment = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + adjustment;
            int green = (index >> 1 & 1) * 170 + adjustment;
            int blue = (index & 1) * 170 + adjustment;

            if (index == 6)
                red += 85;
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            colorCodes[index] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }
    }

    private static final class Gradient {
        private final Color startColor, endColor;
        private final double speed;
        private final int spacing;

        private Gradient(final Color startColor, final Color endColor, final double speed, final int spacing) {
            this.startColor = startColor;
            this.endColor = endColor;
            this.speed = speed;
            this.spacing = spacing;
        }
    }
}
