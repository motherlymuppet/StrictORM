package org.stevenlowes.tools.strictorm.extensions

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.exceptions.DaoInvalidColumnTypeException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

private val dataTypes: Map<KType, String> = listOf(
        String::class.starProjectedType to "VARCHAR",
        BigDecimal::class.starProjectedType to "DECIMAL",
        Int::class.starProjectedType to "BIGINT",
        Int::class.starProjectedType to "INTEGER",
        Boolean::class.starProjectedType to "BOOLEAN",
        LocalDate::class.starProjectedType to "DATE",
        Double::class.starProjectedType to "DOUBLE",
        Float::class.starProjectedType to "FLOAT",
        LocalTime::class.starProjectedType to "TIME",
        LocalDateTime::class.starProjectedType to "TIMESTAMP"
                                                  ).toMap()

fun KType.isValid(): Boolean = isDataType() || isDao()

fun KType.isDao(): Boolean = withNullability(false).isSubtypeOf(Dao::class.starProjectedType)

fun KType.isDataType(): Boolean = this.withNullability(false) in dataTypes

fun KType.toColumnTypeString(): String = dataTypes[this]
        ?: throw DaoInvalidColumnTypeException("Property type ${this} could not be mapped to a database column type")

fun KType.toDaoClass(): KClass<out Dao> {
    @Suppress("UNCHECKED_CAST")
    return jvmErasure as KClass<Dao>
}

fun <T: Dao> KProperty1<T, *>.toDaoClass(): KClass<out Dao> {
    return returnType.toDaoClass()
}