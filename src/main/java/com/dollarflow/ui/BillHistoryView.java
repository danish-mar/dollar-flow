package com.dollarflow.ui;

import com.dollarflow.dao.BillDao;
import com.dollarflow.model.Bill;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.RoundingMode;
import java.util.List;

/** Listing of every saved bill, most recent first — searchable and filterable by date range. */
public class BillHistoryView extends VBox {

    private final BillDao dao = new BillDao();
    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final FilteredList<Bill> filteredBills = new FilteredList<>(bills);
    private final TableView<Bill> table = new TableView<>(new SortedList<>(filteredBills));

    private final TextField searchField = new TextField();
    private final DatePicker fromDatePicker = new DatePicker();
    private final DatePicker toDatePicker = new DatePicker();

    public BillHistoryView() {
        getStyleClass().add("view-container");
        setSpacing(16);
        setPadding(new Insets(20));

        Label title = new Label("Bill History");
        title.getStyleClass().add("view-title");

        buildTable();
        wireRowContextMenu();
        table.getStyleClass().add("card");
        VBox.setVgrow(table, Priority.ALWAYS);

        Button deleteButton = new Button("Delete Selected", new FontIcon(Feather.TRASH_2));
        deleteButton.getStyleClass().add("secondary-button");
        deleteButton.setOnAction(e -> deleteSelected());

        HBox actions = new HBox(10, deleteButton);

        getChildren().addAll(title, buildFilterBar(), table, actions);
        refresh();
    }

    public void refresh() {
        bills.setAll(dao.findAll());
    }

    private HBox buildFilterBar() {
        searchField.setPromptText("Search bill no, customer, mobile, yadi no, reference…");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((o, a, b) -> applyFilter());

        fromDatePicker.setPromptText("From date");
        toDatePicker.setPromptText("To date");
        fromDatePicker.valueProperty().addListener((o, a, b) -> applyFilter());
        toDatePicker.valueProperty().addListener((o, a, b) -> applyFilter());

        Button clearButton = new Button("Clear", new FontIcon(Feather.X));
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(e -> {
            searchField.clear();
            fromDatePicker.setValue(null);
            toDatePicker.setValue(null);
        });

        HBox bar = new HBox(10, fieldLabel("Search"), searchField,
                fieldLabel("From"), fromDatePicker, fieldLabel("To"), toDatePicker, clearButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("card");
        return bar;
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        var from = fromDatePicker.getValue();
        var to = toDatePicker.getValue();

        filteredBills.setPredicate(bill -> {
            if (from != null && bill.billDate().isBefore(from)) {
                return false;
            }
            if (to != null && bill.billDate().isAfter(to)) {
                return false;
            }
            if (query.isEmpty()) {
                return true;
            }
            return String.valueOf(bill.billNo()).contains(query)
                    || bill.customerName().toLowerCase().contains(query)
                    || contains(bill.customerMobile(), query)
                    || contains(bill.yadiNumber(), query)
                    || contains(bill.reference(), query);
        });
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void buildTable() {
        TableColumn<Bill, Number> billNoCol = new TableColumn<>("Bill No");
        billNoCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().billNo()));

        TableColumn<Bill, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().billDate().toString()));

        TableColumn<Bill, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));

        TableColumn<Bill, String> mobileCol = new TableColumn<>("Mobile");
        mobileCol.setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().customerMobile())));

        TableColumn<Bill, String> dobCol = new TableColumn<>("Date of Birth");
        dobCol.setCellValueFactory(c -> {
            var dob = c.getValue().customerDob();
            return new SimpleStringProperty(dob == null ? "" : dob.toString());
        });

        TableColumn<Bill, String> yadiCol = new TableColumn<>("Yadi No");
        yadiCol.setCellValueFactory(c -> new SimpleStringProperty(nullToEmpty(c.getValue().yadiNumber())));

        TableColumn<Bill, String> periodCol = new TableColumn<>("Ad Period");
        periodCol.setCellValueFactory(c -> {
            Bill bill = c.getValue();
            String start = bill.adStartDate() == null ? "?" : bill.adStartDate().toString();
            String end = bill.adEndDate() == null ? "?" : bill.adEndDate().toString();
            return new SimpleStringProperty(start + " to " + end);
        });

        TableColumn<Bill, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(c -> {
            Bill bill = c.getValue();
            return new SimpleStringProperty(strip(bill.sizeX()) + " x " + strip(bill.sizeY())
                    + " = " + strip(bill.totalArea()));
        });

        TableColumn<Bill, String> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().rate())));

        TableColumn<Bill, String> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().discount())));

        TableColumn<Bill, String> taxableCol = new TableColumn<>("Taxable Amt");
        taxableCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().finalAmount())));

        TableColumn<Bill, String> gstCol = new TableColumn<>("CGST + SGST");
        gstCol.setCellValueFactory(c -> {
            Bill bill = c.getValue();
            return new SimpleStringProperty(money(bill.cgstAmount().add(bill.sgstAmount()))
                    + " (" + strip(bill.cgstRate()) + "%+" + strip(bill.sgstRate()) + "%)");
        });

        TableColumn<Bill, String> grandTotalCol = new TableColumn<>("Grand Total");
        grandTotalCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().grandTotal())));

        table.getColumns().addAll(List.of(billNoCol, dateCol, nameCol, mobileCol, dobCol, yadiCol, periodCol,
                sizeCol, rateCol, discountCol, taxableCol, gstCol, grandTotalCol));
        table.setPlaceholder(new Label("No bills yet"));
    }

    private void wireRowContextMenu() {
        table.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    table.getSelectionModel().select(row.getIndex());
                    showRowMenu(row.getItem(), e.getScreenX(), e.getScreenY());
                }
            });
            return row;
        });
    }

    private void showRowMenu(Bill bill, double screenX, double screenY) {
        Popup popup = PopupMenu.build(
                new MenuAction("View Bill", () -> viewBill(bill)),
                new MenuAction("Edit Bill", () -> editBill(bill)),
                new MenuAction("Print as PDF", () -> BillActions.exportPdf(bill, getScene().getWindow())),
                new MenuAction("Print", () -> BillActions.printDirect(bill, getScene().getWindow())),
                MenuAction.separator(),
                new MenuAction("Delete Bill", () -> deleteBill(bill)));
        popup.show(table, screenX, screenY);
    }

    private void viewBill(Bill bill) {
        ViewBillDialog dialog = new ViewBillDialog(bill, getScene().getWindow());
        dialog.setOnChanged(this::refresh);
        dialog.show();
    }

    private void editBill(Bill bill) {
        new EditBillDialog(bill, getScene().getWindow(), this::refresh).show();
    }

    private void deleteBill(Bill bill) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete bill #" + bill.billNo() + " for " + bill.customerName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(response -> response == ButtonType.YES).ifPresent(response -> {
            dao.delete(bill.billNo());
            refresh();
        });
    }

    private void deleteSelected() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        deleteBill(selected);
    }

    private String strip(java.math.BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String money(java.math.BigDecimal value) {
        return "₹ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }
}
