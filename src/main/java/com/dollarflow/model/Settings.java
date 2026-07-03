package com.dollarflow.model;

import java.math.BigDecimal;

/** Company profile and default GST rates applied to new bills. */
public record Settings(String companyName, String companyAddress, BigDecimal cgstRate, BigDecimal sgstRate) {
}
