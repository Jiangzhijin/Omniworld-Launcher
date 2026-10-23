package org.jackhuang.hmcl.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.PopupWindow;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.event.ActionEvent;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.setting.GameDirectoryManager;
import org.jackhuang.hmcl.setting.StyleSheets;
import org.jackhuang.hmcl.ui.account.AccountListPage;
import org.jackhuang.hmcl.ui.account.MicrosoftAccountLoginPane;
import org.jackhuang.hmcl.ui.construct.AdvancedListItem;
import org.jackhuang.hmcl.ui.construct.LineButtonBase;
import org.jackhuang.hmcl.ui.decorator.DecoratorAnimatedPage;
import org.jackhuang.hmcl.ui.download.DownloadPage;
import org.jackhuang.hmcl.ui.instances.GameListPage;
import org.jackhuang.hmcl.ui.main.LauncherSettingsPage;
import org.jackhuang.hmcl.ui.main.OmniworldSettingsPage;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/// Opt-in desktop smoke check; renders real functional pages using isolated launcher storage.
@NotNullByDefault
@EnabledIfEnvironmentVariable(named = "OMNIWORLD_UI_QA", matches = "true")
final class WorkspaceAppearanceTest {
    /// Checks real page skins at normal and compact sizes and exports PNGs for visual review.
    @Test
    void rendersFunctionalWorkspaces() throws Exception {
        Path output = Path.of("build", "ui-review").toAbsolutePath();
        Files.createDirectories(output);
        System.setProperty("hmcl.home", output.resolve("user").toString());
        System.setProperty("hmcl.dir", output.resolve("workspace").toString());
        System.setProperty("user.dir", output.resolve("sandbox").toString());
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> ready.complete(null));
        ready.get(20, TimeUnit.SECONDS);
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                SettingsManager.init();
                SettingsManager.settings().animationDisabledProperty().set(true);
                GameDirectoryManager.init();
                LauncherSettingsPage settings = new LauncherSettingsPage();
                settings.showGameSettings(GameDirectoryManager.getSelectedRepository());
                render(settings, "settings", output);
                for (String tab : java.util.List.of("settings.launcher.general", "settings.launcher.appearance", "download", "help", "contact", "about", "java.management")) {
                    selectSettingsTab(settings, tab);
                    render(settings, "tab-" + tab, output);
                }
                render(new AccountListPage(), "accounts", output);
                render(new GameListPage(), "library", output);
                render(new DownloadPage(), "downloads", output);
                render(new OmniworldSettingsPage(), "preferences", output);
                render(new MicrosoftAccountLoginPane(), "microsoft-login", output);
                reviewResolution(output, done);
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        });
        try {
            done.get(90, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    /// Opens the real editable resolution menu, verifies the migrated value, and records the popup.
    private static void reviewResolution(Path output, CompletableFuture<Void> done) {
        LauncherSettingsPage page = new LauncherSettingsPage();
        page.showGameSettings(GameDirectoryManager.getSelectedRepository());
        StackPane root = new StackPane(page);
        Scene scene = new Scene(root, 1120, 680);
        StyleSheets.init(scene);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        root.applyCss();
        root.layout();
        String title = org.jackhuang.hmcl.util.i18n.I18n.i18n("settings.game.window_type");
        for (Node node : page.lookupAll(".options-sublist-header")) {
            if (node instanceof LineButtonBase header && title.equals(header.getTitle())) {
                header.fireEvent(new ActionEvent());
            }
        }
        Platform.runLater(() -> {
            try {
                root.applyCss();
                root.layout();
                ComboBox<?> resolution = page.lookupAll(".jfx-combo-box").stream()
                        .filter(node -> node instanceof ComboBox<?> box && box.getItems().contains("1600x900"))
                        .map(node -> (ComboBox<?>) node).findFirst().orElseThrow();
                assertEquals("1600x900", resolution.getValue());
                resolution.show();
                Platform.runLater(() -> {
                    try {
                        PopupWindow popup = Window.getWindows().stream()
                                .filter(window -> window instanceof PopupWindow && window.isShowing())
                                .map(window -> (PopupWindow) window).findFirst().orElseThrow();
                        popup.getScene().getRoot().applyCss();
                        popup.getScene().getRoot().layout();
                        save(popup.getScene().getRoot().snapshot(null, null), output.resolve("resolution-menu.png"));
                        resolution.hide();
                        done.complete(null);
                    } catch (Throwable failure) {
                        done.completeExceptionally(failure);
                    } finally {
                        stage.close();
                    }
                });
            } catch (Throwable failure) {
                stage.close();
                done.completeExceptionally(failure);
            }
        });
    }

    /// Applies production styles and checks the new rail before saving a full-page image.
    private static void render(Node page, String name, Path output) throws Exception {
        if (page instanceof MicrosoftAccountLoginPane dialog) {
            dialog.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        StackPane root = new StackPane(page);
        Scene scene = new Scene(root, 1120, 680);
        StyleSheets.init(scene);
        root.applyCss();
        root.layout();
        if (page instanceof DecoratorAnimatedPage) {
            assertNotNull(page.lookup(".voyage-rail"), name);
            assertNotNull(page.lookup(".voyage-workspace-banner"), name);
            assertTrue(page.lookup(".voyage-workspace-content").getBoundsInParent().getWidth() > 600, name);
        }
        save(root.snapshot(null, null), output.resolve(name + ".png"));
        root.resize(960, 580);
        root.layout();
        save(root.snapshot(null, null), output.resolve(name + "-compact.png"));
    }

    /// Selects real navigation tiles so deferred tabs and their controls are included in review.
    private static void selectSettingsTab(LauncherSettingsPage page, String key) {
        String title = org.jackhuang.hmcl.util.i18n.I18n.i18n(key);
        for (Node node : page.lookupAll(".navigation-drawer-item")) {
            if (node instanceof AdvancedListItem item && title.equals(item.getTitle())) {
                item.fireEvent(new ActionEvent());
                return;
            }
        }
        fail("Settings navigation missing: " + key);
    }

    /// Writes JavaFX snapshot pixels without requiring the optional javafx.swing module.
    private static void save(WritableImage snapshot, Path path) throws Exception {
        int width = (int) snapshot.getWidth();
        int height = (int) snapshot.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        assertTrue(ImageIO.write(image, "png", path.toFile()));
    }
}
