package today.vanta.util.system.math;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class ColorUtil {
    private static final Random random = new Random();

    public static int applyAlpha(int color, float alpha) {
        return (int) (alpha * 255.0F) << 24 | color & 16777215;
    }

    public static int randomColor() {
        return new Color(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
        ).getRGB() & 0xFFFFFF;
    }

    public static int getRainbow(int speed, int offset) {
        float hue = (System.currentTimeMillis() + offset) % speed;
        hue /= speed;
        return Color.getHSBColor(hue, 0.85f, 1f).getRGB();
    }

    public static int fadeBetween(int startColour, int endColour, long offset) {
        return fadeBetween(startColour, endColour, ((System.currentTimeMillis() + offset) % 2000L) / 1000.0);
    }

    private static int fadeBetween(int startColour, int endColour, double progress) {
        if (progress > 1) progress = 1 - progress % 1;
        return fadeTo(startColour, endColour, progress);
    }

    private static int fadeTo(int startColour, int endColour, double progress) {
        double invert = 1.0 - progress;
        int r = (int) ((startColour >> 16 & 0xFF) * invert +
                (endColour >> 16 & 0xFF) * progress);
        int g = (int) ((startColour >> 8 & 0xFF) * invert +
                (endColour >> 8 & 0xFF) * progress);
        int b = (int) ((startColour & 0xFF) * invert +
                (endColour & 0xFF) * progress);
        int a = (int) ((startColour >> 24 & 0xFF) * invert +
                (endColour >> 24 & 0xFF) * progress);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    public static float calculateDarkPixelRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;
        int darkPixelCount = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = new Color(image.getRGB(x, y), true);

                if (isDarkOrBrown(color)) {
                    darkPixelCount++;
                }
            }
        }

        return (float) darkPixelCount / totalPixels;
    }

    public static boolean isDarkOrBrown(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        boolean isDark = (r < 90 && g < 90 && b < 90);

        boolean isBrown = (r > 80 && g > 40 && b < 100 && (r - g) > 20);

        return isDark || isBrown;
    }

    public static int getGradientColor(Color color1, Color color2, double step) {
        step = Math.max(0.0, Math.min(1.0, step));

        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * step);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * step);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * step);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * step);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Color getDarker(Color color, int factor) {
        Color colortoDark = color;
        for (int i = 0; i < factor; i++) {
            colortoDark = colortoDark.darker();
        }
        return colortoDark;
    }

    public static Color interpolateColor(Color start, Color end, float progress) {
        progress = Math.min(1.0f, Math.max(0.0f, progress));
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * progress);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * progress);
        return new Color(r, g, b, a);
    }
}