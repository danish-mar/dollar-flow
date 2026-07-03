package com.dollarflow.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** A single advertisement hoarding bill. Totals are computed once at creation and stored as-is. */
public record Bill(
        int billNo,
        LocalDate billDate,
        String customerName,
        String customerAddress,
        String customerMobile,
        String reference,
        String yadiNumber,
        LocalDate adStartDate,
        LocalDate adEndDate,
        BigDecimal sizeX,
        BigDecimal sizeY,
        BigDecimal totalArea,
        BigDecimal rate,
        BigDecimal totalPayable,
        BigDecimal discount,
        BigDecimal finalAmount,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal grandTotal
) {

    /**
     * Builds a Bill from raw entry fields, computing area, payable amount, taxable (final) amount,
     * CGST/SGST amounts at the given rates, and the tax-inclusive grand total.
     */
    public static Bill compute(int billNo, LocalDate billDate, String customerName, String customerAddress,
                                String customerMobile, String reference, String yadiNumber,
                                LocalDate adStartDate, LocalDate adEndDate,
                                BigDecimal sizeX, BigDecimal sizeY, BigDecimal rate, BigDecimal discount,
                                BigDecimal cgstRate, BigDecimal sgstRate) {
        BigDecimal totalArea = sizeX.multiply(sizeY);
        BigDecimal totalPayable = totalArea.multiply(rate);
        BigDecimal finalAmount = totalPayable.subtract(discount);
        BigDecimal cgstAmount = finalAmount.multiply(cgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = finalAmount.multiply(sgstRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = finalAmount.add(cgstAmount).add(sgstAmount);
        return new Bill(billNo, billDate, customerName, customerAddress, customerMobile, reference, yadiNumber,
                adStartDate, adEndDate, sizeX, sizeY, totalArea, rate, totalPayable, discount, finalAmount,
                cgstRate, cgstAmount, sgstRate, sgstAmount, grandTotal);
    }
}
