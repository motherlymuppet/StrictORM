package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.QueryReader
import com.healthmarketscience.sqlbuilder.SelectQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.RejoinTable
import com.healthmarketscience.sqlbuilder.dbspec.Table
import org.stevenlowes.tools.strictorm.database.ParseTree
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
    val parseTree = ParseTree.generate(this)
    val query = parseTree.selectQuery

    val idColumn = parseTree.table.findColumn(dbIdColumn)
    query.addCondition(BinaryCondition(BinaryCondition.Op.EQUAL_TO, idColumn, preparer.addStaticPlaceHolder(id)))

    return query.executeQuery(preparer, parseTree).first()
}