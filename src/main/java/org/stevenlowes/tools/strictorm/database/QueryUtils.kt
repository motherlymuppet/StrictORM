package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.*
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.PreparedStatement
import kotlin.reflect.KFunction

fun <T : Dao> SelectQuery.executeQuery(preparer: QueryPreparer?,
                                                             constructor: KFunction<T>,
                                                             columns: List<QueryReader.Column>): List<T> {
    return Transaction.execute { conn ->
        val sql = validate().toString()
        val stmt = conn.prepareStatement(sql)
        preparer?.setStaticValues(stmt)
        val rs = stmt.executeQuery()
        rs.readList(constructor, columns)
    }
}

fun InsertQuery.executeInsert(preparer: QueryPreparer? = null): Long {
    var id: Long = -1
    val sql = validate().toString()

    Transaction.execute { conn ->
        val stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        preparer?.setStaticValues(stmt)
        stmt.execute()
        val keys = stmt.generatedKeys
        keys.next()
        id = keys.getLong(1)
    }

    if (id == -1L) {
        throw DaoException("Unable to load id of inserted object - $sql")
    }
    else {
        return id
    }
}

fun Query<*>.execute(preparer: QueryPreparer? = null) {
    Transaction.execute { conn ->
        val sql = validate().toString()
        val stmt = conn.prepareStatement(sql)
        preparer?.setStaticValues(stmt)
        stmt.execute()
    }
}