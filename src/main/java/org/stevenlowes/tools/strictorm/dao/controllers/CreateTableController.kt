package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.dao.utils.PropUtils
import org.stevenlowes.tools.strictorm.database.Tx
import java.math.BigDecimal
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class CreateTableController {
    companion object {
        fun createTable(clazz: KClass<*>) {
            val tableName = clazz.simpleName!!.toLowerCase()

            val properties = clazz.memberProperties

            val columns = getColumnStrings(properties)

            val sql = "CREATE TABLE $tableName ($columns, PRIMARY KEY(id));"

            Tx.Companion.execute(sql)
        }

        private fun getColumnStrings(props: Iterable<KProperty1<*, *>>): String{
            val columns =props.map { getColumnString(it) }
            return columns.joinToString(",")
        }

        private fun getColumnString(prop: KProperty1<*, *>): String {
            val name = prop.name.toLowerCase()

            val returnType = prop.returnType

            val sqlType = when (returnType) {
                String::class.starProjectedType -> "LONGVARCHAR"
                Long::class.starProjectedType -> "BIGINT"
                Int::class.starProjectedType -> "INTEGER"
                Boolean::class.starProjectedType -> "BOOLEAN"
                LocalDate::class.starProjectedType -> "DATE"
                Double::class.starProjectedType -> "DOUBLE"
                Float::class.starProjectedType -> "FLOAT"
                LocalTime::class.starProjectedType -> "TIME"
                LocalDateTime::class.starProjectedType -> "TIMESTAMP"
                else -> {
                    throw DaoException("Unable to parse the type of $prop")
                }
            }

            val nullability = when(returnType.isMarkedNullable){
                true -> "NULL"
                false -> "NOT NULL"
            }

            val autoIncrement = if(name == "id"){
                "AUTO_INCREMENT"
            }
            else{
                ""
            }

            val string = "$name $sqlType $nullability $autoIncrement"
            return string
        }
    }
}