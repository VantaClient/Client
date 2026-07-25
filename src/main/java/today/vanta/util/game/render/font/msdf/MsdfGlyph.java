package today.vanta.util.game.render.font.msdf;

import org.lwjgl.opengl.GL11;

public final class MsdfGlyph {
    private final int codePoint;
    private final float advance;
    private final float planeLeft, planeBottom, planeRight, planeTop;
    private final float minimumU, minimumV, maximumU, maximumV;

    public MsdfGlyph(final MsdfFontData.GlyphData data, final float atlasWidth, final float atlasHeight) {
        codePoint = data.getUnicode();
        advance = data.getAdvance();

        if (data.getPlaneBounds() == null) {
            planeLeft = 0.0F;
            planeBottom = 0.0F;
            planeRight = 0.0F;
            planeTop = 0.0F;
        } else {
            planeLeft = data.getPlaneBounds().getLeft();
            planeBottom = data.getPlaneBounds().getBottom();
            planeRight = data.getPlaneBounds().getRight();
            planeTop = data.getPlaneBounds().getTop();
        }

        if (data.getAtlasBounds() == null) {
            minimumU = 0.0F;
            minimumV = 0.0F;
            maximumU = 0.0F;
            maximumV = 0.0F;
        } else {
            minimumU = data.getAtlasBounds().getLeft() / atlasWidth;
            minimumV = 1.0F - data.getAtlasBounds().getTop() / atlasHeight;
            maximumU = data.getAtlasBounds().getRight() / atlasWidth;
            maximumV = 1.0F - data.getAtlasBounds().getBottom() / atlasHeight;
        }
    }

    public void draw(final float x, final float baselineY, final float size, final boolean italic) {
        if (planeLeft == planeRight || planeBottom == planeTop) return;

        final float left = x + planeLeft * size;
        final float right = x + planeRight * size;
        final float top = baselineY - planeTop * size;
        final float bottom = baselineY - planeBottom * size;
        final float topShear = italic ? (baselineY - top) * 0.2F : 0.0F;
        final float bottomShear = italic ? (baselineY - bottom) * 0.2F : 0.0F;

        GL11.glTexCoord2f(minimumU, minimumV);
        GL11.glVertex2f(left + topShear, top);
        GL11.glTexCoord2f(minimumU, maximumV);
        GL11.glVertex2f(left + bottomShear, bottom);
        GL11.glTexCoord2f(maximumU, maximumV);
        GL11.glVertex2f(right + bottomShear, bottom);
        GL11.glTexCoord2f(maximumU, minimumV);
        GL11.glVertex2f(right + topShear, top);
    }

    public int getCodePoint() {
        return codePoint;
    }

    public float getAdvance() {
        return advance;
    }
}
