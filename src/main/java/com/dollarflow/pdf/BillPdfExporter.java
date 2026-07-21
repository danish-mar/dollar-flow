package com.dollarflow.pdf;

import com.dollarflow.model.Bill;
import com.dollarflow.model.Settings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Renders a single bill as a one-page PDF using PDFBox. */
public final class BillPdfExporter {

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();

    private BillPdfExporter() {
    }

    public static void export(Bill bill, Settings settings, File target) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                y = writeLine(cs, PDType1Font.HELVETICA_BOLD, 16,
                        settings.companyName().isBlank() ? "Dollar Flow" : settings.companyName(), y);
                if (!settings.companyAddress().isBlank()) {
                    for (String line : settings.companyAddress().split("\n")) {
                        y = writeLine(cs, PDType1Font.HELVETICA, 10, line, y);
                    }
                }
                y -= 10;
                y = writeLine(cs, PDType1Font.HELVETICA_BOLD, 13, "BILL", y);
                y -= 6;

                y = writeRow(cs, "Bill No", "#" + bill.billNo(), "Bill Date", bill.billDate().toString(), y);
                y -= 10;

                y = writeLine(cs, PDType1Font.HELVETICA_BOLD, 11, "Customer", y);
                y = writeLine(cs, PDType1Font.HELVETICA, 10, bill.customerName(), y);
                if (bill.customerAddress() != null) {
                    y = writeLine(cs, PDType1Font.HELVETICA, 10, bill.customerAddress(), y);
                }
                if (bill.customerMobile() != null) {
                    y = writeLine(cs, PDType1Font.HELVETICA, 10, "Mobile: " + bill.customerMobile(), y);
                }
                if (bill.customerDob() != null) {
                    y = writeLine(cs, PDType1Font.HELVETICA, 10, "Date of Birth: " + bill.customerDob(), y);
                }
                if (bill.reference() != null) {
                    y = writeLine(cs, PDType1Font.HELVETICA, 10, "Reference: " + bill.reference(), y);
                }
                y -= 10;

                if (bill.yadiNumber() != null) {
                    y = writeLine(cs, PDType1Font.HELVETICA, 10, "Yadi Number: " + bill.yadiNumber(), y);
                }
                String start = bill.adStartDate() == null ? "?" : bill.adStartDate().toString();
                String end = bill.adEndDate() == null ? "?" : bill.adEndDate().toString();
                y = writeLine(cs, PDType1Font.HELVETICA, 10, "Ad Period: " + start + " to " + end, y);
                y -= 14;

                y = writeRow(cs, "Size", strip(bill.sizeX()) + " x " + strip(bill.sizeY())
                        + " = " + strip(bill.totalArea()), "Rate", money(bill.rate()), y);
                y = writeRow(cs, "Total Payable", money(bill.totalPayable()), "Discount", money(bill.discount()), y);
                y = writeRow(cs, "Taxable Amount", money(bill.finalAmount()),
                        "CGST (" + strip(bill.cgstRate()) + "%)", money(bill.cgstAmount()), y);
                y = writeRow(cs, "SGST (" + strip(bill.sgstRate()) + "%)", money(bill.sgstAmount()), "", "", y);
                y -= 10;

                writeLine(cs, PDType1Font.HELVETICA_BOLD, 14, "Grand Total: " + money(bill.grandTotal()), y);
            }

            document.save(target);
        }
    }

    private static float writeLine(PDPageContentStream cs, PDType1Font font, float size, String text, float y)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(text);
        cs.endText();
        return y - (size + 6);
    }

    private static float writeRow(PDPageContentStream cs, String label1, String value1, String label2,
                                   String value2, float y) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label1 + ": ");
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.showText(value1);
        cs.endText();

        if (!label2.isEmpty()) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            cs.newLineAtOffset(PAGE_WIDTH / 2, y);
            cs.showText(label2 + ": ");
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.showText(value2);
            cs.endText();
        }

        return y - 16;
    }

    private static String strip(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String money(BigDecimal value) {
        return "Rs. " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
