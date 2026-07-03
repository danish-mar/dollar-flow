package com.dollarflow.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Owns the single SQLite connection and schema bootstrap for the app. */
public final class Database {

    private static final String DB_FILE = "dollarflow.db";
    private static Connection connection;

    private Database() {
    }

    public static synchronized Connection get() {
        if (connection == null) {
            connection = open();
            initSchema();
        }
        return connection;
    }

    private static Connection open() {
        try {
            Path dataDir = dataDirectory();
            Files.createDirectories(dataDir);
            String url = "jdbc:sqlite:" + dataDir.resolve(DB_FILE);
            Connection conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return conn;
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Could not open SQLite database", e);
        }
    }

    /** %APPDATA%\DollarFlow on Windows; ~/.dollarflow elsewhere (e.g. local dev on Linux/macOS). */
    private static Path dataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "DollarFlow");
        }
        return Path.of(System.getProperty("user.home"), ".dollarflow");
    }

    private static void initSchema() {
        String bills = """
                CREATE TABLE IF NOT EXISTS bills (
                    bill_no INTEGER PRIMARY KEY,
                    bill_date TEXT NOT NULL,
                    customer_name TEXT NOT NULL,
                    customer_address TEXT,
                    customer_mobile TEXT,
                    reference TEXT,
                    yadi_number TEXT,
                    ad_start_date TEXT,
                    ad_end_date TEXT,
                    size_x REAL NOT NULL,
                    size_y REAL NOT NULL,
                    total_area REAL NOT NULL,
                    rate REAL NOT NULL,
                    total_payable REAL NOT NULL,
                    discount REAL NOT NULL DEFAULT 0,
                    final_amount REAL NOT NULL,
                    cgst_rate REAL NOT NULL DEFAULT 0,
                    cgst_amount REAL NOT NULL DEFAULT 0,
                    sgst_rate REAL NOT NULL DEFAULT 0,
                    sgst_amount REAL NOT NULL DEFAULT 0,
                    grand_total REAL NOT NULL DEFAULT 0
                )
                """;

        String settings = """
                CREATE TABLE IF NOT EXISTS settings (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    company_name TEXT NOT NULL DEFAULT '',
                    company_address TEXT NOT NULL DEFAULT '',
                    cgst_rate REAL NOT NULL DEFAULT 9,
                    sgst_rate REAL NOT NULL DEFAULT 9
                )
                """;

        String seedSettings = """
                INSERT OR IGNORE INTO settings (id, company_name, company_address, cgst_rate, sgst_rate)
                VALUES (1, '', '', 9, 9)
                """;

        try (Statement st = get0().createStatement()) {
            st.execute(bills);
            st.execute(settings);
            st.execute(seedSettings);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize schema", e);
        }

        // Additive migration for dev databases created before GST columns existed.
        addColumnIfMissing("bills", "cgst_rate", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("bills", "cgst_amount", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("bills", "sgst_rate", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("bills", "sgst_amount", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing("bills", "grand_total", "REAL NOT NULL DEFAULT 0");
    }

    private static void addColumnIfMissing(String table, String column, String definition) {
        try (Statement st = get0().createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException e) {
            // Column already exists — fine, nothing to do.
        }
    }

    private static Connection get0() {
        return connection;
    }
}
