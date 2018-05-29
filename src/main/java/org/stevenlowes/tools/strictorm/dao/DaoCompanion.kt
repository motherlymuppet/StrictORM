package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import org.stevenlowes.tools.strictorm.database.executeQuery
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

abstract class DaoCompanion<T : Dao>(private val clazz: KClass<T>) {
    val table by lazy { clazz.dbTable }
    val columns by lazy { clazz.dbColumns }
    val idColumn by lazy { clazz.dbIdColumn }

    fun read(id: Long): T {
        return clazz.read(id)
    }
}

val <T : Dao> KClass<T>.dbTable: DbTable
        by LazyWithReceiver<KClass<T>, DbTable>
        { DaoInitialiser.getTable(this) }

val <T : Dao> KClass<T>.dbColumns: List<Pair<Column, KProperty1<T, *>>>
        by LazyWithReceiver<KClass<T>, List<Pair<Column, KProperty1<T, *>>>>
        { DaoInitialiser.getColumns(this) }

val <T : Dao> KClass<T>.dbIdColumn: Column
        by LazyWithReceiver<KClass<T>, Column>
        { DaoInitialiser.getIdColumn(this) }

fun <T : Dao> KClass<T>.read(id: Long): T {
    val preparer = QueryPreparer()
    val reader = QueryReader()
    val constructor = primaryConstructor!!
    val constructorParams = constructor.parameters.map { it.name ?: throw DaoException("Constructor param does not have name - $qualifiedName") }

    val columns = (listOf(dbIdColumn) + dbColumns.map { it.first })
            .sortedBy {//Reorder columns to match order that they appear in constructor
                val name = it.columnNameSQL.removeSuffix("_id_otm")
                constructorParams.indexOf(name)
            }
    val readerColumns = columns.map { reader.newColumn.setColumnObject(it) }


    val query = SelectQuery()
    query.addCustomColumns(*readerColumns.toTypedArray())
    query.addJoins(this)

    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, dbIdColumn, preparer.addStaticPlaceHolder(id)))

    return query.executeQuery(preparer, constructor, readerColumns).first()
}

private fun <T: Dao> SelectQuery.addJoins(clazz: KClass<T>) {
    val daoProps = clazz.dbColumns.filter { it.second.returnType.isSubtypeOf(Dao::class.starProjectedType) }
    daoProps.forEach {(column, prop) ->
        val propClass = prop.returnType.jvmErasure as KClass<Dao>
        addJoin(SelectQuery.JoinType.INNER, clazz.dbTable, propClass.dbTable, column, propClass.dbIdColumn)
        addJoins(propClass)
    }
}

class LazyWithReceiver<in This, out Return>(private val initializer: This.() -> Return) {
    private val values = WeakHashMap<This, Return>()

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any, property: KProperty<*>): Return = synchronized(values)
    {
        thisRef as This
        return values.getOrPut(thisRef) { thisRef.initializer() }
    }
}