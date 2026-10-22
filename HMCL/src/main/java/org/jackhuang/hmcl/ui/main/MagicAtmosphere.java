package org.jackhuang.hmcl.ui.main;

import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.ui.animation.AnimationUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Display-synchronized magical atmosphere drawn on one mouse-transparent canvas.
@NotNullByDefault
final class MagicAtmosphere extends Pane {
    /// Cached particle color; opacity is applied through the graphics context.
    private static final Color STAR = Color.web("#fff6de");
    /// Violet energy used for the inner magic seal and comet tails.
    private static final Color VIOLET = Color.web("#c6a1ff");
    /// Cached pearl-white orbit color.
    private static final Color PEARL = Color.web("#fff5ff");
    /// One drawing surface shared by all effects.
    private final Canvas canvas = new Canvas();
    /// User preference for the ambient animation.
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    /// Fixed-size pointer trail coordinates, reused for the lifetime of the page.
    private final double[] trailX = new double[32], trailY = new double[32];
    /// Monotonic time of each pointer trail sample.
    private final long[] trailTime = new long[32];
    /// Next pointer sample slot in the fixed-size ring buffer.
    private int trailIndex;
    /// Current pointer location in artwork coordinates.
    private double pointerX = -1000, pointerY = -1000;
    /// Last pointer movement time.
    private long pointerMoved;
    /// Position of the latest click ripple.
    private double rippleX, rippleY;
    /// Start time of the latest click ripple.
    private long rippleStarted;
    /// Next optional frame-limit deadline, preserving cadence across display pulses.
    private long nextFrame;
    /// Animation epoch, reset when the layer is attached or enabled.
    private long epoch;
    /// Start time of the optional developer frame-rate sample.
    private long sampleStarted;
    /// Rendered frames in the optional developer sample.
    private int sampleFrames;
    /// Timer runs only while this layer is attached and effects are enabled.
    private final AnimationTimer timer = new AnimationTimer() {
        /// Renders at display cadence by default, with optional user-selected frame limiting.
        @Override
        public void handle(long now) {
            if (!isActuallyVisible()) return;
            long interval = frameIntervalNanos(SettingsManager.settings().effectsFrameRateProperty().get());
            if (interval > 0 && now < nextFrame) return;
            nextFrame += interval;
            if (nextFrame < now - interval) nextFrame = now + interval;
            draw((now - epoch) / 1_000_000_000.0, now);
            if (Boolean.getBoolean("omniworld.fx.diagnostics")) {
                sampleFrames++;
                if (now - sampleStarted >= 3_000_000_000L) {
                    System.out.printf(java.util.Locale.ROOT, "[Omniworld FX] rendered %.1f FPS%n",
                            sampleFrames * 1_000_000_000.0 / (now - sampleStarted));
                    sampleStarted = now;
                    sampleFrames = 0;
                }
            }
        }
    };

    /// Creates the canvas and manages animation attachment and user preference changes.
    MagicAtmosphere() {
        setMouseTransparent(true);
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        sceneProperty().addListener((observable, oldScene, newScene) -> refreshAnimation());
        enabled.addListener(observable -> refreshAnimation());
    }

    /// Converts a supported optional frame limit to nanoseconds; zero follows the display pulse.
    static long frameIntervalNanos(int fps) {
        return fps > 0 && fps <= 240 ? 1_000_000_000L / fps : 0;
    }

    /// Exposes the persistent ambient-effects switch.
    BooleanProperty enabledProperty() { return enabled; }

    /// Adds a timestamped pointer sample without allocating a new particle object.
    void followPointer(double x, double y) {
        pointerX = x;
        pointerY = y;
        pointerMoved = System.nanoTime();
        trailX[trailIndex] = x;
        trailY[trailIndex] = y;
        trailTime[trailIndex] = pointerMoved;
        trailIndex = (trailIndex + 1) % trailX.length;
    }

    /// Starts an expanding magic seal at the clicked point.
    void ripple(double x, double y) {
        rippleX = x;
        rippleY = y;
        rippleStarted = System.nanoTime();
    }

    /// Stops detached or disabled work and resets animation clocks when resumed.
    private void refreshAnimation() {
        timer.stop();
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (getScene() != null && enabled.get() && AnimationUtils.isAnimationEnabled()) {
            epoch = System.nanoTime();
            sampleStarted = epoch;
            sampleFrames = 0;
            nextFrame = 0;
            timer.start();
        }
    }

    /// Avoids drawing hidden pages, minimized windows, and invisible ancestors.
    private boolean isActuallyVisible() {
        if (getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isShowing()) return false;
        if (getScene().getWindow() instanceof javafx.stage.Stage stage && stage.isIconified()) return false;
        for (@Nullable Node node = this; node != null; node = node.getParent()) {
            if (!node.isVisible() || node.getOpacity() == 0) return false;
        }
        return true;
    }

    /// Draws counter-rotating seals, star streams, comets and a fading pointer trail.
    private void draw(double seconds, long now) {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double radius = Math.min(w * 0.19, h * 0.42);
        g.save();
        g.translate(w * 0.69, h * 0.34);
        seal(g, radius, seconds * 6, 0.46);
        seal(g, radius * 0.73, -seconds * 9, 0.25);
        for (int i = 0; i < 3; i++) {
            double angle = seconds * 0.55 + i * Math.PI * 2 / 3;
            star(g, Math.cos(angle) * radius * 1.06, Math.sin(angle) * radius * 1.06, 5, 0.9);
        }
        g.restore();

        int density = Math.max(20, Math.min(100, SettingsManager.settings().effectsIntensityProperty().get()));
        for (int i = 0; i < density; i++) {
            double x = w * (0.25 + (i * 0.61803398875 % 1) * 0.74) + Math.sin(seconds * 0.35 + i) * 18;
            double phase = i * 0.381966 - seconds * (0.018 + i % 4 * 0.004);
            double y = h * (phase - Math.floor(phase));
            double alpha = (0.40 + 0.35 * Math.sin(seconds * 1.1 + i * 1.8))
                    * Math.min(1, Math.min(y, h - y) / 25);
            star(g, x, y, i % 9 == 0 ? 4.5 : 1.7, Math.max(0, alpha));
        }
        for (int i = 0; i < 3; i++) {
            double phase = seconds * 0.17 + i * 0.37;
            double progress = phase - Math.floor(phase);
            double x = w * (0.30 + progress * 0.80), y = h * (0.04 + i * 0.17 + progress * 0.38);
            double opacity = Math.sin(progress * Math.PI) * 0.7;
            g.setStroke(PEARL);
            for (int segment = 0; segment < 14; segment++) {
                g.setGlobalAlpha(opacity * (1 - segment / 14.0));
                g.setLineWidth(1.6 - segment * 0.08);
                g.strokeLine(x - segment * 8, y - segment * 3, x - (segment + 1) * 8, y - (segment + 1) * 3);
            }
            star(g, x, y, 4, opacity);
        }
        for (int i = 0; i < trailTime.length; i++) {
            double age = (now - trailTime[i]) / 1_000_000_000.0;
            if (age < 0.7) star(g, trailX[i], trailY[i] - age * 22, 3.5 * (1 - age / 0.7), 1 - age / 0.7);
        }
        double pointerAge = (now - pointerMoved) / 1_000_000_000.0;
        if (pointerAge < 1.3) {
            for (int i = 0; i < 7; i++) {
                double angle = seconds * 1.8 + i * Math.PI * 2 / 7;
                double distance = 19 + i % 3 * 11;
                star(g, pointerX + Math.cos(angle) * distance, pointerY + Math.sin(angle) * distance,
                        i % 2 == 0 ? 3.5 : 2, (1 - pointerAge / 1.3) * 0.85);
            }
        }
        double age = (now - rippleStarted) / 1_000_000_000.0;
        if (age < 1.3) {
            g.save();
            g.translate(rippleX, rippleY);
            seal(g, 15 + age * 95, age * 50, (1 - age / 1.3) * 0.85);
            g.restore();
        }
        g.setGlobalAlpha(1);
    }

    /// Draws a double-ring rune seal and two interlocked triangular constellations.
    private static void seal(GraphicsContext g, double radius, double rotation, double opacity) {
        g.save();
        g.rotate(rotation);
        g.setGlobalAlpha(opacity);
        g.setStroke(PEARL);
        g.setLineWidth(0.85);
        g.strokeOval(-radius, -radius, radius * 2, radius * 2);
        g.strokeOval(-radius * 0.94, -radius * 0.94, radius * 1.88, radius * 1.88);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI / 24;
            double inner = radius * (i % 4 == 0 ? 0.88 : 0.96);
            g.strokeLine(Math.cos(angle) * inner, Math.sin(angle) * inner,
                    Math.cos(angle) * radius, Math.sin(angle) * radius);
        }
        g.setGlobalAlpha(opacity * 0.65);
        g.setStroke(VIOLET);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3, b = a + Math.PI * 2 / 3;
            g.strokeLine(Math.cos(a) * radius * 0.88, Math.sin(a) * radius * 0.88,
                    Math.cos(b) * radius * 0.88, Math.sin(b) * radius * 0.88);
        }
        g.restore();
    }

    /// Paints a glowing four-point star with cached colors and graphics-state opacity.
    private static void star(GraphicsContext g, double x, double y, double size, double opacity) {
        g.setFill(VIOLET);
        g.setGlobalAlpha(opacity * 0.12);
        g.fillOval(x - size * 3, y - size * 3, size * 6, size * 6);
        g.setFill(STAR);
        g.setGlobalAlpha(opacity * 0.30);
        g.fillOval(x - size, y - size, size * 2, size * 2);
        g.setStroke(STAR);
        g.setGlobalAlpha(opacity);
        g.setLineWidth(size > 3 ? 1.1 : 0.7);
        g.strokeLine(x - size, y, x + size, y);
        g.strokeLine(x, y - size, x, y + size);
    }
}
