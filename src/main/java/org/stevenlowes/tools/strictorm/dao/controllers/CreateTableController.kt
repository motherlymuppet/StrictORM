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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class CreateTableController {
    companion object {
        fun createTable(clazz: KClass<*>) {
            val tableName = clazz.simpleName!!.toLowerCase()

            val properties = clazz.memberProperties

            val columns = PropUtils.getCreateTableStrings(properties)

            val sql = "CREATE TABLE $tableName ($columns, PRIMARY KEY(id));"

            Tx.Companion.execute(sql)
        }
    }
}