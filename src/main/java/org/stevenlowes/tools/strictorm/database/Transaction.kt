package org.stevenlowes.tools.strictorm.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Transaction{
    companion object {
        fun <T> execute(transaction: (Connection) -> T): T {
            val conn = DriverManager.getConnection("jdbc:h2:~/test")
            try {
                val ret = transaction(conn)
                conn.commit()
                return ret
            }
            catch (ex: Exception) {
                conn.rollback()
                throw SQLException("Error when executing transaction", ex)
            }
            finally {
                conn.close()
            }
        }
    }
}