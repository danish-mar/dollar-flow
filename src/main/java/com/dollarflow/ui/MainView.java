package com.dollarflow.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Root layout, classic style: a single toolbar (brand, File/View/Help menus, and page switcher),
 * a content area, and a status bar. The File/View/Help menus are hand-built on {@link Popup} rather
 * than {@link javafx.scene.control.MenuBar} — MenuBar's dropdown skin has a built-in "grow" open
 * animation with no public switch to disable it; a raw Popup shows and hides instantly, no animation.
 */
public class MainView extends BorderPane {

    private final Label statusLabel = new Label("Ready");
    private Popup openPopup;

    public MainView() {
        getStyleClass().add("app-root");

        BillHistoryView historyView = new BillHistoryView();
        BillFormView formView = new BillFormView();
        SettingsView settingsView = new SettingsView();
        formView.setOnSaved(historyView::refresh);
        settingsView.setOnSaved(formView::refreshRates);

        StackPane content = new StackPane(formView, historyView, settingsView);
        content.getStyleClass().add("content-area");

        ToggleGroup navGroup = new ToggleGroup();
        ToggleButton newBillButton = toolbarToggle("New Bill", Feather.FILE_PLUS, navGroup);
        ToggleButton historyButton = toolbarToggle("Bill History", Feather.LIST, navGroup);
        ToggleButton settingsButton = toolbarToggle("Settings", Feather.SETTINGS, navGroup);

        newBillButton.setOnAction(e -> {
            formView.setVisible(true);
            historyView.setVisible(false);
            settingsView.setVisible(false);
            statusLabel.setText("New Bill");
        });
        historyButton.setOnAction(e -> {
            historyView.refresh();
            historyView.setVisible(true);
            formView.setVisible(false);
            settingsView.setVisible(false);
            statusLabel.setText("Bill History");
        });
        settingsButton.setOnAction(e -> {
            settingsView.setVisible(true);
            formView.setVisible(false);
            historyView.setVisible(false);
            statusLabel.setText("Settings");
        });
        newBillButton.setSelected(true);
        historyView.setVisible(false);
        settingsView.setVisible(false);

        ToolBar toolBar = buildToolBar(newBillButton, historyButton, settingsButton);
        setTop(toolBar);
        setCenter(content);
        setBottom(buildStatusBar());
    }

    /** A top-level menu label that opens a plain, non-animated Popup of item buttons below it. */
    private Button menu(String title, MenuAction... actions) {
        Button menuButton = new Button(title);
        menuButton.getStyleClass().add("xp-menu-button");

        Popup popup = PopupMenu.build(actions);
        popup.setOnHidden(e -> {
            menuButton.getStyleClass().remove("xp-menu-button-open");
            if (openPopup == popup) {
                openPopup = null;
            }
        });

        menuButton.setOnAction(e -> {
            if (openPopup != null) {
                openPopup.hide();
            }
            if (popup.isShowing()) {
                return;
            }
            Bounds bounds = menuButton.localToScreen(menuButton.getBoundsInLocal());
            menuButton.getStyleClass().add("xp-menu-button-open");
            popup.show(menuButton, bounds.getMinX(), bounds.getMaxY());
            openPopup = popup;
        });

        return menuButton;
    }

    private MenuAction menuItem(String label, Runnable handler) {
        return new MenuAction(label, handler);
    }

    private ToolBar buildToolBar(ToggleButton newBillButton, ToggleButton historyButton, ToggleButton settingsButton) {
        Label logoIcon = new Label("₹");
        logoIcon.getStyleClass().add("brand-icon");
        Label brand = new Label("Dollar Flow");
        brand.getStyleClass().add("brand");
        HBox brandBox = new HBox(4, logoIcon, brand);
        brandBox.setPadding(new Insets(0, 12, 0, 4));

        Button fileMenu = menu("File",
                menuItem("Exit", () -> ((Stage) getScene().getWindow()).close()));

        Button viewMenu = menu("View",
                menuItem("New Bill", newBillButton::fire),
                menuItem("Bill History", historyButton::fire),
                menuItem("Settings", settingsButton::fire));

        Button helpMenu = menu("Help",
                menuItem("About Dollar Flow", this::showAbout));

        ToolBar toolBar = new ToolBar(brandBox, new Separator(), fileMenu, viewMenu, helpMenu, new Separator(),
                newBillButton, historyButton, settingsButton);
        toolBar.getStyleClass().add("xp-toolbar");
        return toolBar;
    }

    private HBox buildStatusBar() {
        HBox statusBar = new HBox(statusLabel);
        statusBar.getStyleClass().add("status-bar");
        statusLabel.setText("New Bill");
        return statusBar;
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Dollar Flow");
        alert.setHeaderText("Dollar Flow");
        alert.setContentText("A billing application backed by SQLite.\nVersion 1.0");
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    private ToggleButton toolbarToggle(String text, org.kordamp.ikonli.Ikon icon, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text, new FontIcon(icon));
        button.setToggleGroup(group);
        button.getStyleClass().add("xp-toolbar-button");
        return button;
    }
}
