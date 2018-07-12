package org.stevenlowes.tools.strictorm.dao

import org.stevenlowes.tools.strictorm.extensions.*
import kotlin.reflect.KClass

abstract class DaoCompanion<T : Dao>(private val clazz: KClass<T>) {
    val table by lazy { clazz.dbTable }
    val columns by lazy { clazz.dbColumns }
    val idColumn by lazy { clazz.dbIdColumn }

    fun read(id: Int): T {
        return clazz.read(id)
    }

    fun list(): List<T>{
        return clazz.list
    }
}

private val <T: Dao> KClass<T>.list: List<T> get() {
    val parseTree = getParseTree()
    return parseTree.selectQuery.executeQuery(null, parseTree)
}