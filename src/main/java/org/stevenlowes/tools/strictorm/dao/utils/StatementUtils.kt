package org.stevenlowes.tools.strictorm.dao.utils

import java.util.*
import kotlin.reflect.KProperty1

class StatementUtils{
    companion object {
        fun paramString(count: Int): String{
            return 0.rangeTo(count).map{
                "?"
            }.joinToString { "," }
        }

        fun columnNameString(props: Iterable<KProperty1<*, *>>): String{
            return props.map {
                it.name.toLowerCase()
            }.joinToString { "," }
        }
    }
}