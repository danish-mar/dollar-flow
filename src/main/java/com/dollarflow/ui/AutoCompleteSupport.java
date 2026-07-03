package com.dollarflow.ui;

import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Popup;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Attaches a plain, non-animated suggestion dropdown to a TextField, filtered as the user types. */
final class AutoCompleteSupport {

    private AutoCompleteSupport() {
    }

    static void attach(TextField field, Supplier<List<String>> candidatesSupplier) {
        attach(field, candidatesSupplier, value -> {
        });
    }

    /** Same as {@link #attach(TextField, Supplier)} but also fires {@code onSelected} once a value is chosen —
     * either by clicking a suggestion, or by leaving the field with text that matches a known value exactly. */
    static void attach(TextField field, Supplier<List<String>> candidatesSupplier, Consumer<String> onSelected) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        ListView<String> listView = new ListView<>();
        listView.getStyleClass().add("xp-suggestion-list");
        listView.setPrefHeight(120);
        listView.setPrefWidth(240);
        popup.getContent().add(listView);

        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                field.setText(selected);
                field.positionCaret(selected.length());
                popup.hide();
                onSelected.accept(selected);
            }
        });

        field.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isBlank()) {
                popup.hide();
                return;
            }
            List<String> matches = candidatesSupplier.get().stream()
                    .filter(c -> c.toLowerCase().contains(newText.toLowerCase()))
                    .limit(8)
                    .toList();
            if (matches.isEmpty()) {
                popup.hide();
                return;
            }
            listView.getItems().setAll(matches);
            if (!popup.isShowing()) {
                Bounds bounds = field.localToScreen(field.getBoundsInLocal());
                popup.show(field, bounds.getMinX(), bounds.getMaxY());
            }
        });

        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                popup.hide();
                String text = field.getText();
                if (text != null && !text.isBlank() && candidatesSupplier.get().contains(text)) {
                    onSelected.accept(text);
                }
            }
        });
    }
}
