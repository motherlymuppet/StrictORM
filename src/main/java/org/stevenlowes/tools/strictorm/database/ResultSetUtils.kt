package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.QueryReader
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KFunction
import kotlin.reflect.full.starProjectedType

class ParseTree<T : Dao>(private val constructor: KFunction<T>,
                         private val columns: Map<QueryReader.Column, Int>,
                         private val children: Map<ParseTree<*>, Int>) {
    fun parse(rs: ResultSet): List<T>{
        return buildSequence {
            rs.use {
                while (rs.next()) {
                    yield(parseOnce(rs))
                }
            }
        }.toList()
    }

    private fun parseOnce(rs: ResultSet): T {
        val paramMap = mutableMapOf<Any?, Int>()

        paramMap.putAll(columns.mapKeys { (column, pos) ->
            val obj: Any? = column.getObject(rs)
            when {
                rs.wasNull() -> return@mapKeys null
                obj == null -> throw DaoException("Column not in query: $column")
                else -> return@mapKeys obj to pos
            }
        })

        paramMap.putAll(children.mapKeys { (column, pos) ->
            column.parseOnce(rs) to pos
        })

        val params = paramMap.toList().sortedBy { it.second }.map{it.first}
        return constructor.call(*params.toTypedArray())
    }
}

fun <T : Dao> ResultSet.readList(parseTree: ParseTree<T>): List<T> {
    use {
        return buildSequence {
            while (next()) {
                val params = getParams(columns).zip(constructor.parameters)
                val castParams = params.map { (value, type) ->
                    when (type.type) {
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