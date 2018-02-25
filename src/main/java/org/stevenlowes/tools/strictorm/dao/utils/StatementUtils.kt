package org.stevenlowes.tools.strictorm.dao.utils

import java.util.*
import kotlin.reflect.KProperty1

class StatementUtils{
    companion object {
        fun paramString(count: Int): String{
            return 1.rangeTo(count).map{
                "?"
            }.joinToString(",")
        }
    }
}