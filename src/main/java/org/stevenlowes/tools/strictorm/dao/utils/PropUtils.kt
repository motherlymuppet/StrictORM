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
        fun <T> getValues(obj: T, props: Iterable<KProperty1<out T, Any?>>): List<Any?> {
            return buildSequence {
                props.forEach { prop ->
                    yield(getValue(obj, prop))
                }
            }.toList()
        }

        fun <T> getValue(obj: T, prop: KProperty1<out T, *>): Any? {
            return (prop as KProperty1<T, Any?>).get(obj)
        }
    }
}