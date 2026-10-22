package org.jackhuang.hmcl.ui.main;

import com.jfoenix.controls.JFXButton;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.jackhuang.hmcl.game.HMCLGameInstance;
import org.jackhuang.hmcl.setting.Accounts;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.SVG;
import org.jackhuang.hmcl.ui.animation.AnimationUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Full-width Omniworld home with top navigation, an illustrated stage and one integrated launch dock.
@NotNullByDefault
public final class OmniworldHome extends BorderPane {
    /// Selected instance name shown above the launch dock.
    private final Label gameName = label("你的第一段旅途", "voyage-game-name");
    /// Selected instance details or the install hint when no game is present.
    private final Label gameDetail = label("尚未安装游戏 · 从探索中添加一个世界", "voyage-muted");
    /// Container hosting the launcher's existing launch and instance-menu actions.
    private final HBox dock = new HBox(20);

    /// Builds a complete home shell using the existing game, account and settings workflows.
    public OmniworldHome() {
        getStyleClass().add("voyage-home");
        JFXButton home = nav("启程", () -> { });
        home.getStyleClass().add("voyage-nav-selected");
        JFXButton library = nav("世界", () -> Controllers.navigate(Controllers.getGameListPage()));
        JFXButton discover = nav("探索", () -> Controllers.navigate(Controllers.getDownloadPage()));
        JFXButton settings = nav("设置", () -> Controllers.navigate(new OmniworldSettingsPage()));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        JFXButton account = nav("旅人账户", () -> Controllers.navigate(Controllers.getAccountListPage()));
        account.getStyleClass().add("voyage-account");
        account.setGraphic(SVG.PERSON.createIcon(17));
        FXUtils.onChangeAndOperate(Accounts.selectedAccountProperty(), value ->
                account.setText(value == null ? "登录账户" : value.getProfileName()));
        HBox navigation = new HBox(8, home, library, discover, settings, gap, account);
        navigation.setAlignment(Pos.CENTER_LEFT);
        navigation.getStyleClass().add("voyage-navigation");
        setTop(navigation);

        Label edition = label("✦  OMNIWORLD  /  ELAINA EDITION", "voyage-edition");
        Label heading = label("下一站，\n万千世界。", "voyage-heading");
        Label subtitle = label("把日常留在身后，和灰之魔女一起出发。", "voyage-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(310);
        JFXButton explore = nav("发现新的世界  →", () -> Controllers.navigate(Controllers.getDownloadPage()));
        explore.getStyleClass().add("voyage-explore");
        Label chapter = label("CHAPTER 01     /     魔女的旅途", "voyage-chapter");
        VBox copy = new VBox(18, edition, heading, subtitle, explore, chapter);
        copy.setAlignment(Pos.CENTER_LEFT);
        copy.getStyleClass().add("voyage-copy");
        copy.setMaxWidth(410);
        copy.setPickOnBounds(false);
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);

        Label signature = label("THE WANDERING WITCH\nElaina's travel journal  /  01", "voyage-signature");
        StackPane.setAlignment(signature, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(signature, new Insets(0, 22, 20, 0));
        MagicAtmosphere atmosphere = new MagicAtmosphere();
        atmosphere.enabledProperty().bind(SettingsManager.settings().magicEffectsProperty());
        StackPane artwork = new StackPane(atmosphere, copy, signature);
        heading.styleProperty().bind(javafx.beans.binding.Bindings.when(artwork.heightProperty().lessThan(320))
                .then("-fx-font-size: 34px;").otherwise("-fx-font-size: 46px;"));
        copy.spacingProperty().bind(javafx.beans.binding.Bindings.when(artwork.heightProperty().lessThan(320))
                .then(10).otherwise(18));
        chapter.visibleProperty().bind(artwork.heightProperty().greaterThanOrEqualTo(320));
        chapter.managedProperty().bind(chapter.visibleProperty());
        artwork.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                event -> atmosphere.followPointer(event.getX(), event.getY()));
        artwork.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                event -> atmosphere.ripple(event.getX(), event.getY()));
        artwork.getStyleClass().add("voyage-artwork");
        artwork.setMinHeight(200);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(artwork.widthProperty());
        clip.heightProperty().bind(artwork.heightProperty());
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        artwork.setClip(clip);
        artwork.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null && AnimationUtils.isAnimationEnabled()) {
                FadeTransition reveal = new FadeTransition(Duration.millis(700), artwork);
                reveal.setFromValue(0.25);
                reveal.setToValue(1);
                reveal.play();
                TranslateTransition rise = new TranslateTransition(Duration.millis(700), copy);
                rise.setFromY(12);
                rise.setToY(0);
                rise.play();
            }
        });
        BorderPane.setMargin(artwork, new Insets(0, 24, 0, 24));
        setCenter(artwork);

        Label glyph = label("✧", "voyage-world-glyph");
        VBox details = new VBox(5, label("当前世界", "voyage-overline"), gameName, gameDetail);
        HBox.setHgrow(details, Priority.ALWAYS);
        details.setMinWidth(0);
        dock.setAlignment(Pos.CENTER_LEFT);
        dock.getStyleClass().add("voyage-dock");
        dock.getChildren().addAll(glyph, details);
        Label credit = label("OMNIWORLD  ·  Built on HMCL", "voyage-credit");
        HBox status = new HBox(credit);
        status.setAlignment(Pos.CENTER_LEFT);
        status.getStyleClass().add("voyage-status");
        VBox bottom = new VBox(dock, status);
        VBox.setMargin(dock, new Insets(18, 24, 0, 24));
        setBottom(bottom);
    }

    /// Embeds the actual launcher controls into the dock instead of overlaying a second launch area.
    public void setLaunchControls(Node controls) {
        dock.getChildren().add(controls);
    }

    /// Updates the current-world card from the repository's selected instance.
    public void setCurrentGame(@Nullable HMCLGameInstance game) {
        gameName.setText(game == null ? "你的第一段旅途" : game.getId().toString());
        gameDetail.setText(game == null ? "尚未安装游戏 · 从探索中添加一个世界" : "世界已就绪 · 点击右侧开始旅途");
    }

    /// Creates a styled text node.
    private static Label label(String text, String style) {
        Label result = new Label(text);
        result.getStyleClass().add(style);
        return result;
    }

    /// Creates a focusable top-level navigation action.
    private static JFXButton nav(String text, Runnable action) {
        JFXButton button = new JFXButton(text);
        button.getStyleClass().add("voyage-nav");
        button.setOnAction(event -> action.run());
        if (AnimationUtils.isAnimationEnabled()) {
            TranslateTransition hover = new TranslateTransition(Duration.millis(160), button);
            button.setOnMouseEntered(event -> {
                hover.stop();
                hover.setToY(-2);
                hover.play();
            });
            button.setOnMouseExited(event -> {
                hover.stop();
                hover.setToY(0);
                hover.play();
            });
        }
        return button;
    }
}
