package today.vanta.util.game.render.font.msdf;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class MsdfFontData {
    private AtlasData atlas;
    private MetricsData metrics;
    private List<GlyphData> glyphs;
    @SerializedName("kerning")
    private List<KerningData> kernings;

    public AtlasData getAtlas() {
        return atlas;
    }

    public MetricsData getMetrics() {
        return metrics;
    }

    public List<GlyphData> getGlyphs() {
        return glyphs;
    }

    public List<KerningData> getKernings() {
        return kernings;
    }

    public static final class AtlasData {
        @SerializedName("distanceRange")
        private float range;
        private float width, height;

        public float getRange() {
            return range;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }
    }

    public static final class MetricsData {
        private float lineHeight, ascender, descender;

        public float getLineHeight() {
            return lineHeight;
        }

        public float getAscender() {
            return ascender;
        }

        public float getDescender() {
            return descender;
        }

        public float getBaselineHeight() {
            return lineHeight + descender;
        }
    }

    public static final class GlyphData {
        private int unicode;
        private float advance;
        private BoundsData planeBounds, atlasBounds;

        public int getUnicode() {
            return unicode;
        }

        public float getAdvance() {
            return advance;
        }

        public BoundsData getPlaneBounds() {
            return planeBounds;
        }

        public BoundsData getAtlasBounds() {
            return atlasBounds;
        }
    }

    public static final class BoundsData {
        private float left, bottom, right, top;

        public float getLeft() {
            return left;
        }

        public float getBottom() {
            return bottom;
        }

        public float getRight() {
            return right;
        }

        public float getTop() {
            return top;
        }
    }

    public static final class KerningData {
        @SerializedName("unicode1")
        private int leftCharacter;
        @SerializedName("unicode2")
        private int rightCharacter;
        private float advance;

        public int getLeftCharacter() {
            return leftCharacter;
        }

        public int getRightCharacter() {
            return rightCharacter;
        }

        public float getAdvance() {
            return advance;
        }
    }
}
