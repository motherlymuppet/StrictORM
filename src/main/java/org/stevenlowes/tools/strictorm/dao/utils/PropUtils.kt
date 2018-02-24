package org.stevenlowes.tools.strictorm.dao.utils

import org.stevenlowes.tools.strictorm.dao.DaoException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KProperty1
import kotlin.reflect.full.starProjectedType

class PropUtils {
    companion object {
        fun <T> getValues(obj: T, props: Iterable<KProperty1<out T, Any?>>): Array<Any?> {
            return buildSequence {
                props.forEach { prop ->
                    yield(getValue(obj, prop))
                }
            }.toList().toTypedArray()
        }

        private fun <T> getValue(obj: T, prop: KProperty1<out T, *>): Any? {
            return (prop as KProperty1<T, Any?>).get(obj)
        }

        fun getCreateTableStrings(props: Iterable<KProperty1<*, *>>): String{
            return props.map { getCreateTableString(it) }.joinToString { "," }
        }

        private fun getCreateTableString(prop: KProperty1<*, *>): String {
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

            return "$name $sqlType $nullability $autoIncrement"
        }
    }
}