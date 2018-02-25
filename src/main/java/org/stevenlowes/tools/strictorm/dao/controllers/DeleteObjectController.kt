package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.utils.PropUtils
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.full.memberProperties

class DeleteObjectController {
    companion object {
        fun <T : Any> delete(obj: T) {
            val sqlBuilder = StringBuilder()

            val clazz = obj::class
            val props = clazz.memberProperties
            val idProp = props.first { it.name == "id" }
            val tableName = clazz.simpleName!!.toLowerCase()

            sqlBuilder.append("DELETE FROM $tableName WHERE id = ?;")

            val id = PropUtils.getValue(obj, idProp)

            Tx.Companion.execute(sqlBuilder.toString(), listOf(id))
        }
    }
}