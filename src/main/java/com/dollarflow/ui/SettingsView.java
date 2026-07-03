package com.dollarflow.ui;

import com.dollarflow.dao.SettingsDao;
import com.dollarflow.model.Settings;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;

/** Company profile and default GST rates applied to every new bill. */
public class SettingsView extends VBox {

    private final SettingsDao dao = new SettingsDao();
    private Runnable onSaved = () -> {
    };

    private final TextField companyNameField = new TextField();
    private final TextArea companyAddressField = new TextArea();
    private final TextField cgstRateField = new TextField();
    private final TextField sgstRateField = new TextField();

    public SettingsView() {
        getStyleClass().add("view-container");
        setSpacing(16);
        setPadding(new Insets(20));

        Label title = new Label("Settings");
        title.getStyleClass().add("view-title");

        VBox card = new VBox(14, buildForm(), buildActions());
        card.getStyleClass().add("card");

        getChildren().addAll(title, card);
        loadSettings();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");

        ColumnConstraints labelCol = new ColumnConstraints();
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        companyNameField.setPromptText("Your company name");
        companyAddressField.setPromptText("Your company address");
        companyAddressField.setPrefRowCount(3);
        companyAddressField.setWrapText(true);
        cgstRateField.setPromptText("9");
        sgstRateField.setPromptText("9");

        int row = 0;
        grid.addRow(row++, fieldLabel("Company Name"), companyNameField);
        grid.addRow(row++, fieldLabel("Company Address"), companyAddressField);
        grid.addRow(row++, fieldLabel("CGST Rate (%)"), cgstRateField);
        grid.addRow(row, fieldLabel("SGST Rate (%)"), sgstRateField);

        return grid;
    }

    private javafx.scene.layout.HBox buildActions() {
        Button saveButton = new Button("Save Settings", new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> save());
        return new javafx.scene.layout.HBox(10, saveButton);
    }

    private void loadSettings() {
        Settings settings = dao.get();
        companyNameField.setText(settings.companyName());
        companyAddressField.setText(settings.companyAddress());
        cgstRateField.setText(strip(settings.cgstRate()));
        sgstRateField.setText(strip(settings.sgstRate()));
    }

    private void save() {
        BigDecimal cgst = parse(cgstRateField.getText());
        BigDecimal sgst = parse(sgstRateField.getText());
        if (cgst.signum() < 0 || sgst.signum() < 0) {
            new Alert(Alert.AlertType.WARNING, "GST rates cannot be negative.", ButtonType.OK).showAndWait();
            return;
        }

        Settings settings = new Settings(
                companyNameField.getText() == null ? "" : companyNameField.getText().trim(),
                companyAddressField.getText() == null ? "" : companyAddressField.getText().trim(),
                cgst, sgst);
        dao.save(settings);
        onSaved.run();

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings saved.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String strip(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }
}
