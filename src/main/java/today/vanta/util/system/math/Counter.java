package today.vanta.util.system.math;

public class Counter {

    private long lastTimestamp = System.currentTimeMillis();

    public long getElapsedTime() {
        return System.currentTimeMillis() - lastTimestamp;
    }

    public void reset() {
        lastTimestamp = System.currentTimeMillis();
    }

    public boolean hasElapsed(long duration, boolean reset) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTimestamp >= duration) {
            if (reset) {
                this.lastTimestamp = currentTime;
            }
            return true;
        }
        return false;
    }

    public boolean hasElapsed(long duration) {
        return hasElapsed(duration, false);
    }
}