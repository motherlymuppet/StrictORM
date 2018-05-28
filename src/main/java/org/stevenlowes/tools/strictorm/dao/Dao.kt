package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.*
import com.healthmarketscience.sqlbuilder.dbspec.Column
import org.stevenlowes.tools.strictorm.database.execute
import org.stevenlowes.tools.strictorm.database.executeInsert
import kotlin.reflect.KProperty1

interface Dao{
    val id: Long
    val dbTable get() = this::class.dbTable
    val dbIdColumn get() = this::class.dbIdColumn

    fun delete() {
        val preparer = QueryPreparer()
        val query = DeleteQuery(dbTable).addCondition(
                BinaryCondition(BinaryCondition.Op.EQUAL_TO,
                                dbIdColumn,
                                preparer.addStaticPlaceHolder(id)
                               ))
        query.execute(preparer)
    }
}

@Suppress("UNCHECKED_CAST")
val <T: Dao> T.dbColumns: List<Pair<Column, KProperty1<T, *>>> get() = this::class.dbColumns as List<Pair<Column, KProperty1<T, *>>>

fun <T: Dao> T.reload(): T{
    val id = this.id
    if(id == -1L){
        throw DaoException("Cannot reload when never saved")
    }
    else{
        return this::class.read(id)
    }
}

fun <T : Dao> T.save(): T {
    return if (id == -1L) {
        insert(this)
    }
    else {
        update(this)
    }
}

private fun <T : Dao> insert(dao: T): T {
    val preparer = QueryPreparer()
    val query = InsertQuery(dao.dbTable)
    dao.dbColumns.forEach { (column, prop) ->
        val placeholder = preparer.addStaticPlaceHolder(prop.get(dao))
        query.addColumn(column, placeholder)
    }

    val id = query.executeInsert(preparer)
    return dao::class.read(id)
}

private fun <T : Dao> update(dao: T): T {
    val preparer = QueryPreparer()
    val query = UpdateQuery(dao.dbTable)

    dao.dbColumns.forEach { (column, prop) ->
        query.addSetClause(column, preparer.addStaticPlaceHolder(prop.get(dao)))
    }

    query.addCondition(BinaryCondition(
            BinaryCondition.Op.EQUAL_TO,
            dao.dbIdColumn,
            preparer.addStaticPlaceHolder(dao.id)
                                      ))

    query.execute(preparer)
    return dao
}