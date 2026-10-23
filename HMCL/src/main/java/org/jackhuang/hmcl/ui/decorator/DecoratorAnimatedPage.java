/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui.decorator;

import javafx.scene.Node;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Shared desktop workspace for accounts, downloads, instance management and preferences.
@NotNullByDefault
public class DecoratorAnimatedPage extends Control {

    /// Existing navigation controls, kept intact inside the new navigation rail.
    protected final VBox left = new VBox();
    /// Existing page content, retained across tab changes.
    protected final StackPane center = new StackPane();

    {
        getStyleClass().add("voyage-workspace");
    }

    /// Replaces navigation items without rebuilding their bindings or actions.
    protected void setLeft(Node... children) {
        left.getChildren().setAll(children);
    }

    /// Replaces the active workspace content.
    protected void setCenter(Node... children) {
        center.getChildren().setAll(children);
    }

    /// Returns the page navigation container.
    public VBox getLeft() {
        return left;
    }

    /// Returns the page content container.
    public StackPane getCenter() {
        return center;
    }

    /// Installs the common workspace layout.
    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratorAnimatedPageSkin<>(this);
    }

    /// Frames each functional page with a navigation rail and a contextual heading.
    @NotNullByDefault
    public static class DecoratorAnimatedPageSkin<T extends DecoratorAnimatedPage> extends SkinBase<T> {

        /// Creates the workspace; the illustrated home opts out of the inner-page frame.
        protected DecoratorAnimatedPageSkin(T control) {
            super(control);

            BorderPane pane = new BorderPane();
            FXUtils.setLimitWidth(control.left, 200);
            if (control.getStyleClass().contains("voyage-workspace")) {
                Label brand = new Label("✧  OMNIWORLD");
                brand.getStyleClass().add("voyage-rail-brand");
                Label edition = new Label("魔女的旅途 / 旅程手册");
                edition.getStyleClass().add("voyage-rail-caption");
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                Label footer = new Label("✦  每一段旅途，都有魔法。");
                footer.getStyleClass().add("voyage-rail-footer");
                VBox rail = new VBox(8, brand, edition, control.left, spacer, footer);
                rail.getStyleClass().add("voyage-rail");
                VBox.setVgrow(control.left, Priority.ALWAYS);
                pane.setLeft(rail);

                Label title = new Label();
                title.getStyleClass().add("voyage-workspace-title");
                title.setWrapText(true);
                if (control instanceof DecoratorPage page) {
                    title.textProperty().bind(Bindings.createStringBinding(() -> {
                        DecoratorPage.@Nullable State state = page.stateProperty().get();
                        return state == null || state.title() == null ? "旅程手册" : state.title();
                    }, page.stateProperty()));
                }
                Label eyebrow = new Label("THE WITCH'S JOURNEY  /  魔法工坊");
                eyebrow.getStyleClass().add("voyage-workspace-eyebrow");
                VBox heading = new VBox(5, eyebrow, title);
                HBox.setHgrow(heading, Priority.ALWAYS);
                Label seal = new Label("✧");
                seal.getStyleClass().add("voyage-workspace-seal");
                HBox banner = new HBox(16, heading, seal);
                banner.setAlignment(Pos.CENTER_LEFT);
                banner.getStyleClass().add("voyage-workspace-banner");
                BorderPane body = new BorderPane(control.center);
                body.setTop(banner);
                body.getStyleClass().add("voyage-workspace-body");
                control.center.getStyleClass().add("voyage-workspace-content");
                pane.setCenter(body);
            } else {
                pane.setLeft(control.left);
                pane.setCenter(control.center);
            }
            getChildren().setAll(pane);
        }

        /// Replaces the navigation of the owning control.
        protected void setLeft(Node... children) {
            getSkinnable().setLeft(children);
        }

        /// Replaces the content of the owning control.
        protected void setCenter(Node... children) {
            getSkinnable().setCenter(children);
        }

    }

}
