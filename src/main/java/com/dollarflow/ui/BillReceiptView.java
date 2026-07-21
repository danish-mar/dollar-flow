package com.dollarflow.ui;

import com.dollarflow.model.Bill;
import com.dollarflow.model.Settings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Read-only, print-friendly layout of a single bill — used by the View dialog and by printing. */
public class BillReceiptView extends VBox {

    public BillReceiptView(Bill bill, Settings settings) {
        getStyleClass().add("receipt");
        setSpacing(10);
        setPadding(new Insets(24));
        setPrefWidth(520);

        getChildren().addAll(buildHeader(settings), buildBillMeta(bill), new Separator(),
                buildCustomerBlock(bill), buildAdBlock(bill), new Separator(),
                buildTotalsBlock(bill));
    }

    private VBox buildHeader(Settings settings) {
        Label companyName = new Label(
                settings.companyName().isBlank() ? "Dollar Flow" : settings.companyName());
        companyName.getStyleClass().add("receipt-company-name");

        VBox header = new VBox(2, companyName);
        if (!settings.companyAddress().isBlank()) {
            Label address = new Label(settings.companyAddress());
            address.getStyleClass().add("receipt-company-address");
            address.setWrapText(true);
            header.getChildren().add(address);
        }
        return header;
    }

    private GridPane buildBillMeta(Bill bill) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.addRow(0, label("Bill No", true), label("#" + bill.billNo(), false),
                label("Bill Date", true), label(bill.billDate().toString(), false));
        return grid;
    }

    private GridPane buildCustomerBlock(Bill bill) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        int row = 0;
        grid.addRow(row++, label("Customer", true), label(bill.customerName(), false));
        if (bill.customerAddress() != null) {
            grid.addRow(row++, label("Address", true), label(bill.customerAddress(), false));
        }
        if (bill.customerMobile() != null) {
            grid.addRow(row++, label("Mobile", true), label(bill.customerMobile(), false));
        }
        if (bill.customerDob() != null) {
            grid.addRow(row++, label("Date of Birth", true), label(bill.customerDob().toString(), false));
        }
        if (bill.reference() != null) {
            grid.addRow(row, label("Reference", true), label(bill.reference(), false));
        }
        return grid;
    }

    private GridPane buildAdBlock(Bill bill) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        int row = 0;
        if (bill.yadiNumber() != null) {
            grid.addRow(row++, label("Yadi Number", true), label(bill.yadiNumber(), false));
        }
        String start = bill.adStartDate() == null ? "—" : bill.adStartDate().toString();
        String end = bill.adEndDate() == null ? "—" : bill.adEndDate().toString();
        grid.addRow(row, label("Ad Period", true), label(start + " to " + end, false));
        return grid;
    }

    private GridPane buildTotalsBlock(Bill bill) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        int row = 0;
        grid.addRow(row++, label("Size", true),
                label(strip(bill.sizeX()) + " x " + strip(bill.sizeY()) + " = " + strip(bill.totalArea()), false));
        grid.addRow(row++, label("Rate", true), label(money(bill.rate()), false));
        grid.addRow(row++, label("Total Payable", true), label(money(bill.totalPayable()), false));
        grid.addRow(row++, label("Discount", true), label(money(bill.discount()), false));
        grid.addRow(row++, label("Taxable Amount", true), label(money(bill.finalAmount()), false));
        grid.addRow(row++, label("CGST (" + strip(bill.cgstRate()) + "%)", true), label(money(bill.cgstAmount()), false));
        grid.addRow(row++, label("SGST (" + strip(bill.sgstRate()) + "%)", true), label(money(bill.sgstAmount()), false));

        Label grandTotalLabel = label("Grand Total", true);
        grandTotalLabel.getStyleClass().add("receipt-grand-total-label");
        Label grandTotalValue = label(money(bill.grandTotal()), false);
        grandTotalValue.getStyleClass().add("receipt-grand-total-value");
        grid.addRow(row, grandTotalLabel, grandTotalValue);

        return grid;
    }

    private Label label(String text, boolean muted) {
        Label label = new Label(text);
        label.getStyleClass().add(muted ? "receipt-field-label" : "receipt-field-value");
        return label;
    }

    private String strip(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String money(BigDecimal value) {
        return "₹ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
