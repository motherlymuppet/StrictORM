package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.RejoinTable
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.function.Supplier
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

class ParseTreeBuilder<T : Dao>(private val constructor: KFunction<T>,
                                private val table: RejoinTable,
                                private val columns: Map<Int, RejoinTable.RejoinColumn>,
                                private val children: Map<Int, ParseTreeBuilder<*>>) : Supplier<ParseTree<T>> {
    override fun get(): ParseTree<T> {
        return innerGet(QueryReader())
    }

    private fun innerGet(queryReader: QueryReader): ParseTree<T> {
        return ParseTree(
                constructor,
                table,
                columns.mapValues {
                    queryReader.newColumn.setColumnObject(it.value)
                },
                children.mapValues {
                    it.value.innerGet(queryReader)
                }
                        )
    }

    companion object {
        fun <T : Dao> generate(clazz: KClass<T>): ParseTreeBuilder<T> {

            val aliasGenerator = buildSequence {
                var i = 0
                while (true) {
                    yield("t$i")
                    i++
                }
            }.iterator()
            return generate(clazz, aliasGenerator)
        }

        private fun <T : Dao> generate(clazz: KClass<T>, aliasGenerator: Iterator<String>): ParseTreeBuilder<T> {
            val table = RejoinTable(clazz.dbTable, aliasGenerator.next())
            val constructor = clazz.primaryConstructor!!

            val constructorParams = constructor.parameters.map {
                it.name ?: throw DaoException("Constructor param does not have name - ${clazz.qualifiedName}")
            }

            val dataColumns = (listOf(clazz.dbIdColumn) + clazz.dbColumns.map { it.first })
                    .filterNot { it.columnNameSQL.endsWith("_id_otm") }
                    .map { constructorParams.indexOf(it.columnNameSQL) to table.findColumn(it) }
                    .toMap()

            val childTrees = constructor.parameters.withIndex()
                    .filter { (_, value) -> value.type.withNullability(false).isSubtypeOf(Dao::class.starProjectedType) }
                    .map { (index, value) ->
                        index to generate(value.type.toDaoClass(), aliasGenerator)
                    }.toMap()


            return ParseTreeBuilder(clazz.primaryConstructor!!,
                                    table,
                                    dataColumns,
                                    childTrees)
        }
    }
}

class ParseTree<T : Dao> internal constructor(val constructor: KFunction<T>,
                                              val table: RejoinTable,
                                              val columns: Map<Int, QueryReader.Column>,
                                              val children: Map<Int, ParseTree<*>>) {
    val selectQuery: SelectQuery = SelectQuery()
    private val idColumn = columns[constructor.parameters.indexOfFirst { it.name == "id" }]!!

    init {
        addJoins(selectQuery)
        selectQuery.addCustomColumns(*getColumnsRecursively().toTypedArray())
    }

    fun parse(rs: ResultSet): List<T> {
        return buildSequence {
            rs.use {
                while (rs.next()) {
                    val obj = parseOnce(rs) ?: continue
                    yield(obj)
                }
            }
        }.toList()
    }

    private fun parseOnce(rs: ResultSet): T? {
        idColumn.getInt(rs)
        if (rs.wasNull()) return null

        val paramMap = mutableListOf<Pair<Int, Any?>>()
        columns.forEach { pos, column ->
            val obj: Any? = column.getObject(rs)
            if(rs.wasNull()){
                println("here")
            }
            when {
                rs.wasNull() -> paramMap.add(pos to null)
                obj == null -> throw DaoException("Column not in query: $column")
                else -> paramMap.add(pos to obj)
            }
        }

        children.forEach { pos, column ->
            paramMap.add(pos to column.parseOnce(rs))
        }

        val params = paramMap.toList()
                .sortedBy { it.first }
                .map { it.second }
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

    private fun getColumnsRecursively(): List<QueryReader.Column> {
        return columns.values.toList() + children.flatMap { it.value.getColumnsRecursively() }
    }

    private fun addJoins(query: SelectQuery) {
        children.forEach { constructorParamIndex, child ->
            val childIdColumnName = constructor.parameters[constructorParamIndex].name!!.toLowerCase() + "_id_otm"
            query.addJoin(SelectQuery.JoinType.LEFT_OUTER,
                          table,
                          child.table,
                          BinaryCondition(BinaryCondition.Op.EQUAL_TO,
                                          table.findColumnByName(childIdColumnName),
                                          child.table.findColumnByName("id")))
            child.addJoins(query)
        }
    }
}