package org.stevenlowes.tools.strictorm.dao.operations

import com.healthmarketscience.sqlbuilder.BinaryCondition
import com.healthmarketscience.sqlbuilder.DeleteQuery
import com.healthmarketscience.sqlbuilder.QueryPreparer
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.dbIdColumn
import org.stevenlowes.tools.strictorm.dao.dbTable
import org.stevenlowes.tools.strictorm.database.execute

fun <T : Dao> T.delete() {
    val preparer = QueryPreparer()
    val query = DeleteQuery(dbTable).addCondition(
            BinaryCondition(BinaryCondition.Op.EQUAL_TO,
                            dbIdColumn,
                            preparer.addStaticPlaceHolder(id)
                           ))
    query.execute(preparer)
}