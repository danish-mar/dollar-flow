package com.dollarflow.dao;

import com.dollarflow.db.Database;
import com.dollarflow.model.Settings;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SettingsDao {

    public Settings get() {
        String sql = "SELECT company_name, company_address, cgst_rate, sgst_rate FROM settings WHERE id = 1";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                return new Settings("", "", BigDecimal.valueOf(9), BigDecimal.valueOf(9));
            }
            return new Settings(
                    rs.getString("company_name"),
                    rs.getString("company_address"),
                    BigDecimal.valueOf(rs.getDouble("cgst_rate")),
                    BigDecimal.valueOf(rs.getDouble("sgst_rate"))
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load settings", e);
        }
    }

    public void save(Settings settings) {
        String sql = """
                UPDATE settings
                SET company_name = ?, company_address = ?, cgst_rate = ?, sgst_rate = ?
                WHERE id = 1
                """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, settings.companyName());
            ps.setString(2, settings.companyAddress());
            ps.setDouble(3, settings.cgstRate().doubleValue());
            ps.setDouble(4, settings.sgstRate().doubleValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save settings", e);
        }
    }
}
