package today.vanta.util.system.math.animation;

import today.vanta.client.processor.impl.AnimationProcessor;

import java.util.function.Consumer;

public class Animation {
    public final float start, end;
    public final long duration;
    public float progress;
    private final Easing easing;
    private final Consumer<Float> onUpdate;

    public long startTime;
    public boolean started = false;
    public boolean finished = false;

    private Animation(float start, float end, long duration, Easing easing, Consumer<Float> onUpdate) {
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.easing = easing;
        this.onUpdate = onUpdate;
    }

    /**
     * Creates an animation.
     *
     * @param from     The value that the animation will start from
     * @param to       The value that the animation will update to
     * @param duration The duration of the animation in milliseconds
     * @param easing   The easing type of the animation
     * @param onUpdate The value that will be updated every frame
     * @return The animation instance
     */
    public static Animation create(float from, float to, long duration, Easing easing, Consumer<Float> onUpdate) {
        return new Animation(from, to, duration, easing, onUpdate);
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.started = true;
        this.finished = false;

        AnimationProcessor.getInstance().register(this);
    }

    public void stop() {
        this.finished = true;
    }

    public void update() {
        if (!started || finished) return;

        long elapsed = System.currentTimeMillis() - startTime;
        progress = (float) elapsed / duration;

        if (progress >= 1.0f) {
            progress = 1.0f;
            finished = true;
        }

        float current = start + (end - start) * easing.ease(progress);
        onUpdate.accept(current);
    }
}