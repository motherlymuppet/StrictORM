package org.stevenlowes.tools.strictorm.dao

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

fun KType.toDaoClass(): KClass<out Dao> {
    @Suppress("UNCHECKED_CAST")
    return jvmErasure as KClass<Dao>
}

fun <T: Dao> KProperty1<T, *>.toDaoClass(): KClass<out Dao> {
    return returnType.toDaoClass()
}