/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

final class TidalMotion {
    private static long lastFrameNanos;
    private static float frameSeconds = 1.0f / 60.0f;

    static final class Spring {
        private float value;
        private float velocity;

        Spring(float initialValue) {
            value = initialValue;
        }

        float step(float target, float stiffness, float dampingRatio) {
            if (!TidalConfig.uiAnimations) {
                value = target;
                velocity = 0.0f;
                return value;
            }
            float angularFrequency = (float) Math.sqrt(stiffness);
            int steps = Math.max(1, (int) Math.ceil(frameSeconds / (1.0f / 120.0f)));
            float delta = frameSeconds / steps;
            for (int i = 0; i < steps; i++) {
                float acceleration = stiffness * (target - value)
                        - 2.0f * dampingRatio * angularFrequency * velocity;
                velocity += acceleration * delta;
                value += velocity * delta;
            }
            if (Math.abs(target - value) < 0.001f && Math.abs(velocity) < 0.001f) {
                value = target;
                velocity = 0.0f;
            }
            return value;
        }
    }

    private TidalMotion() {}

    static void beginFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            frameSeconds = Math.min(0.05f,
                    Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000.0f));
        }
        lastFrameNanos = now;
    }

    static float approach(float value, float target, float response) {
        if (!TidalConfig.uiAnimations) return target;
        float amount = 1.0f - (float) Math.exp(-response * frameSeconds);
        return value + (target - value) * amount;
    }
}
