package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import org.stevenlowes.tools.strictorm.database.executeQuery
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

abstract class DaoCompanion<T : Dao>(private val clazz: KClass<T>) {
    val table by lazy { clazz.dbTable }
    val columns by lazy { clazz.dbColumns }
    val idColumn by lazy { clazz.dbIdColumn }

    fun read(id: Int): T {
        return clazz.read(id)
    }

    fun list(): List<T>{
        return clazz.list
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

fun <T : Dao> KClass<T>.getParseTree(): ParseTree<T> = DaoInitialiser.getParseTree(this)

fun <T : Dao> KClass<T>.read(id: Int): T {
    val preparer = QueryPreparer()
    val parseTree = getParseTree()
    val query = parseTree.selectQuery

    val idColumn = parseTree.table.findColumn(dbIdColumn)
    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, idColumn, preparer.addStaticPlaceHolder(id)))

    return query.executeQuery(preparer, parseTree).first()
}

private val <T: Dao> KClass<T>.list: List<T> get() {
    val parseTree = getParseTree()
    return parseTree.selectQuery.executeQuery(null, parseTree)
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