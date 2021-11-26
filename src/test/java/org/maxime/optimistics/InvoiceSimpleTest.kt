package org.maxime.optimistics

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

@Suppress("SqlResolve")
class InvoiceSimpleTest {

    @Test
    fun insertAlone() {

        assertInvoiceAmount("F001", 1000.0)

        val con = startConnection();
        con.createStatement().executeUpdate("""
                UPDATE Invoice
                SET amount = 2000.0
                WHERE id = 'F001'"""
        )
        con.commit()

        assertInvoiceAmount("F001", 2000.0)
    }

    @Test
    fun insertConcurrencyLastOneWins() {

        assertInvoiceAmount("F001", 1000.0)

        runBlocking {

            launch {

                //
                // JOE
                //
                val con = startConnection(autoCommit = false)
                val amount = getInvoiceAmout("F001", con)
                println("# JOE Starts connection : amount = $amount")
                delay(1000L)

                println("# JOE Sets amount to 6666")
                con.createStatement().executeUpdate("""
                    UPDATE Invoice
                    SET amount = 6666.0
                    WHERE id = 'F001'""")
                con.commit()

                println("# JOE Commit OK")
            }

            launch {

                //
                // WILLY
                //
                delay(500L)
                val con = startConnection(autoCommit = false)
                val amount = getInvoiceAmout("F001", con)
                println("# WILLY Starts connection : amount = $amount")

                println("# WILLY Sets amount to 9999")
                con.createStatement().executeUpdate("""
                    UPDATE Invoice
                    SET amount = 9999.0
                    WHERE id = 'F001'""")
                con.commit()

                println("# WILLY Commit OK")
            }
        }


        val amount = getInvoiceAmout("F001")
        println("# Finally : amount = $amount")

        //# JOE Starts connection : amount = 1000.0
        //# WILLY Starts connection : amount = 1000.0
        //# WILLY Sets amount to 9999
        //# WILLY Commit OK
        //# JOE Sets amount to 6666
        //# JOE Commit OK

        assertInvoiceAmount("F001", 6666.0)
    }

    @Test
    fun insertConcurrencyOptimisticsLock() {

        assertInvoiceAmount("F001", 1000.0)
        assertInvoiceVersion("F001", 13)

        var exception : ConcurrentModificationException? = null

        runBlocking {

            launch {

                //
                // JOE
                //
                val amount = getInvoiceAmout("F001")
                val version = getInvoiceVersion("F001")
                println("# JOE Starts connection : amount = $amount, version = $version")
                delay(1000L)

                println("# JOE Sets amount to 6666")
                val con = startConnection(autoCommit = false)
                val rows = con.createStatement().executeUpdate("""
                    UPDATE Invoice
                    SET amount = 6666.0, version = version + 1
                    WHERE id = 'F001'
                    AND version = $version
                    """)

                if (rows == 0) {
                    println("# JOE LOSTS !!!")
                    exception = ConcurrentModificationException("JOE LOSTS !")
                    con.rollback()
                    println("# JOE Rollback OK")
                    return@launch
                }

                con.commit()
                println("# JOE Commit OK")
            }

            launch {

                //
                // WILLY
                //
                delay(500L)
                val amount = getInvoiceAmout("F001")
                val version = getInvoiceVersion("F001")
                println("# WILLY Starts connection : amount = $amount, version = $version")

                println("# WILLY Sets amount to 9999")
                val con = startConnection(autoCommit = false)
                getInvoiceVersion("F001", con)
                val rows = con.createStatement().executeUpdate("""
                    UPDATE Invoice
                    SET amount = 9999.0, version = version + 1
                    WHERE id = 'F001' 
                    AND version = $version
                    """)

                if (rows == 0) {
                    println("# WILLY LOSTS !!!")
                    exception = ConcurrentModificationException("WILLY LOSTS !")
                    con.rollback()
                    println("# WILLY Rollback OK")
                    return@launch
                }

                con.commit()
                println("# WILLY Commit OK")
            }
        }

        val amount = getInvoiceAmout("F001")
        println("# Finally amount is $amount")

        //# JOE Starts connection : amount = 1000.0, version = 13
        //# WILLY Starts connection : amount = 1000.0, version = 13
        //# WILLY Sets amount to 9999
        //# WILLY Commit OK
        //# JOE Sets amount to 6666
        //# JOE LOSTS !!!
        //# Finally amount is 9999.0

        assertInvoiceAmount("F001", 9999.0)
    }


    @BeforeEach
    fun initDB() {

        val con = startConnection(autoCommit = false);

        con.createStatement().executeUpdate(
            "DROP TABLE Invoice"
        )
        con.createStatement().executeUpdate("""
               CREATE TABLE Invoice (
                   id VARCHAR(32),
                   amount DECIMAL NOT NULL,
                   version INTEGER,
                   PRIMARY KEY (id))"""
        )
        con.createStatement().executeUpdate("""
            INSERT INTO Invoice (id, amount, version)
            VALUES ('F001', 1000.0, 13);"""
        )
        con.commit()
        assertInvoicesCount(1)
    }




    private fun startConnection(autoCommit : Boolean = true) : Connection {
        val con = DriverManager.getConnection("jdbc:hsqldb:file:/Users/Maxime/Temp/db/Optimistics", "maxime", "maxime")
        con.autoCommit = autoCommit;
        return con
    }

    private fun getInvoiceAmout(invoiceId: String, con: Connection = startConnection()): Double {
        val stmt = con.createStatement()
        val r = stmt.executeQuery("SELECT amount FROM Invoice WHERE id = '$invoiceId'")
        r.next()
        return r.getBigDecimal("amount").toDouble()
    }

    private fun getInvoiceVersion(invoiceId: String, con: Connection = startConnection()): Int {
        val stmt = con.createStatement()
        val r = stmt.executeQuery("SELECT version FROM Invoice WHERE id = '$invoiceId'")
        r.next()
        return r.getInt("version")
    }

    private fun assertInvoiceAmount(invoiceId: String, amount: Double, con: Connection = startConnection(),) {
        assertEquals(amount, getInvoiceAmout(invoiceId, con))
    }

    private fun assertInvoiceVersion(invoiceId: String, verson: Int, con: Connection = startConnection(), ) {
        assertEquals(verson, getInvoiceVersion(invoiceId, con))
    }

    private fun assertInvoicesCount(count: Int, con: Connection = startConnection()) {
        val stmt = con.createStatement()
        val r = stmt.executeQuery("SELECT count(*) as count from Invoice;")
        r.next()
        assertEquals(count, r.getInt("count"))
    }
}