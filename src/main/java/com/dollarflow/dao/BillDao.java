package com.dollarflow.dao;

import com.dollarflow.db.Database;
import com.dollarflow.model.Bill;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class BillDao {

    private static final int MIN_BILL_NO = 10_000;
    private static final int MAX_BILL_NO = 99_999;
    private final Random random = new Random();

    /** A random 5-digit bill number not already in use; shown to the user before they submit. */
    public int generateBillNumber() {
        int candidate;
        do {
            candidate = MIN_BILL_NO + random.nextInt(MAX_BILL_NO - MIN_BILL_NO + 1);
        } while (exists(candidate));
        return candidate;
    }

    private boolean exists(int billNo) {
        String sql = "SELECT 1 FROM bills WHERE bill_no = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, billNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check bill number", e);
        }
    }

    public Bill insert(Bill bill) {
        String sql = """
                INSERT INTO bills (bill_no, bill_date, customer_name, customer_address, customer_mobile, reference,
                                    yadi_number, ad_start_date, ad_end_date, size_x, size_y, total_area,
                                    rate, total_payable, discount, final_amount,
                                    cgst_rate, cgst_amount, sgst_rate, sgst_amount, grand_total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, bill.billNo());
            ps.setString(2, bill.billDate().toString());
            ps.setString(3, bill.customerName());
            ps.setString(4, bill.customerAddress());
            ps.setString(5, bill.customerMobile());
            ps.setString(6, bill.reference());
            ps.setString(7, bill.yadiNumber());
            ps.setString(8, bill.adStartDate() == null ? null : bill.adStartDate().toString());
            ps.setString(9, bill.adEndDate() == null ? null : bill.adEndDate().toString());
            ps.setDouble(10, bill.sizeX().doubleValue());
            ps.setDouble(11, bill.sizeY().doubleValue());
            ps.setDouble(12, bill.totalArea().doubleValue());
            ps.setDouble(13, bill.rate().doubleValue());
            ps.setDouble(14, bill.totalPayable().doubleValue());
            ps.setDouble(15, bill.discount().doubleValue());
            ps.setDouble(16, bill.finalAmount().doubleValue());
            ps.setDouble(17, bill.cgstRate().doubleValue());
            ps.setDouble(18, bill.cgstAmount().doubleValue());
            ps.setDouble(19, bill.sgstRate().doubleValue());
            ps.setDouble(20, bill.sgstAmount().doubleValue());
            ps.setDouble(21, bill.grandTotal().doubleValue());
            ps.executeUpdate();
            return bill;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save bill", e);
        }
    }

    public void update(Bill bill) {
        String sql = """
                UPDATE bills SET bill_date = ?, customer_name = ?, customer_address = ?, customer_mobile = ?,
                                  reference = ?, yadi_number = ?, ad_start_date = ?, ad_end_date = ?,
                                  size_x = ?, size_y = ?, total_area = ?, rate = ?, total_payable = ?,
                                  discount = ?, final_amount = ?, cgst_rate = ?, cgst_amount = ?,
                                  sgst_rate = ?, sgst_amount = ?, grand_total = ?
                WHERE bill_no = ?
                """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, bill.billDate().toString());
            ps.setString(2, bill.customerName());
            ps.setString(3, bill.customerAddress());
            ps.setString(4, bill.customerMobile());
            ps.setString(5, bill.reference());
            ps.setString(6, bill.yadiNumber());
            ps.setString(7, bill.adStartDate() == null ? null : bill.adStartDate().toString());
            ps.setString(8, bill.adEndDate() == null ? null : bill.adEndDate().toString());
            ps.setDouble(9, bill.sizeX().doubleValue());
            ps.setDouble(10, bill.sizeY().doubleValue());
            ps.setDouble(11, bill.totalArea().doubleValue());
            ps.setDouble(12, bill.rate().doubleValue());
            ps.setDouble(13, bill.totalPayable().doubleValue());
            ps.setDouble(14, bill.discount().doubleValue());
            ps.setDouble(15, bill.finalAmount().doubleValue());
            ps.setDouble(16, bill.cgstRate().doubleValue());
            ps.setDouble(17, bill.cgstAmount().doubleValue());
            ps.setDouble(18, bill.sgstRate().doubleValue());
            ps.setDouble(19, bill.sgstAmount().doubleValue());
            ps.setDouble(20, bill.grandTotal().doubleValue());
            ps.setInt(21, bill.billNo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update bill", e);
        }
    }

    public List<Bill> findAll() {
        String sql = "SELECT * FROM bills ORDER BY bill_no DESC";
        List<Bill> bills = new ArrayList<>();
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                bills.add(map(rs));
            }
            return bills;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load bills", e);
        }
    }

    public void delete(int billNo) {
        String sql = "DELETE FROM bills WHERE bill_no = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, billNo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete bill", e);
        }
    }

    /** Distinct customer names typed so far, most recent first, for autocomplete. */
    public List<String> distinctCustomerNames() {
        return distinctValues("customer_name");
    }

    /** Distinct reference values typed so far, most recent first, for autocomplete. */
    public List<String> distinctReferences() {
        return distinctValues("reference");
    }

    /** Address/mobile/reference from this customer's most recent bill, for fast-entry autofill. */
    public Optional<CustomerInfo> findLatestCustomerInfo(String customerName) {
        String sql = """
                SELECT customer_address, customer_mobile, reference FROM bills
                WHERE customer_name = ? ORDER BY bill_no DESC LIMIT 1
                """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, customerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CustomerInfo(
                        rs.getString("customer_address"),
                        rs.getString("customer_mobile"),
                        rs.getString("reference")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not look up customer info", e);
        }
    }

    private List<String> distinctValues(String column) {
        String sql = "SELECT " + column + " AS v FROM bills WHERE " + column + " IS NOT NULL AND " + column
                + " != '' ORDER BY bill_no DESC";
        Set<String> values = new LinkedHashSet<>();
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString("v"));
            }
            return new ArrayList<>(values);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load suggestions for " + column, e);
        }
    }

    private Bill map(ResultSet rs) throws SQLException {
        return new Bill(
                rs.getInt("bill_no"),
                LocalDate.parse(rs.getString("bill_date")),
                rs.getString("customer_name"),
                rs.getString("customer_address"),
                rs.getString("customer_mobile"),
                rs.getString("reference"),
                rs.getString("yadi_number"),
                parseDate(rs.getString("ad_start_date")),
                parseDate(rs.getString("ad_end_date")),
                BigDecimal.valueOf(rs.getDouble("size_x")),
                BigDecimal.valueOf(rs.getDouble("size_y")),
                BigDecimal.valueOf(rs.getDouble("total_area")),
                BigDecimal.valueOf(rs.getDouble("rate")),
                BigDecimal.valueOf(rs.getDouble("total_payable")),
                BigDecimal.valueOf(rs.getDouble("discount")),
                BigDecimal.valueOf(rs.getDouble("final_amount")),
                BigDecimal.valueOf(rs.getDouble("cgst_rate")),
                BigDecimal.valueOf(rs.getDouble("cgst_amount")),
                BigDecimal.valueOf(rs.getDouble("sgst_rate")),
                BigDecimal.valueOf(rs.getDouble("sgst_amount")),
                BigDecimal.valueOf(rs.getDouble("grand_total"))
        );
    }

    private LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    public record CustomerInfo(String address, String mobile, String reference) {
    }
}
