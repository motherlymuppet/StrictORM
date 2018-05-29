package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.RejoinTable
import com.healthmarketscience.sqlbuilder.dbspec.Table
import org.stevenlowes.tools.strictorm.database.executeQuery
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

fun <T : Dao> KClass<T>.read(id: Long): T {
    val preparer = QueryPreparer()
    val reader = QueryReader()

    val aliasGenerator = buildSequence {
        var i = 0
        while(true){
            yield("t$i")
            i++
        }
    }.iterator()

    //TODO fix this alias
    val table = RejoinTable(dbTable, aliasGenerator.next())

    val query = SelectQuery()

    val columns = recursiveConstructorColumns(query, aliasGenerator, table).map { reader.newColumn.setColumnObject(it) }
    query.addCustomColumns(*columns.toTypedArray())

    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, table.findColumn(dbIdColumn), preparer.addStaticPlaceHolder(id)))

    return query.executeQuery(preparer, primaryConstructor!!, columns).first()
}

private fun <T : Dao> KClass<T>.recursiveConstructorColumns(query: SelectQuery, aliasGenerator: Iterator<String>, table: RejoinTable): List<Column> {
    val columns = mutableListOf<Column>()

    val dataColumns = orderColumns(
            (listOf(dbIdColumn) + dbColumns.map { it.first })
                    .filterNot { it.columnNameSQL.endsWith("_id_otm") })
            .map { table.findColumn(it) }

    columns.addAll(dataColumns)

    val childDAOs = primaryConstructor!!.parameters
            .filter { it.type.isSubtypeOf(Dao::class.starProjectedType) }
            .map {
                val daoClass = (it.type.toDaoClass())
                val name = it.name + "_id_otm"
                //TODO fix this alias
                Triple(daoClass, RejoinTable(daoClass.dbTable, aliasGenerator.next()), name)
            }

    childDAOs.forEach {(propClass, propTable, columnName) ->
        query.addJoin(SelectQuery.JoinType.INNER, table, propTable, BinaryCondition(BinaryCondition.Op.EQUAL_TO, table.findColumnByName(columnName), propTable.findColumn(propClass.dbIdColumn)))
    }

    val daoColumns = childDAOs.flatMap { (propClass, propTable, _) ->
        propClass.recursiveConstructorColumns(query, aliasGenerator, propTable)
    }
    columns.addAll(daoColumns)

    return columns
}

private fun <T : Dao> KClass<T>.orderColumns(columns: List<Column>): List<Column> {
    val constructorParams = primaryConstructor!!.parameters.map {
        it.name ?: throw DaoException("Constructor param does not have name - $qualifiedName")
    }

    return columns.sortedBy {
        val name = it.columnNameSQL
        constructorParams.indexOf(name)
    }
}

private fun <T : Dao> SelectQuery.addJoins(clazz: KClass<T>) {
    val daoProps = clazz.dbColumns.filter { it.second.returnType.isSubtypeOf(Dao::class.starProjectedType) }
    daoProps.forEach { (column, prop) ->
        val propClass = prop.toDaoClass()
        addJoin(SelectQuery.JoinType.INNER, clazz.dbTable, propClass.dbTable, column, propClass.dbIdColumn)
        addJoins(propClass)
    }
}