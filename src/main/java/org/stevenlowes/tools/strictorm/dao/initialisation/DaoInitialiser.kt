package org.stevenlowes.tools.strictorm.dao.initialisation

import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class DaoInitialiser {

    companion object {
        private val spec = DbSpec()
        private val schema = spec.addDefaultSchema()

        private val tables: MutableMap<KClass<out Dao>, Table> = mutableMapOf()
        private val columns: MutableMap<KClass<out Dao>, List<Pair<Column, KProperty1<out Dao, *>>>> = mutableMapOf()
        private val idColumns: MutableMap<KClass<out Dao>, Column> = mutableMapOf()

        internal fun <T : Dao> getTable(clazz: KClass<T>): Table {
            return tables[clazz] ?: throw DaoException("Table not found for class ${clazz.simpleName}")
        }

        internal fun <T : Dao> getColumns(clazz: KClass<T>): List<Pair<Column, KProperty1<T, *>>> {
            val columns = columns[clazz] ?: throw DaoException("Columns not found for class ${clazz.simpleName}")
            @Suppress("UNCHECKED_CAST")
            return columns as List<Pair<Column, KProperty1<T, *>>>
        }

        internal fun <T : Dao> getIdColumn(clazz: KClass<T>): Column {
            return idColumns[clazz] ?: throw DaoException("ID Column not found for class ${clazz.simpleName}")
        }

        fun initialise(daos: List<KClass<out Dao>>){
            daos.forEach {
                initialise(it)
            }
        }

        fun <T : Dao> initialise(dao: KClass<T>) {
            val tableName = dao.simpleName!!.toLowerCase()
            val table = schema.addTable(tableName)

            var idColumn: Column? = null
            val columns: MutableList<Pair<Column, KProperty1<T, Any?>>> = mutableListOf()

            dao.memberProperties.forEach { property ->
                val name = property.name.toLowerCase()

                val sj = StringJoiner(" ")
                val type = property.returnType

                sj.add(
                        when (type) {
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
                                throw DaoException("Unable to parse the type of $property")
                            }
                        }
                      )

                sj.add(
                        when (type.isMarkedNullable) {
                            true -> "NULL"
                            false -> "NOT NULL"
                        }
                      )

                sj.add(
                        if (name == "id") {
                            "AUTO_INCREMENT"
                        }
                        else {
                            ""
                        }
                      )

                val column = table.addColumn(name, sj.toString(), null)

                if (name == "id") {
                    idColumn = column
                }
                else {
                    columns.add(Pair(column, property))
                }
            }

            idColumn ?: throw DaoException("No ID Column found")

            tables[dao] = table
            this.columns[dao] = columns
            idColumns[dao] = idColumn!!
        }
    }
}