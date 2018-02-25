package org.stevenlowes.tools.strictorm.database

import org.stevenlowes.tools.strictorm.dao.DaoException
import java.math.BigDecimal
import java.sql.*
import java.sql.Date
import java.time.*
import java.util.*

/**
 * A class for simplifying running queries on a database.
 *
 *
 * By running queries in a transaction, we ensure that when something goes wrong everything gets rolled back cleanly.
 *
 * @param <T> What you want to return from the database. Use Void (capital V) or boolean for update/delete.
</T> */
class Tx<T> @Throws(SQLException::class)
private constructor(private val transaction: (Connection) -> T) {
    private val connection: Connection = DriverManager.getConnection("jdbc:h2:~/test")

    init {
        connection.autoCommit = false
    }

    /**
     * Run the transaction. Handle committing the transaction / rolling it back, and closing the connection.
     *
     * @return The value returned by the transaction supplied to execute
     *
     * @throws SQLException transaction supplied to execute threw an error.
     */
    @Throws(SQLException::class)
    private fun fire(): T {
        try {
            val ret = transaction(connection)
            connection.commit()
            return ret
        } catch (ex: Exception) {
            connection.rollback()
            throw SQLException("Error when executing transaction", ex)
        } finally {
            connection.close()
        }
    }

    /**
     * Basically just a supplier that throws SQLException.
     */
    interface RowParser<T> {
        @Throws(SQLException::class)
        operator fun get(resultSet: ResultSet): T?
    }

    companion object {

        /**
         * WARNING [Tx]
         *
         *
         * Supply some code to run in a transaction. Handles creating a connection, committing and rolling back the transaction (if necessary), closing the connection, and protects the database from any
         * errors.
         *
         *
         * Look out for **TxHandler Warnings**. These are in the documentation to warn you that a method uses a transaction. Do not call these methods from within a [Transaction]. The method will
         * not be safe to run and will not be cleanly rolled back in the case of an error.
         *
         * @param transaction The code to run. The output of this is the return value of this method.
         * @param <T>         The type that is returned from the database. Use Void (capital V) to return nothing.
         *
         * @return The value returned by transaction
         *
         * @throws SQLException An error was thrown when running the transaction.
        </T> */
        @Throws(SQLException::class)
        fun <T> execute(transaction: (Connection) -> T): T {
            return Tx(transaction).fire()
        }

        fun execute(sql: String, params: List<Any?> = emptyList()){
            execute { conn ->
                val stmt = conn.prepareStatement(sql)
                fillParams(stmt, params)
                stmt.execute()
            }
        }

        fun executeInsert(sql: String, params: List<Any?> = emptyList()): Long {
            var id: Long = -1

            execute { conn ->
                val stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
                fillParams(stmt, params)
                stmt.execute()
                val keys = stmt.generatedKeys
                keys.next()
                id = keys.getLong(1)
            }

            if(id == -1L){
                throw DaoException("Unable to load id of inserted object - $sql")
            }
            else{
                return id
            }

        }

        fun <T> executeQuery(sql: String, parser: (ResultSet) -> T, params: List<Any?> = emptyList()): T{
            return execute { conn ->
                val stmt = conn.prepareStatement(sql)
                fillParams(stmt, params)
                val rs = stmt.executeQuery()
                return@execute parser(rs)
            }
        }

        fun fillParams(stmt: PreparedStatement, params: List<Any?> = emptyList()){
            params.withIndex().forEach {(index, value) ->
                //Remember that SQL is 1-indexed
                fillParam(stmt, index + 1, value)
            }
        }

        private fun fillParam(stmt: PreparedStatement, index: Int, value: Any?) {
            if(value == null){
                val type = when (value) {
                    is String -> Types.LONGVARCHAR
                    is Long -> Types.BIGINT
                    is Int -> Types.INTEGER
                    is Boolean -> Types.BOOLEAN
                    is LocalDate -> Types.DATE
                    is Double -> Types.DOUBLE
                    is Float -> Types.FLOAT
                    is LocalTime -> Types.TIME
                    is LocalDateTime -> Types.TIMESTAMP
                    else -> {
                        throw DaoException("Unable to parse the type of $value")
                    }
                }
                stmt.setNull(index, type)
            }
            else{
                when(value){
                    is String -> stmt.setString(index, value)
                    is Long -> stmt.setLong(index, value)
                    is Int -> stmt.setInt(index, value)
                    is Boolean -> stmt.setBoolean(index, value)
                    is LocalDate -> stmt.setDate(index, Date.valueOf(value))
                    is Double -> stmt.setDouble(index, value)
                    is Float -> stmt.setFloat(index, value)
                    is LocalTime -> stmt.setTime(index, Time(value.hour, value.minute, value.second))
                    is LocalDateTime -> stmt.setTimestamp(index, Timestamp(value.year, value.monthValue, value.dayOfMonth, value.hour, value.minute, value.second, value.nano))
                    else -> {
                        throw DaoException("Unable to parse the type of $value")
                    }
                }
            }
        }

        /**
         * Parse all rows from result set and combine into a list.
         *
         * @param rs        The resultSet to parse.
         * @param rowParser A transaction which parses one row of the resultSet and returns the object represented in that row. Null values are filtered from the list. This transaction should not call
         * rs.next().
         * @param <T>       The type of object represented in each row of the RS.
         *
         * @return The list of all objects represented by the RS.
         *
         * @throws SQLException Something went wrong with your row parser code when retrieving values from the RS.
        </T> */
        @Throws(SQLException::class)
        fun <T> parseRS(rs: ResultSet, rowParser: RowParser<T>): List<T> {
            val list = ArrayList<T>()
            while (rs.next()) {
                val elem = rowParser[rs]
                if (elem != null) {
                    list.add(elem)
                }
            }
            return list
        }
    }
}