package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.QueryReader
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KFunction

fun <T: Dao> ResultSet.readList(constructor: KFunction<T>, columns: List<QueryReader.Column>): List<T> {
    use {
        return buildSequence {
            while (next()) {
                yield(constructor.call(*getParams(columns)))
            }
        }.toList()
    }
}

private fun ResultSet.getParams(columns: List<QueryReader.Column>): Array<Any?> {
    return columns.map {
        val obj: Any? = it.getObject(this)
        if(obj == null && !wasNull()){
            throw DaoException("Column not in query: $it")
        }
        return@map obj
    }.toTypedArray()
}