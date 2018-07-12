package org.stevenlowes.tools.strictorm.extensions

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoInitialiser
import org.stevenlowes.tools.strictorm.dao.ParseTree
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

fun <T : Dao> KClass<T>.getParseTree(): ParseTree<T> = DaoInitialiser.getParseTree(this)

val <T : Dao> KClass<T>.dbTable: DbTable
        by LazyWithReceiver<KClass<T>, DbTable>
        { DaoInitialiser.getTable(this) }

val <T : Dao> KClass<T>.dbColumns: List<Pair<Column, KProperty1<T, *>>>
        by LazyWithReceiver<KClass<T>, List<Pair<Column, KProperty1<T, *>>>>
        { DaoInitialiser.getColumns(this) }

val <T : Dao> KClass<T>.dbIdColumn: Column
        by LazyWithReceiver<KClass<T>, Column>
        { DaoInitialiser.getIdColumn(this) }

fun <T : Dao> KClass<T>.read(id: Int): T {
    val preparer = QueryPreparer()
    val parseTree = getParseTree()
    val query = parseTree.selectQuery

    val idColumn = parseTree.table.findColumn(dbIdColumn)
    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, idColumn, preparer.addStaticPlaceHolder(id)))

    return query.executeQuery(preparer, parseTree).first()
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