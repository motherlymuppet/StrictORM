package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.dao.utils.PropUtils
import org.stevenlowes.tools.strictorm.dao.utils.StatementUtils
import org.stevenlowes.tools.strictorm.database.Tx
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class CreateObjectController {
    companion object {
        fun <T : Any> create(obj: T): T {
            val clazz = obj::class
            val props: List<KProperty1<out T, Any?>> = clazz.memberProperties.filter { it.name != "id" }

            val tableName = clazz.simpleName!!.toLowerCase()
            val columnString = columnNameString(props)
            val paramString = StatementUtils.paramString(props.size)

            val sql = "INSERT INTO $tableName ($columnString) VALUES ($paramString);"

            val values = PropUtils.getValues(obj, props)

            val id = Tx.Companion.executeInsert(sql, values)

            return DaoController.loadById(id, clazz)
        }

        private fun columnNameString(props: Iterable<KProperty1<*, *>>): String{
            return props.map {
                it.name.toLowerCase()
            }.joinToString(",")
        }
    }
}