package com.dollarflow.ui;

import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Sends a bill receipt straight to a printer via JavaFX's built-in printing support. */
final class BillPrinter {

    private BillPrinter() {
    }

    static void print(BillReceiptView receipt, Window owner) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            new Alert(Alert.AlertType.WARNING, "No printer found. Connect or configure a printer and try again.",
                    ButtonType.OK).showAndWait();
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            new Alert(Alert.AlertType.ERROR, "Could not start a print job.", ButtonType.OK).showAndWait();
            return;
        }

        if (!job.showPrintDialog(owner)) {
            return;
        }

        PageLayout layout = job.getPrinter().createPageLayout(
                Paper.A4, javafx.print.PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        boolean printed = job.printPage(layout, receipt);
        if (printed) {
            job.endJob();
        } else {
            new Alert(Alert.AlertType.ERROR, "Printing failed.", ButtonType.OK).showAndWait();
        }
    }
}
