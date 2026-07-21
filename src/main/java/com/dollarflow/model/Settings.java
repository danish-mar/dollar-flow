package com.dollarflow.model;

import java.math.BigDecimal;

/** Company profile, default GST rates, and bill numbering applied to new bills. */
public record Settings(String companyName, String companyAddress, BigDecimal cgstRate, BigDecimal sgstRate,
                        int startingBillNo) {
}
