package org.stevenlowes.tools.strictorm.dao.utils

import com.healthmarketscience.sqlbuilder.dbspec.Column
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

fun <T: Dao> ResultSet.readObject(clazz: KClass<T>, columns: List<Column>): T {
    return readList(clazz, columns).first()
}

fun <T: Dao> ResultSet.readList(clazz: KClass<T>, columns: List<Column>): List<T> {
    val constructor = clazz.primaryConstructor ?: throw DaoException("DAO does not have primary constructor")
    return buildSequence {
        while(next()){
            yield(constructor.call(*getParams(columns)))
        }
    }.toList()
}

private fun ResultSet.getParams(columns: List<Column>): Array<Any> {
    return columns.map {
        getObject(it.columnNameSQL)
    }.toTypedArray()
}