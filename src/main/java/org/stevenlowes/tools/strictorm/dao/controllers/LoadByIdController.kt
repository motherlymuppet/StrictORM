package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.utils.ResultSetUtils
import org.stevenlowes.tools.strictorm.database.Tx
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class LoadByIdController {
    companion object {
        fun <T : Any> load(id: Long, clazz: KClass<T>): T{
            val sql = "SELECT * FROM ${clazz.simpleName} WHERE id = ?"

            return Tx.executeQuery(sql, { ResultSetUtils.readObject(it, clazz) }, listOf(id))
        }
    }
}