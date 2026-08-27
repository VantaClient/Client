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

    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");

        return sb.toString();
    }


    public static int range(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static double range(double min, double max) {
        return (Math.random() * (max - min)) + min;
    }

    public static float range(float min, float max) {
        return (float) ((Math.random() * (max - min)) + min);
    }

    public static long range(long min, long max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

}