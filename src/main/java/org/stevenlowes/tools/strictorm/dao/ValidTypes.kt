package org.stevenlowes.tools.strictorm.dao

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

class ValidTypes {
    companion object {
        private val dataTypes: List<KType> = listOf(
                String::class.starProjectedType,
                BigDecimal::class.starProjectedType,
                Long::class.starProjectedType,
                Int::class.starProjectedType,
                Boolean::class.starProjectedType,
                LocalDate::class.starProjectedType,
                Double::class.starProjectedType,
                Float::class.starProjectedType,
                LocalTime::class.starProjectedType,
                LocalDateTime::class.starProjectedType
                                           )

        fun isValid(type: KType): Boolean {
            return isDataType(type) || isDao(type)
        }

        private fun isDao(type: KType): Boolean {
            return type.isSubtypeOf(Dao::class.starProjectedType)
        }

        fun isDataType(type: KType): Boolean {
            return type in dataTypes
        }
    }
}