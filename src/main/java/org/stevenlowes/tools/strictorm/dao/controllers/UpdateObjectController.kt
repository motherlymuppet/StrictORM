package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.database.Tx
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class UpdateObjectController {
    companion object {
        fun <T : Any> update(obj: T) {
            val sqlBuilder = StringBuilder()

            val clazz = obj::class
            val props = clazz.memberProperties
            val (idProp, nonIdProps) = props.partition { it.name == "id" }
            val tableName = clazz.simpleName!!.toLowerCase()

            sqlBuilder.append("UPDATE $tableName ")

            val setJoiner = StringJoiner(",")

            nonIdProps.forEach { property ->
                setJoiner.add("SET ${property.name.toLowerCase()} = ?")
            }

            sqlBuilder.append(setJoiner.toString())

            sqlBuilder.append(" WHERE id = ?;")

            val idIndex = props.indexOfFirst { it.name == "id" }

            val nonIdValues = nonIdProps.map { property ->
                (property as KProperty1<T, Any?>).get(obj)
            }

            val idValues = idProp.map { property ->
                (property as KProperty1<T, Any?>).get(obj)
            }

            val values = nonIdValues + idValues

            Tx.Companion.execute(sqlBuilder.toString(), *values.toTypedArray())
        }
    }
}