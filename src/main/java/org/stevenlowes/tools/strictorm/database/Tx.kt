package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.*
import com.healthmarketscience.sqlbuilder.dbspec.Column
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.dao.utils.readObject
import java.sql.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

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

        fun execute(sql: String, preparer: QueryPreparer?){
            execute { conn ->
                val stmt = conn.prepareStatement(sql)
                preparer?.setStaticValues(stmt)
                stmt.execute()
            }
        }

        fun executeInsert(sql: String, preparer: QueryPreparer?): Long {
            var id: Long = -1

            execute { conn ->
                val stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
                preparer?.setStaticValues(stmt)
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

        fun <T> executeQuery(sql: String, preparer: QueryPreparer?, parser: (ResultSet) -> T, params: List<Any?> = emptyList()): T{
            return execute { conn ->
                val stmt = conn.prepareStatement(sql)
                preparer?.setStaticValues(stmt)
                val rs = stmt.executeQuery()
                return@execute parser(rs)
            }
        }
    }
}

fun DeleteQuery.execute(preparer: QueryPreparer?){
    Tx.execute(validate().toString(), preparer)
}

fun <T: Dao> SelectQuery.execute(preparer: QueryPreparer?, constructor: KFunction<T>, columns: List<QueryReader.Column>): List<T>{
    return Tx.executeQuery(validate().toString(), preparer, {
        return@executeQuery buildSequence {
            while(it.next()){
                yield(it.readObject(constructor, columns))
            }
        }.toList()
    })
}

fun InsertQuery.execute(preparer: QueryPreparer?): Long{
    return Tx.executeInsert(validate().toString(), preparer)
}

fun UpdateQuery.execute(preparer: QueryPreparer?){
    Tx.execute(validate().toString(), preparer)
}

fun CreateTableQuery.execute(){
    Tx.execute(validate().toString(), null)
}