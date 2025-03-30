package today.vanta.util.system.math;

public class MathUtil {
    public static double round(double value, int places) {
        if (places < 0) places = 0;

        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static float lerp(float pct, float start, float end) {
        return start + pct * (end - start);
    }

    public static float interpolateRotation(float previous, float current, float partialTicks) {
        float delta = current - previous;
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        while (delta >= 180.0F) {
            delta -= 360.0F;
        }
        return previous + partialTicks * delta;
    }

}
