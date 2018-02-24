package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.database.Tx
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class CreateObjectController {
    companion object {
        fun <T : Any> create(obj: T): T {
            val sqlBuilder = StringBuilder()

            val clazz = obj::class
            val props = clazz.memberProperties.filter { it.name != "id" }
            val tableName = clazz.simpleName!!.toLowerCase()

            sqlBuilder.append("INSERT INTO $tableName (")

            val columnNameJoiner = StringJoiner(",")
            val paramsJoiner = StringJoiner(",")
            props.forEach { property ->
                columnNameJoiner.add(property.name.toLowerCase())
                paramsJoiner.add("?")
            }

            sqlBuilder.append(columnNameJoiner)
            sqlBuilder.append(") VALUES (")
            sqlBuilder.append(paramsJoiner)
            sqlBuilder.append(");")

            val values = props.map { property ->
                (property as KProperty1<T, Any?>).get(obj)
            }.toTypedArray()

            val id = Tx.Companion.executeInsert(sqlBuilder.toString(), *values)

            return DaoController.loadById(id, clazz)
        }
    }
}