package org.jackhuang.hmcl.ui.main;

import com.jfoenix.controls.JFXButton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.decorator.DecoratorPage;
import org.jetbrains.annotations.NotNullByDefault;

/// Dedicated Omniworld settings for effects and launch behavior, with access to all advanced settings.
@NotNullByDefault
public final class OmniworldSettingsPage extends ScrollPane implements DecoratorPage {
    /// Title and navigation behavior of this settings screen.
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle("Omniworld 设置"));

    /// Builds immediately persisted visual and launch controls.
    public OmniworldSettingsPage() {
        setFitToWidth(true);
        getStyleClass().add("voyage-settings-scroll");
        var preferences = SettingsManager.settings();
        Label title = text("让魔法，按你的方式发生。", "voyage-settings-title");
        Label hint = text("外观与启动偏好会自动保存。", "voyage-settings-caption");
        CheckBox effects = new CheckBox("开启魔法特效");
        effects.selectedProperty().bindBidirectional(preferences.magicEffectsProperty());
        effects.getStyleClass().add("voyage-preference-toggle");

        ComboBox<String> frames = new ComboBox<>();
        frames.getItems().setAll("跟随屏幕刷新率", "60 FPS", "120 FPS", "144 FPS");
        frames.setValue(switch (preferences.effectsFrameRateProperty().get()) {
            case 60 -> "60 FPS";
            case 120 -> "120 FPS";
            case 144 -> "144 FPS";
            default -> "跟随屏幕刷新率";
        });
        frames.setOnAction(event -> preferences.effectsFrameRateProperty().set(switch (frames.getSelectionModel().getSelectedIndex()) {
            case 1 -> 60;
            case 2 -> 120;
            case 3 -> 144;
            default -> 0;
        }));
        frames.disableProperty().bind(effects.selectedProperty().not());
        Slider intensity = new Slider(20, 100, Math.max(20, Math.min(100, preferences.effectsIntensityProperty().get())));
        intensity.setPrefWidth(180);
        intensity.setShowTickMarks(true);
        intensity.setMajorTickUnit(40);
        intensity.setBlockIncrement(10);
        intensity.valueProperty().addListener((observable, oldValue, newValue) ->
                preferences.effectsIntensityProperty().set(newValue.intValue()));
        intensity.disableProperty().bind(effects.selectedProperty().not());
        VBox appearance = card("01  /  魔法与流光", effects,
                row("特效帧率", "默认跟随屏幕；实际帧率取决于显示器与设备性能。", frames),
                row("星尘浓度", "调节漂浮星尘与流星的数量。", intensity));
        Label motion = text("系统或高级设置启用“减少动画”时，魔法动效会暂停。", "voyage-settings-caption");
        motion.setWrapText(true);
        appearance.getChildren().add(motion);
        CheckBox sounds = new CheckBox("轻柔点击音效");
        sounds.selectedProperty().bindBidirectional(preferences.clickSoundsProperty());
        sounds.getStyleClass().add("voyage-preference-toggle");
        CheckBox cursor = new CheckBox("可爱猫咪鼠标");
        cursor.selectedProperty().bindBidirectional(preferences.cuteCursorProperty());
        cursor.getStyleClass().add("voyage-preference-toggle");
        appearance.getChildren().addAll(sounds, cursor);

        CheckBox keep = new CheckBox("游戏启动后保持启动器可见");
        keep.selectedProperty().bindBidirectional(preferences.keepLauncherOpenProperty());
        keep.getStyleClass().add("voyage-preference-toggle");
        Label keepHint = text("开启后覆盖游戏配置中的隐藏或关闭行为；关闭后遵循每个游戏的设置。", "voyage-settings-caption");
        keepHint.setWrapText(true);
        VBox launch = card("02  /  启动行为", keep, keepHint);
        JFXButton advanced = new JFXButton("打开高级设置  →");
        advanced.getStyleClass().add("voyage-nav");
        advanced.setOnAction(event -> Controllers.navigate(Controllers.getSettingsPage()));
        VBox more = card("03  /  更多选项", row("游戏、Java 与下载", "内存分配、网络、主题以及其他启动器设置。", advanced));
        VBox content = new VBox(14, title, hint, appearance, launch, more);
        content.getStyleClass().add("voyage-settings-content");
        setContent(content);
    }

    /// Returns the page title and standard back behavior.
    @Override
    public ReadOnlyObjectProperty<State> stateProperty() { return state; }

    /// Creates one titled preference card.
    private static VBox card(String title, Node... children) {
        VBox result = new VBox(17);
        result.getStyleClass().add("voyage-preference-card");
        result.getChildren().add(text(title, "voyage-preference-heading"));
        result.getChildren().addAll(children);
        return result;
    }

    /// Places a wrapping explanation beside a fixed-width setting control.
    private static HBox row(String title, String description, Node control) {
        Label explanation = text(description, "voyage-settings-caption");
        explanation.setWrapText(true);
        VBox words = new VBox(5, text(title, "voyage-preference-label"), explanation);
        words.setMinWidth(0);
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox result = new HBox(24, words, control);
        result.setAlignment(Pos.CENTER_LEFT);
        return result;
    }

    /// Creates a CSS-styled settings label.
    private static Label text(String value, String style) {
        Label result = new Label(value);
        result.getStyleClass().add(style);
        return result;
    }
}
