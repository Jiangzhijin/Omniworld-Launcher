package org.jackhuang.hmcl.ui.main;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/// Scene-local cute pointer and short control-click feedback; never changes the system mouse cursor.
@NotNullByDefault
public final class UiFeedback {
    /// Asynchronous sound work is coalesced instead of queued for every mouse event.
    private static final AtomicBoolean SOUND_PENDING = new AtomicBoolean();
    /// The lazily loaded, reusable PCM click clip.
    private static volatile @Nullable Clip sound;
    /// Prevents a pending asynchronous load from retaining audio resources after shutdown.
    private static volatile boolean stopping;
    /// Remembers an unavailable audio device to avoid repeated initialization failures.
    private static boolean soundUnavailable;
    /// Last submitted click time, used to limit rapid-fire feedback.
    private static long lastSound;
    /// Cute cursor shared by nodes in this scene.
    private final ImageCursor cuteCursor = createCursor();
    /// The node whose cursor is temporarily replaced while hovered.
    private @Nullable Node hovered;
    /// Cursor value restored when the hovered node changes or the preference is disabled.
    private @Nullable Cursor previous;

    /// Installs event filters once on the main launcher scene.
    public static void install(Scene scene) {
        UiFeedback feedback = new UiFeedback();
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> feedback.pointer(scene, event));
        scene.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (event.getTarget() == scene) feedback.restore();
        });
        SettingsManager.settings().cuteCursorProperty().addListener(observable -> feedback.restore());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || !SettingsManager.settings().clickSoundsProperty().get()) return;
            if (event.getTarget() instanceof Node target) {
                for (@Nullable Node node = target; node != null; node = node.getParent()) {
                    if (node.isDisabled()) return;
                    if (node instanceof ButtonBase || node instanceof ComboBoxBase<?> || node instanceof Slider) {
                        playClick();
                        return;
                    }
                }
            }
        });
    }

    /// Replaces ordinary pointers while preserving text entry, wait cursors, and window-resize edges.
    private void pointer(Scene scene, MouseEvent event) {
        restore();
        if (!SettingsManager.settings().cuteCursorProperty().get()
                || event.getSceneX() < 16 || event.getSceneY() < 40
                || event.getSceneX() > scene.getWidth() - 16 || event.getSceneY() > scene.getHeight() - 16
                || !(event.getTarget() instanceof Node target) || target.cursorProperty().isBound()) return;
        @Nullable Cursor effective = null;
        for (@Nullable Node node = target; node != null; node = node.getParent()) {
            if (node instanceof TextInputControl) return;
            if (effective == null) effective = node.getCursor();
        }
        if (effective != null && effective != Cursor.DEFAULT && effective != Cursor.HAND) return;
        hovered = target;
        previous = target.getCursor();
        target.setCursor(cuteCursor);
    }

    /// Restores the node's original cursor without changing any operating-system preference.
    private void restore() {
        if (hovered != null && !hovered.cursorProperty().isBound()) hovered.setCursor(previous);
        hovered = null;
        previous = null;
    }

    /// Draws a small pearl-and-lavender cat pointer with a precise arrow-tip hotspot.
    private static ImageCursor createCursor() {
        Canvas canvas = new Canvas(36, 40);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#fff7fc"));
        g.setStroke(Color.web("#8e6aa9"));
        g.setLineWidth(1.6);
        g.beginPath();
        g.moveTo(3, 2); g.lineTo(5, 26); g.lineTo(11, 20); g.lineTo(17, 30);
        g.lineTo(21, 27); g.lineTo(15, 17); g.lineTo(23, 17); g.closePath();
        g.fill(); g.stroke();
        g.setFill(Color.web("#fff5fc"));
        g.beginPath();
        g.moveTo(14, 22); g.lineTo(13, 13); g.lineTo(20, 17); g.lineTo(25, 16);
        g.lineTo(32, 12); g.lineTo(32, 23); g.bezierCurveTo(34, 36, 12, 36, 14, 22);
        g.closePath(); g.fill(); g.stroke();
        g.setFill(Color.web("#efb8d4"));
        g.fillOval(15, 25, 5, 3); g.fillOval(27, 24, 5, 3);
        g.setFill(Color.web("#725086"));
        g.fillOval(19, 23, 2, 3); g.fillOval(26, 22, 2, 3);
        g.setStroke(Color.web("#ab7c9c")); g.strokeLine(23, 27, 24, 28); g.strokeLine(24, 28, 25, 27);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return new ImageCursor(canvas.snapshot(parameters, null), 3, 2);
    }

    /// Loads and plays a quiet click on a worker so audio-device access never blocks the UI thread.
    private static void playClick() {
        long now = System.nanoTime();
        if (now - lastSound < 70_000_000L || !SOUND_PENDING.compareAndSet(false, true)) return;
        lastSound = now;
        CompletableFuture.runAsync(() -> {
            try {
                if (stopping || !SettingsManager.settings().clickSoundsProperty().get() || soundUnavailable) return;
                if (sound == null) {
                    @Nullable var resource = UiFeedback.class.getResourceAsStream("/assets/audio/click.wav");
                    if (resource == null) return;
                    try (var input = new BufferedInputStream(resource); var audio = AudioSystem.getAudioInputStream(input)) {
                        Clip candidate = AudioSystem.getClip();
                        try { candidate.open(audio); } catch (Exception failure) { candidate.close(); throw failure; }
                        sound = candidate;
                    }
                }
                if (stopping) { sound.close(); return; }
                sound.stop();
                sound.setFramePosition(0);
                sound.start();
            } catch (Exception unavailable) {
                soundUnavailable = true;
            } finally {
                SOUND_PENDING.set(false);
            }
        });
    }

    /// Releases the audio device when the launcher is stopped.
    public static void dispose() {
        stopping = true;
        @Nullable Clip current = sound;
        if (current != null) current.close();
    }
}
