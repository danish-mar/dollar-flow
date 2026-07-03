package com.dollarflow.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Builds a plain, non-animated dropdown of action buttons on {@link Popup} — used for both the
 * toolbar's File/View/Help menus and right-click row context menus. A raw Popup shows and hides
 * instantly with no built-in animation, unlike {@link javafx.scene.control.ContextMenu}/
 * {@link javafx.scene.control.Menu}, whose skins have a hardcoded "grow" open animation.
 */
final class PopupMenu {

    private PopupMenu() {
    }

    static Popup build(MenuAction... actions) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox box = new VBox();
        box.getStyleClass().add("xp-menu-popup");

        for (MenuAction action : actions) {
            if (action.isSeparator()) {
                box.getChildren().add(new Separator());
                continue;
            }
            Button item = new Button(action.label());
            item.getStyleClass().add("xp-menu-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnAction(e -> {
                popup.hide();
                action.handler().run();
            });
            box.getChildren().add(item);
        }

        popup.getContent().add(box);
        return popup;
    }
}
