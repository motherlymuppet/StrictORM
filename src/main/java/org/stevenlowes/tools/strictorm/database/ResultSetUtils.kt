package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.QueryReader
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.dao.toDaoClass
import java.math.BigDecimal
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

fun <T : Dao> ResultSet.readList(constructor: KFunction<T>, columns: List<QueryReader.Column>): List<T> {
    use {
        return buildSequence {
            while (next()) {
                val params = getParams(columns).zip(constructor.parameters)
                val castParams = params.map { (value, type) ->
                    when(type.type){
                        LocalDate::class.starProjectedType -> (value as Date).toLocalDate()
                        LocalTime::class.starProjectedType -> (value as Time).toLocalTime()
                        LocalDateTime::class.starProjectedType -> (value as Timestamp).toLocalDateTime()
                            else -> value
                    }
                }.toTypedArray()
                yield(constructor.call(*castParams))
            }
        }.toList()
    }
}

private fun ResultSet.getParams(columns: List<QueryReader.Column>): List<Any?> {
    return columns.map { column ->
        val obj: Any? = column.getObject(this)
        //TODO date objects come through as dates not localdatetime
        if (obj == null && !wasNull()) {
            throw DaoException("Column not in query: $column")
        }
        return@map obj
    }
}