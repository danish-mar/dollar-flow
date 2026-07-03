package com.dollarflow.ui;

import com.dollarflow.dao.SettingsDao;
import com.dollarflow.model.Bill;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/** Modal, read-only view of one bill with Edit / Print / Export-PDF actions. */
public class ViewBillDialog {

    private final Stage stage = new Stage();
    private Runnable onChanged = () -> {
    };

    public ViewBillDialog(Bill bill, Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Bill #" + bill.billNo());

        BillReceiptView receipt = new BillReceiptView(bill, new SettingsDao().get());

        Button editButton = new Button("Edit Bill", new FontIcon(Feather.EDIT_2));
        editButton.getStyleClass().add("secondary-button");
        editButton.setOnAction(e -> {
            stage.close();
            new EditBillDialog(bill, owner, onChanged).show();
        });

        Button printPdfButton = new Button("Print as PDF", new FontIcon(Feather.FILE_TEXT));
        printPdfButton.getStyleClass().add("secondary-button");
        printPdfButton.setOnAction(e -> BillActions.exportPdf(bill, stage));

        Button printButton = new Button("Print", new FontIcon(Feather.PRINTER));
        printButton.getStyleClass().add("primary-button");
        printButton.setOnAction(e -> BillPrinter.print(receipt, stage));

        Button closeButton = new Button("Close", new FontIcon(Feather.X));
        closeButton.getStyleClass().add("secondary-button");
        closeButton.setOnAction(e -> stage.close());

        HBox actions = new HBox(10, editButton, printPdfButton, printButton, closeButton);
        actions.setPadding(new Insets(0, 20, 20, 20));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("view-container");
        root.setCenter(receipt);
        root.setBottom(actions);
        BorderPane.setMargin(actions, new Insets(10, 0, 0, 0));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setScene(scene);
    }

    /** Called after a successful Edit save, so the caller (Bill History) can refresh its table. */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public void show() {
        stage.show();
    }
}
