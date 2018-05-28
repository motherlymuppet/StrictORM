package org.stevenlowes.tools.strictorm.dao.utils

import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.dbspec.Column
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

fun <T: Dao> ResultSet.readObject(constructor: KFunction<T>, columns: List<QueryReader.Column>): T {
    return readList(constructor, columns).first()
}

fun <T: Dao> ResultSet.readList(constructor: KFunction<T>, columns: List<QueryReader.Column>): List<T> {
    return buildSequence {
        while(next()){
            yield(constructor.call(*getParams(columns)))
        }
    }.toList()
}

private fun ResultSet.getParams(columns: List<QueryReader.Column>): Array<Any> {
    return columns.map {
        it.getObject(this)
    }.toTypedArray()
}

//TODO use QueryReader