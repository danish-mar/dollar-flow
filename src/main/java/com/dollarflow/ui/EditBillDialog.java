package com.dollarflow.ui;

import com.dollarflow.dao.BillDao;
import com.dollarflow.model.Bill;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Modal editor for an existing bill — same shape as the New Bill form, pre-filled, with Save Changes. */
public class EditBillDialog {

    private final Stage stage = new Stage();
    private final BillDao dao = new BillDao();
    private final Bill original;
    private final Runnable onSaved;

    private final DatePicker billDatePicker = new DatePicker();
    private final TextField customerNameField = new TextField();
    private final TextField referenceField = new TextField();
    private final TextField customerAddressField = new TextField();
    private final TextField customerMobileField = new TextField();
    private final TextField yadiNumberField = new TextField();
    private final DatePicker adStartDatePicker = new DatePicker();
    private final DatePicker adEndDatePicker = new DatePicker();
    private final TextField sizeXField = new TextField();
    private final TextField sizeYField = new TextField();
    private final TextField rateField = new TextField();
    private final TextField discountField = new TextField();
    private final TextField cgstRateField = new TextField();
    private final TextField sgstRateField = new TextField();
    private final DatePicker customerDobField = new DatePicker();

    private final Label grandTotalLabel = new Label("₹ 0.00");

    public EditBillDialog(Bill bill, Window owner, Runnable onSaved) {
        this.original = bill;
        this.onSaved = onSaved;

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Edit Bill #" + bill.billNo());

        VBox card = new VBox(14, buildForm(), buildTotalsRow(), buildActions());
        card.getStyleClass().add("card");

        Label title = new Label("Edit Bill #" + bill.billNo());
        title.getStyleClass().add("view-title");

        VBox root = new VBox(16, title, card);
        root.getStyleClass().add("view-container");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);

        populate(bill);
        wireLiveTotal();
        recomputeGrandTotal();
    }

    public void show() {
        stage.show();
    }

    private void populate(Bill bill) {
        billDatePicker.setValue(bill.billDate());
        customerNameField.setText(bill.customerName());
        referenceField.setText(nullToEmpty(bill.reference()));
        customerAddressField.setText(nullToEmpty(bill.customerAddress()));
        customerMobileField.setText(nullToEmpty(bill.customerMobile()));
        yadiNumberField.setText(nullToEmpty(bill.yadiNumber()));
        adStartDatePicker.setValue(bill.adStartDate());
        adEndDatePicker.setValue(bill.adEndDate());
        sizeXField.setText(strip(bill.sizeX()));
        sizeYField.setText(strip(bill.sizeY()));
        rateField.setText(strip(bill.rate()));
        discountField.setText(strip(bill.discount()));
        cgstRateField.setText(strip(bill.cgstRate()));
        sgstRateField.setText(strip(bill.sgstRate()));
        customerDobField.setValue(bill.customerDob());
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");

        ColumnConstraints labelCol = new ColumnConstraints();
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol, labelCol, fieldCol);

        billDatePicker.setMaxWidth(Double.MAX_VALUE);
        adStartDatePicker.setMaxWidth(Double.MAX_VALUE);
        adEndDatePicker.setMaxWidth(Double.MAX_VALUE);
        customerDobField.setMaxWidth(Double.MAX_VALUE);

        int row = 0;
        grid.addRow(row++, requiredFieldLabel("Bill Date"), billDatePicker, fieldLabel("Reference"), referenceField);
        grid.addRow(row++, requiredFieldLabel("Customer Name"), customerNameField, fieldLabel("Mobile No"), customerMobileField);
        grid.addRow(row++, fieldLabel("Address"), customerAddressField, fieldLabel("Yadi Number"), yadiNumberField);
        grid.addRow(row++, fieldLabel("Ad Start Date"), adStartDatePicker, fieldLabel("Ad End Date"), adEndDatePicker);
        grid.addRow(row++, fieldLabel("Size X"), sizeXField, fieldLabel("Size Y"), sizeYField);
        grid.addRow(row++, fieldLabel("Rate (₹)"), rateField, fieldLabel("Discount (₹)"), discountField);
        grid.addRow(row++, fieldLabel("CGST Rate (%)"), cgstRateField, fieldLabel("SGST Rate (%)"), sgstRateField);
        grid.addRow(row, fieldLabel("Date of Birth"), customerDobField);

        return grid;
    }

    private GridPane buildTotalsRow() {
        grandTotalLabel.getStyleClass().add("grand-total");
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.getStyleClass().add("form-grid");
        grid.addRow(0, fieldLabel("Grand Total"), grandTotalLabel);
        return grid;
    }

    private HBox buildActions() {
        Button saveButton = new Button("Save Changes", new FontIcon(Feather.SAVE));
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(e -> save());
        saveButton.setDefaultButton(true);

        Button cancelButton = new Button("Cancel", new FontIcon(Feather.X));
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> stage.close());

        return new HBox(10, saveButton, cancelButton);
    }

    private void wireLiveTotal() {
        Runnable recompute = this::recomputeGrandTotal;
        sizeXField.textProperty().addListener((o, a, b) -> recompute.run());
        sizeYField.textProperty().addListener((o, a, b) -> recompute.run());
        rateField.textProperty().addListener((o, a, b) -> recompute.run());
        discountField.textProperty().addListener((o, a, b) -> recompute.run());
        cgstRateField.textProperty().addListener((o, a, b) -> recompute.run());
        sgstRateField.textProperty().addListener((o, a, b) -> recompute.run());
    }

    private void recomputeGrandTotal() {
        BigDecimal x = parse(sizeXField.getText());
        BigDecimal y = parse(sizeYField.getText());
        BigDecimal rate = parse(rateField.getText());
        BigDecimal discount = parse(discountField.getText());
        BigDecimal cgstRate = parse(cgstRateField.getText());
        BigDecimal sgstRate = parse(sgstRateField.getText());

        BigDecimal finalAmount = x.multiply(y).multiply(rate).subtract(discount);
        BigDecimal cgstAmount = finalAmount.multiply(cgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = finalAmount.multiply(sgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = finalAmount.add(cgstAmount).add(sgstAmount);

        grandTotalLabel.setText("₹ " + grandTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private void save() {
        String name = customerNameField.getText() == null ? "" : customerNameField.getText().trim();
        if (name.isEmpty()) {
            warn("Customer name is required.");
            return;
        }
        BigDecimal x = parse(sizeXField.getText());
        BigDecimal y = parse(sizeYField.getText());
        BigDecimal rate = parse(rateField.getText());
        if (x.signum() <= 0 || y.signum() <= 0) {
            warn("Size X and Size Y must be greater than zero.");
            return;
        }
        if (rate.signum() <= 0) {
            warn("Rate must be greater than zero.");
            return;
        }
        if (billDatePicker.getValue() == null) {
            warn("Bill date is required.");
            return;
        }

        Bill updated = Bill.compute(
                original.billNo(),
                billDatePicker.getValue(),
                name,
                blankToNull(customerAddressField.getText()),
                blankToNull(customerMobileField.getText()),
                blankToNull(referenceField.getText()),
                blankToNull(yadiNumberField.getText()),
                adStartDatePicker.getValue(),
                adEndDatePicker.getValue(),
                x, y, rate, parse(discountField.getText()),
                parse(cgstRateField.getText()), parse(sgstRateField.getText()), customerDobField.getValue());

        dao.update(updated);
        onSaved.run();
        stage.close();
    }

    private void warn(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
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
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private HBox requiredFieldLabel(String text) {
        Label asterisk = new Label("*");
        asterisk.getStyleClass().add("required-marker");
        return new HBox(2, fieldLabel(text), asterisk);
    }
}
