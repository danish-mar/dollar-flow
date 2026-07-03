package com.dollarflow.ui;

import com.dollarflow.dao.SettingsDao;
import com.dollarflow.model.Bill;
import com.dollarflow.model.Settings;
import com.dollarflow.pdf.BillPdfExporter;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

/** Shared PDF-export and direct-print actions for a bill, usable from any dialog or menu. */
final class BillActions {

    private static final SettingsDao SETTINGS_DAO = new SettingsDao();

    private BillActions() {
    }

    static void exportPdf(Bill bill, Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Bill as PDF");
        chooser.setInitialFileName("Bill-" + bill.billNo() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        try {
            BillPdfExporter.export(bill, SETTINGS_DAO.get(), target);
            new Alert(Alert.AlertType.INFORMATION, "Saved to " + target.getName(), ButtonType.OK).showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not save PDF: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    static void printDirect(Bill bill, Window owner) {
        Settings settings = SETTINGS_DAO.get();
        BillReceiptView receipt = new BillReceiptView(bill, settings);
        Scene offscreenScene = new Scene(receipt);
        offscreenScene.getStylesheets().add(BillActions.class.getResource("/css/app.css").toExternalForm());
        receipt.applyCss();
        receipt.layout();
        BillPrinter.print(receipt, owner);
    }
}
