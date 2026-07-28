package today.vanta.util.game.render.font.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import today.vanta.util.game.render.font.CFont;
import today.vanta.util.game.render.font.IRenderer;
import today.vanta.util.system.math.ColorUtil;

import java.awt.*;

/**
 * @author <a href="https://github.com/IUDevman/gamesense-client/tree/master">GAMESENSE Client</a>
 */
public class GlyphFontRenderer extends CFont implements IRenderer {
    protected CharData[] boldChars = new CharData[256];
    protected CharData[] italicChars = new CharData[256];
    protected CharData[] boldItalicChars = new CharData[256];

    private final int[] colorCode = new int[32];
    private final String colorCodeCharacters = "0123456789abcdefklmnor";

    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;
    protected DynamicTexture texItalicBold;

    public GlyphFontRenderer(Font font) {
        this(font, null);
    }

    public GlyphFontRenderer(Font font, char[] customChars) {
        super(font, customChars);
        setupMinecraftColorCodes();
        setupBoldItalicIDs(customChars);
    }

    @Override
    public float drawYCenteredString(String text, float x, float y, Color color, boolean dropShadow) {
        return drawString(text, x, y - getFontHeight() / 2F, color.getRGB(), dropShadow);
    }

    @Override
    public float drawStringWithShadow(String text, float x, float y, Color color) {
        float shadowWidth = drawString(text, x + 1, y + 1, color.getRGB(), true);
        return Math.max(shadowWidth, drawString(text, x, y, color.getRGB(), false));
    }

    @Override
    public float drawStringWithShadow(String text, float x, float y, int color) {
        float shadowWidth = drawString(text, x + 1, y + 1, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    @Override
    public float drawHorizontalGradientString(String text, float x, float y, Color startColor, Color endColor, double speed, int spacing) {
        if (text == null) return 0.0F;

        CharData[] currentData = this.charData;

        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;

        boolean useGradient = true;
        int currentColorRGB = 0;

        x *= 2.0F;
        y *= 2.0F;

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex.getGlTextureId());

        int size = text.length();
        long time = (long) (System.currentTimeMillis() * speed);

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if ((character == '§') && (i + 1 < size)) {
                char codeChar = text.charAt(i + 1);
                int colorIndex = colorCodeCharacters.indexOf(codeChar);

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;

                    useGradient = false;
                    currentColorRGB = colorCode[colorIndex];

                } else if (colorIndex == 21) { // Reset (§r)
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;

                    useGradient = true;

                } else if (colorIndex == 17) { // Bold
                    bold = true;
                    if (italic) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texBold.getGlTextureId());
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 18) {
                    strikethrough = true;
                } else if (colorIndex == 19) {
                    underline = true;
                } else if (colorIndex == 20) { // Italic
                    italic = true;
                    if (bold) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texItalic.getGlTextureId());
                        currentData = this.italicChars;
                    }
                }
                i++;
            } else if ((character < currentData.length) && (character >= 0)) {

                int colorRGB;

                if (useGradient) {
                    double offset = (time + (i * spacing)) % 2000 / 2000.0;
                    double factor = Math.abs(Math.sin(offset * Math.PI));
                    colorRGB = ColorUtil.getGradientColor(startColor, endColor, factor);
                } else {
                    colorRGB = currentColorRGB;
                }

                int cr = (colorRGB >> 16) & 0xFF;
                int cg = (colorRGB >> 8) & 0xFF;
                int cb = colorRGB & 0xFF;

                int ca = ((colorRGB >> 24) & 0xFF) == 0 ? 255 : ((colorRGB >> 24) & 0xFF);

                GlStateManager.color(cr / 255.0F, cg / 255.0F, cb / 255.0F, ca / 255.0F);

                GlStateManager.glBegin(GL11.GL_TRIANGLES);
                drawChar(currentData, character, x, y);
                GlStateManager.glEnd();

                if (strikethrough)
                    drawLine(x, y + (double) currentData[character].height / 2, x + currentData[character].width - 8.0D, y + (double) currentData[character].height / 2, 1.0F);
                if (underline)
                    drawLine(x, y + currentData[character].height - 2.0D, x + currentData[character].width - 8.0D, y + currentData[character].height - 2.0D, 1.0F);

                x += currentData[character].width - 8 + this.charOffset;
            }
        }

        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GlStateManager.popMatrix();
        return x / 2.0F;
    }

    @Override
    public float drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null)
            return 0.0F;

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        if (alpha < 4) alpha = 255;

        if (shadow) {
            red /= 4;
            green /= 4;
            blue /= 4;
        }

        CharData[] currentData = this.charData;

        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;

        x *= 2.0F;
        y *= 2.0F;

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.color(
                red / 255.0f,
                green / 255.0f,
                blue / 255.0f,
                alpha / 255.0f
        );

        int size = text.length();

        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex.getGlTextureId());

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if ((character == '§') && (i < size)) {
                int colorIndex = 21;

                try {
                    colorIndex = colorCodeCharacters.indexOf(text.charAt(i + 1));
                } catch (Exception ignored) {
                }

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;

                    if (colorIndex < 0 || colorIndex > 15)
                        colorIndex = 15;

                    if (shadow) colorIndex += 16;

                    int colorCode = this.colorCode[colorIndex];

                    int cr = (colorCode >> 16) & 0xFF;
                    int cg = (colorCode >> 8) & 0xFF;
                    int cb = colorCode & 0xFF;

                    GlStateManager.color(cr / 255.0F, cg / 255.0F, cb / 255.0F, alpha / 255.0F);
                } else if (colorIndex == 17) {
                    bold = true;

                    if (italic) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texBold.getGlTextureId());
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 18) {
                    strikethrough = true;
                } else if (colorIndex == 19) {
                    underline = true;
                } else if (colorIndex == 20) {
                    italic = true;

                    if (bold) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texItalic.getGlTextureId());
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.color(
                            red / 255.0f,
                            green / 255.0f,
                            blue / 255.0f,
                            alpha / 255.0f
                    );
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;
                }
                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                GlStateManager.glBegin(GL11.GL_TRIANGLES);
                drawChar(currentData, character, (float) x, (float) y);
                GlStateManager.glEnd();

                if (strikethrough)
                    drawLine(x, y + (double) currentData[character].height / 2, x + currentData[character].width - 8.0D, y + (double) currentData[character].height / 2, 1.0F);
                if (underline)
                    drawLine(x, y + currentData[character].height - 2.0D, x + currentData[character].width - 8.0D, y + currentData[character].height - 2.0D, 1.0F);

                x += currentData[character].width - 8 + this.charOffset;
            }
        }
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GlStateManager.popMatrix();
        return x / 2.0F;
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null) {
            return 0;
        }
        int width = 0;
        CharData[] currentData = this.charData;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if ((character == '§') && (i < size)) {
                int colorIndex = colorCodeCharacters.indexOf(character);
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;
                    if (italic) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 20) {
                    italic = true;
                    if (bold) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                }
                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                width += currentData[character].width - 8 + this.charOffset;
            }
        }
        return width / 2;
    }

    public void setFont(Font font) {
        super.setFont(font);
        setupBoldItalicIDs(this.customChars);
    }

    private void setupBoldItalicIDs(char[] customChars) {
        int size = this.charData.length;
        this.boldChars = new CharData[size];
        this.italicChars = new CharData[size];
        this.boldItalicChars = new CharData[size];
        this.texBold = setupTexture(this.font.deriveFont(Font.BOLD), this.boldChars, customChars);
        this.texItalic = setupTexture(this.font.deriveFont(Font.ITALIC), this.italicChars, customChars);
        this.texItalicBold = setupTexture(this.font.deriveFont(Font.BOLD | Font.ITALIC), this.boldItalicChars, customChars);
    }

    private void drawLine(double x, double y, double x1, double y1, float width) {
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(width);
        GlStateManager.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x1, y1);
        GlStateManager.glEnd();
        GlStateManager.enableTexture2D();
    }

    private void setupMinecraftColorCodes() {
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index >> 0 & 0x1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }
}