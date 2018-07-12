package org.stevenlowes.tools.strictorm.extensions

import com.healthmarketscience.sqlbuilder.*
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.exceptions.DaoException
import org.stevenlowes.tools.strictorm.dao.ParseTree
import org.stevenlowes.tools.strictorm.database.Transaction
import java.sql.PreparedStatement

fun <T : Dao> SelectQuery.executeQuery(preparer: QueryPreparer?, parseTree: ParseTree<T>): List<T> {
    return Transaction.execute { conn ->
        val sql = validate().toString()
        println(sql)
        val stmt = conn.prepareStatement(sql)
        preparer?.setStaticValues(stmt)
        val rs = stmt.executeQuery()
        parseTree.parse(rs)
    }
}

fun InsertQuery.executeInsert(preparer: QueryPreparer? = null): Int {
    var id: Int = -1
    val sql = validate().toString()

    Transaction.execute { conn ->
        val stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
        preparer?.setStaticValues(stmt)
        stmt.execute()
        val keys = stmt.generatedKeys
        keys.next()
        id = keys.getInt(1)
    }

    if (id == -1) {
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