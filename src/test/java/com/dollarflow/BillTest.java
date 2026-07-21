package com.dollarflow;

import com.dollarflow.model.Bill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillTest {

    @Test
    void computesAreaPayableFinalAndGrandTotal() {
        Bill bill = Bill.compute(
                1,
                LocalDate.of(2026, 1, 1),
                "Acme Corp",
                "123 Market St",
                "9999999999",
                "Walk-in",
                "YADI-42",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("15"),
                new BigDecimal("500"),
                new BigDecimal("9"),
                new BigDecimal("9"),
                LocalDate.of(1990, 5, 15));

        assertEquals(new BigDecimal("200"), bill.totalArea());
        assertEquals(new BigDecimal("3000"), bill.totalPayable());
        assertEquals(new BigDecimal("2500"), bill.finalAmount());
        assertEquals(0, new BigDecimal("225.00").compareTo(bill.cgstAmount()));
        assertEquals(0, new BigDecimal("225.00").compareTo(bill.sgstAmount()));
        assertEquals(0, new BigDecimal("2950.00").compareTo(bill.grandTotal()));
    }
}
