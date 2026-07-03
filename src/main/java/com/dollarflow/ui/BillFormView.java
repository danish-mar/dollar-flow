package com.dollarflow.ui;

import com.dollarflow.dao.BillDao;
import com.dollarflow.dao.SettingsDao;
import com.dollarflow.model.Bill;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Fast, single-form bill entry: fill fields, hit Save & Next, form resets ready for the next bill. */
public class BillFormView extends VBox {

    private final BillDao dao = new BillDao();
    private final SettingsDao settingsDao = new SettingsDao();
    private Runnable onSaved = () -> {
    };

    private final Label billNoLabel = new Label();
    private final Label billDateLabel = new Label();

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

    private final Label totalAreaLabel = new Label("0");
    private final Label totalPayableLabel = new Label("₹ 0.00");
    private final Label cgstLabel = new Label("CGST (0%)");
    private final Label cgstAmountLabel = new Label("₹ 0.00");
    private final Label sgstLabel = new Label("SGST (0%)");
    private final Label sgstAmountLabel = new Label("₹ 0.00");
    private final Label grandTotalLabel = new Label("₹ 0.00");

    private BigDecimal cgstRate = BigDecimal.ZERO;
    private BigDecimal sgstRate = BigDecimal.ZERO;
    private int currentBillNo;

    public BillFormView() {
        getStyleClass().add("view-container");
        setSpacing(16);
        setPadding(new Insets(20));

        Label title = new Label("New Bill");
        title.getStyleClass().add("view-title");

        VBox card = new VBox(14, buildIdRow(), buildForm(), buildTotalsRow(), buildActions());
        card.getStyleClass().add("card");

        getChildren().addAll(title, card);

        AutoCompleteSupport.attach(customerNameField, dao::distinctCustomerNames, this::autofillFromCustomer);
        AutoCompleteSupport.attach(referenceField, dao::distinctReferences);

        wireLiveTotals();
        resetForm();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    /** Reloads CGST/SGST rates from Settings — call after Settings are saved so new bills pick them up. */
    public void refreshRates() {
        loadRates();
        recomputeTotals();
    }

    private void loadRates() {
        var settings = settingsDao.get();
        cgstRate = settings.cgstRate();
        sgstRate = settings.sgstRate();
        cgstLabel.setText("CGST (" + strip(cgstRate) + "%)");
        sgstLabel.setText("SGST (" + strip(sgstRate) + "%)");
    }

    private void autofillFromCustomer(String customerName) {
        dao.findLatestCustomerInfo(customerName).ifPresent(info -> {
            if (customerAddressField.getText().isBlank() && info.address() != null) {
                customerAddressField.setText(info.address());
            }
            if (customerMobileField.getText().isBlank() && info.mobile() != null) {
                customerMobileField.setText(info.mobile());
            }
            if (referenceField.getText().isBlank() && info.reference() != null) {
                referenceField.setText(info.reference());
            }
        });
    }

    private GridPane buildIdRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.getStyleClass().add("form-grid");
        billNoLabel.getStyleClass().add("card-title");
        billDateLabel.getStyleClass().add("card-title");
        grid.addRow(0, fieldLabel("Bill No"), billNoLabel, fieldLabel("Bill Date"), billDateLabel);
        return grid;
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

        customerNameField.setPromptText("Customer name");
        referenceField.setPromptText("Referred by");
        customerAddressField.setPromptText("Customer address");
        customerMobileField.setPromptText("Mobile no.");
        yadiNumberField.setPromptText("Yadi number");
        sizeXField.setPromptText("Width");
        sizeYField.setPromptText("Height");
        rateField.setPromptText("Rate per sq. unit (₹)");
        discountField.setPromptText("0");
        adStartDatePicker.setPromptText("Start date");
        adEndDatePicker.setPromptText("End date");
        adStartDatePicker.setMaxWidth(Double.MAX_VALUE);
        adEndDatePicker.setMaxWidth(Double.MAX_VALUE);

        int row = 0;
        grid.addRow(row++, fieldLabel("Customer Name"), customerNameField, fieldLabel("Reference"), referenceField);
        grid.addRow(row++, fieldLabel("Address"), customerAddressField, fieldLabel("Mobile No"), customerMobileField);
        grid.addRow(row++, fieldLabel("Yadi Number"), yadiNumberField, fieldLabel("Ad Start Date"), adStartDatePicker);
        grid.addRow(row++, fieldLabel("Ad End Date"), adEndDatePicker, fieldLabel("Size X"), sizeXField);
        grid.addRow(row, fieldLabel("Size Y"), sizeYField, fieldLabel("Rate (₹)"), rateField);
        grid.addRow(row + 1, fieldLabel("Discount (₹)"), discountField);

        return grid;
    }

    private GridPane buildTotalsRow() {
        totalAreaLabel.getStyleClass().add("draft-total");
        totalPayableLabel.getStyleClass().add("draft-total");
        cgstAmountLabel.getStyleClass().add("draft-total");
        sgstAmountLabel.getStyleClass().add("draft-total");
        grandTotalLabel.getStyleClass().add("grand-total");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(6);
        grid.getStyleClass().add("form-grid");
        grid.addRow(0, fieldLabel("Total (X×Y)"), totalAreaLabel, fieldLabel("Total Payable"), totalPayableLabel);
        grid.addRow(1, cgstLabel, cgstAmountLabel, sgstLabel, sgstAmountLabel);
        grid.addRow(2, fieldLabel("Grand Total"), grandTotalLabel);
        return grid;
    }

    private javafx.scene.layout.HBox buildActions() {
        Button saveNextButton = new Button("Save && Next Bill", new FontIcon(Feather.ARROW_RIGHT_CIRCLE));
        saveNextButton.getStyleClass().add("primary-button");
        saveNextButton.setOnAction(e -> saveAndNext());
        saveNextButton.setDefaultButton(true);

        Button clearButton = new Button("Clear", new FontIcon(Feather.X));
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(e -> resetForm());

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, saveNextButton, clearButton);
        return actions;
    }

    private void wireLiveTotals() {
        Runnable recompute = this::recomputeTotals;
        sizeXField.textProperty().addListener((o, a, b) -> recompute.run());
        sizeYField.textProperty().addListener((o, a, b) -> recompute.run());
        rateField.textProperty().addListener((o, a, b) -> recompute.run());
        discountField.textProperty().addListener((o, a, b) -> recompute.run());

        discountField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                saveAndNext();
            }
        });
    }

    private void recomputeTotals() {
        BigDecimal x = parse(sizeXField.getText());
        BigDecimal y = parse(sizeYField.getText());
        BigDecimal rate = parse(rateField.getText());
        BigDecimal discount = parse(discountField.getText());

        BigDecimal totalArea = x.multiply(y);
        BigDecimal totalPayable = totalArea.multiply(rate);
        BigDecimal finalAmount = totalPayable.subtract(discount);
        BigDecimal cgstAmount = finalAmount.multiply(cgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = finalAmount.multiply(sgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = finalAmount.add(cgstAmount).add(sgstAmount);

        totalAreaLabel.setText(strip(totalArea));
        totalPayableLabel.setText(money(totalPayable));
        cgstAmountLabel.setText(money(cgstAmount));
        sgstAmountLabel.setText(money(sgstAmount));
        grandTotalLabel.setText(money(grandTotal));
    }

    private void saveAndNext() {
        String name = customerNameField.getText() == null ? "" : customerNameField.getText().trim();
        if (name.isEmpty()) {
            warn("Customer name is required.");
            customerNameField.requestFocus();
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
            rateField.requestFocus();
            return;
        }
        BigDecimal discount = parse(discountField.getText());

        Bill bill = Bill.compute(
                currentBillNo,
                LocalDate.now(),
                name,
                blankToNull(customerAddressField.getText()),
                blankToNull(customerMobileField.getText()),
                blankToNull(referenceField.getText()),
                blankToNull(yadiNumberField.getText()),
                adStartDatePicker.getValue(),
                adEndDatePicker.getValue(),
                x, y, rate, discount, cgstRate, sgstRate);

        dao.insert(bill);
        onSaved.run();
        resetForm();
    }

    private void resetForm() {
        loadRates();
        currentBillNo = dao.generateBillNumber();
        billNoLabel.setText(String.valueOf(currentBillNo));
        billDateLabel.setText(LocalDate.now().toString());

        customerNameField.clear();
        referenceField.clear();
        customerAddressField.clear();
        customerMobileField.clear();
        yadiNumberField.clear();
        adStartDatePicker.setValue(null);
        adEndDatePicker.setValue(null);
        sizeXField.clear();
        sizeYField.clear();
        rateField.clear();
        discountField.clear();

        recomputeTotals();
        customerNameField.requestFocus();
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

    private String money(BigDecimal value) {
        return "₹ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String blankToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }
}
