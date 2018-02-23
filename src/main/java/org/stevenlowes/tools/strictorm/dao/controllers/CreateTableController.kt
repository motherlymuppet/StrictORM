package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.database.Tx
import java.math.BigDecimal
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class CreateTableController {
    companion object {
        fun createTable(clazz: KClass<*>) {
            val tableName = clazz.simpleName!!.toLowerCase()

            val fields = clazz.memberProperties

            val sqlBuilder = StringBuilder()
            sqlBuilder.append("CREATE TABLE ")
            sqlBuilder.append(tableName)
            sqlBuilder.append("(")

            fields.forEach { field ->
                sqlBuilder.append(field.name)
                sqlBuilder.append(" ")

                val type = when (field.returnType) {
                    String::class.starProjectedType -> "BLOB"
                    Long::class.starProjectedType -> "BIGINT"
                    Int::class.starProjectedType -> "INTEGER"
                    Boolean::class.starProjectedType -> "BOOLEAN"
                    LocalDate::class.starProjectedType -> "DATE"
                    Double::class.starProjectedType -> "DOUBLE"
                    Float::class.starProjectedType -> "FLOAT"
                    LocalTime::class.starProjectedType -> "TIME"
                    LocalDateTime::class.starProjectedType -> "TIMESTAMP"
                    else -> {
                        throw DaoException("Unable to parse the type of $field")
                    }
                }

                sqlBuilder.append(type)

                if(field.returnType.isMarkedNullable){
                    sqlBuilder.append(" NULL,")
                }
                else{
                    sqlBuilder.append(" NOT NULL,")
                }
            }

            sqlBuilder.append("PRIMARY KEY (id));")

            Tx.Companion.execute(sqlBuilder.toString())
        }
    }
}