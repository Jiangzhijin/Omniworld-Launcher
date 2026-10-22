package org.jackhuang.hmcl.setting;

import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/// Checks that the user's motion preference survives saving and reopening the launcher.
@NotNullByDefault
final class OmniworldPreferencesTest {
    /// New games use the requested resolution while explicit user choices remain effective.
    @Test
    void resolvesDefaultGameWindowAndCustomOverride() {
        GameSettings.Preset preset = new GameSettings.Preset(GameSettingsPresetID.NIL);
        GameSettings.Effective settings = GameSettings.resolve(preset, null);
        assertEquals(1920, settings.getWidth());
        assertEquals(1680, settings.getHeight());
        preset.widthProperty().setValue(1280.0);
        preset.heightProperty().setValue(720.0);
        assertEquals(1280, settings.getWidth());
        assertEquals(720, settings.getHeight());
    }

    /// Ambient effects default to enabled and an explicit opt-out persists through JSON storage.
    @Test
    void persistsEffectsOptOut() {
        LauncherSettings settings = new LauncherSettings();
        assertTrue(settings.magicEffectsProperty().get());
        assertEquals(0, settings.effectsFrameRateProperty().get());
        settings.magicEffectsProperty().set(false);
        settings.effectsFrameRateProperty().set(144);
        settings.effectsIntensityProperty().set(90);
        settings.clickSoundsProperty().set(false);
        settings.cuteCursorProperty().set(false);
        LauncherSettings restored = Objects.requireNonNull(LauncherSettings.fromJson(
                JsonParser.parseString(settings.toJson()).getAsJsonObject()));
        assertFalse(restored.magicEffectsProperty().get());
        assertEquals(144, restored.effectsFrameRateProperty().get());
        assertEquals(90, restored.effectsIntensityProperty().get());
        assertFalse(restored.clickSoundsProperty().get());
        assertFalse(restored.cuteCursorProperty().get());
        assertTrue(restored.keepLauncherOpenProperty().get());
    }
}
