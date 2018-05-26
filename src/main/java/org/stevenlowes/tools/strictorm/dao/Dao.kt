package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import org.stevenlowes.tools.strictorm.dao.initialisation.DaoInitialiser
import org.stevenlowes.tools.strictorm.dao.utils.LazyWithReceiver
import org.stevenlowes.tools.strictorm.dao.utils.ResultSetUtils
import org.stevenlowes.tools.strictorm.database.execute
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface Dao{
    val id: Long?
}

val <T: Dao> KClass<T>.dbTable: Table
        by LazyWithReceiver<KClass<T>, Table>
        { DaoInitialiser.getTable(this) }

val <T: Dao> KClass<T>.dbColumns: Map<Column, KProperty1<Dao, Any>>
        by LazyWithReceiver<KClass<T>, Map<Column, KProperty1<Dao, Any>>>
        { DaoInitialiser.getColumns(this) }

val <T: Dao> KClass<T>.dbIdColumn: Column
        by LazyWithReceiver<KClass<T>, Column>
        { DaoInitialiser.getIdColumn(this) }

fun <T: Dao> KClass<T>.read(id: Long): T{
    val preparer = QueryPreparer()
    val columns = listOf(dbIdColumn) + dbColumns.keys

    val query = SelectQuery()
    query.addColumns(*columns.toTypedArray())
    query.addFromTable(dbTable)
    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, dbIdColumn, preparer.addStaticPlaceHolder(id)))
    return query.execute(preparer, {ResultSetUtils.readObject(it, this, columns)})
}

val <T: Dao> T.dbTable get() = this::class.dbTable
val <T: Dao> T.dbColumns get() = this::class.dbColumns
val <T: Dao> T.dbIdColumn get() = this::class.dbIdColumn