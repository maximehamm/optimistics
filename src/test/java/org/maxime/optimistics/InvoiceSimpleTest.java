package org.maxime.optimistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;

@SuppressWarnings("SqlResolve")
public class InvoiceSimpleTest {

    private Connection con = null;

    @Test
    public void insertInvoicesSimple() throws SQLException {

        assertInvoiceAmount("F001", 1000.0);

        con.createStatement().executeUpdate(
        "UPDATE Invoice\n" +
            "SET amount = 2000.0\n" +
            "WHERE id = 'F001'");
        con.commit();

        assertInvoiceAmount("F001", 2000.0);
    }

    @Test
    public void insertInvoicesConcurent() throws SQLException {

        assertInvoiceAmount("F001", 1000.0);

        con.createStatement().executeUpdate(
                "UPDATE Invoice\n" +
                    "SET amount = 2000.0\n" +
                    "WHERE id = 'F001'");
        con.commit();

        assertInvoiceAmount("F001", 2000.0);
    }



    @BeforeEach
    void initDB() throws SQLException {

        con = DriverManager.getConnection( "jdbc:hsqldb:file:/Users/Maxime/Temp/db/Optimistics", "maxime", "maxime");

        con.createStatement().executeUpdate(
                "DROP TABLE Invoice");

        con.createStatement().executeUpdate(
                "CREATE TABLE Invoice (\n" +
                        "   id VARCHAR(32),\n" +
                        "   amount DECIMAL NOT NULL,\n" +
                        "   version INT,\n" +
                        "   PRIMARY KEY (id) \n" +
                        ")");

        con.createStatement().executeUpdate(
                "INSERT INTO Invoice (id, amount)\n" +
                        "VALUES ('F001', 1000.0);");

        con.commit();

        assertInvoicesCount(1);
    }


    private void assertInvoicesCount(int count) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet r = stmt.executeQuery("SELECT count(*) as count from Invoice;");
        r.next();
        assertEquals(count, r.getInt("count"));
    }

    private void assertInvoiceAmount(String invoiceId, double amount) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet r = stmt.executeQuery("SELECT amount FROM Invoice WHERE id = '" + invoiceId + "'");
        r.next();
        assertEquals(amount, r.getBigDecimal("amount").doubleValue());
    }
}
