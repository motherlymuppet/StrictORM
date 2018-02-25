package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.dao.utils.PropUtils
import org.stevenlowes.tools.strictorm.database.Tx
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class UpdateObjectController {
    companion object {
        fun <T : Any> update(obj: T) {
            val clazz = obj::class
            val props = clazz.memberProperties
            val (idProp, nonIdProps) = props.partition { it.name == "id" }

            val tableName = clazz.simpleName!!.toLowerCase()
            val setString = getSetString(nonIdProps)
            val sql = "UPDATE $tableName $setString WHERE id = ?;"

            val nonIdValues = PropUtils.getValues(obj, nonIdProps)
            val idValues = PropUtils.getValues(obj, idProp)
            val values = nonIdValues + idValues

            Tx.Companion.execute(sql, values)
        }

        private fun <T> getSetString(nonIdProps: List<KProperty1<out T, Any?>>): String{
            return nonIdProps.map { "SET ${it.name.toLowerCase()} = ?" }.joinToString(",")
        }
    }
}