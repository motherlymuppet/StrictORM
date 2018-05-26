package org.stevenlowes.tools.strictorm.dao.operations

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.InsertQuery
import com.healthmarketscience.sqlbuilder.QueryPreparer
import com.healthmarketscience.sqlbuilder.UpdateQuery
import org.stevenlowes.tools.strictorm.dao.*
import org.stevenlowes.tools.strictorm.database.execute

fun <T : Dao> T.save(): T {
    return if (id == null) {
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
        query.addColumn(column, preparer.addStaticPlaceHolder(prop.get(dao)))
    }

    val id = query.execute(preparer)
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