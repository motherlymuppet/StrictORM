package org.stevenlowes.tools.strictorm.database

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.RejoinTable
import org.stevenlowes.tools.strictorm.dao.*
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
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

class ParseTree<T : Dao>(val constructor: KFunction<T>,
                         val table: RejoinTable,
                         val columns: Map<QueryReader.Column, Int>,
                         val children: Map<ParseTree<*>, Int>) {
    fun parse(rs: ResultSet): List<T> {
        return buildSequence {
            rs.use {
                while (rs.next()) {
                    yield(parseOnce(rs))
                }
            }
        }.toList()
    }

    private fun parseOnce(rs: ResultSet): T {
        val paramMap = mutableListOf<Pair<Any?, Int>>()

        columns.forEach{ column, pos ->
            val obj: Any? = column.getObject(rs)
            when {
                rs.wasNull() -> paramMap.add(null to pos)
                obj == null -> throw DaoException("Column not in query: $column")
                else -> paramMap.add(obj to pos)
            }
        }

        children.forEach { column, pos ->
            paramMap.add(column.parseOnce(rs) to pos)
        }

        val params = paramMap.toList()
                .sortedBy { it.second }
                .map { it.first }
                .zip(constructor.parameters)
                .map { (value, constructorParam) ->
                    when (constructorParam.type) { //Map SQL date types to java 8 date types
                        LocalDate::class.starProjectedType -> (value as Date).toLocalDate()
                        LocalTime::class.starProjectedType -> (value as Time).toLocalTime()
                        LocalDateTime::class.starProjectedType -> (value as Timestamp).toLocalDateTime()
                        else -> value
                    }
                }
        return constructor.call(*params.toTypedArray())
    }

    val selectQuery: SelectQuery
        get() {
            val query = SelectQuery()
            addJoins(query)
            query.addCustomColumns(*getColumnsRecursively().toTypedArray())
            return query
        }

    private fun getColumnsRecursively(): List<QueryReader.Column> {
        return columns.keys.toList() + children.flatMap { it.key.getColumnsRecursively() }
    }

    private fun addJoins(query: SelectQuery) {
        children.forEach { child, constructorParamIndex ->
            val childIdColumnName = constructor.parameters[constructorParamIndex].name!!.toLowerCase() + "_id_otm"
            query.addJoin(SelectQuery.JoinType.INNER,
                          table,
                          child.table,
                          BinaryCondition(BinaryCondition.Op.EQUAL_TO,
                                          table.findColumnByName(childIdColumnName),
                                          child.table.findColumnByName("id")))
            child.addJoins(query)
        }
    }

    companion object {
        fun <T: Dao> generate(clazz: KClass<T>): ParseTree<T> {
            val queryReader = QueryReader()

            val aliasGenerator = buildSequence {
                var i = 0
                while (true) {
                    yield("t$i")
                    i++
                }
            }.iterator()
            return generate(clazz, queryReader, aliasGenerator)
        }

        private fun <T : Dao> generate(clazz: KClass<T>, queryReader: QueryReader, aliasGenerator: Iterator<String>): ParseTree<T> {
            val table = RejoinTable(clazz.dbTable, aliasGenerator.next())
            val constructor = clazz.primaryConstructor!!

            val constructorParams = constructor.parameters.map {
                it.name ?: throw DaoException("Constructor param does not have name - ${clazz.qualifiedName}")
            }

            val dataColumns = (listOf(clazz.dbIdColumn) + clazz.dbColumns.map { it.first })
                    .filterNot { it.columnNameSQL.endsWith("_id_otm") }
                    .map { queryReader.newColumn.setColumnObject(table.findColumn(it)) to constructorParams.indexOf(it.columnNameSQL) }
                    .toMap()

            val childTrees = constructor.parameters.withIndex()
                    .filter { (_, value) -> value.type.isSubtypeOf(Dao::class.starProjectedType) }
                    .map { (index, value) ->
                        generate(value.type.toDaoClass(), queryReader, aliasGenerator) to index
                    }.toMap()


            return ParseTree(clazz.primaryConstructor!!, table, dataColumns, childTrees)
        }
    }
}