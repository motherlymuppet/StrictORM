package org.stevenlowes.tools.strictorm.dao.initialisation

import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec
import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.database.Tx
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
        private val tables: Map<KClass<out Dao>, Table> = null
        private val columns: Map<KClass<out Dao>, Map<Column, KProperty1<Dao, Any>>> = null
        private val idColumns: Map<KClass<out Dao>, Column> = null

        internal fun <T: Dao> getTable(clazz: KClass<T>): Table{
            return tables[clazz] ?: throw DaoException("Table not found for class ${clazz.simpleName}")
        }

        internal fun <T: Dao> getColumns(clazz: KClass<T>): Map<Column, KProperty1<Dao, Any>>{
            return columns[clazz] ?: throw DaoException("Columns not found for class ${clazz.simpleName}")
        }

        internal fun <T: Dao> getIdColumn(clazz: KClass<T>): Column{
            return idColumns[clazz] ?: throw DaoException("ID Column not found for class ${clazz.simpleName}")
        }

        fun initialise(daos: Iterable<KClass<out Dao>>) {
            val spec = DbSpec()
            val schema = spec.addDefaultSchema()

            daos.forEach { dao ->
                val tableName = dao.simpleName!!.toLowerCase()
                val table = schema.addTable(tableName)

                dao.memberProperties.forEach { property ->
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

                    val name = property.name.toLowerCase()

                    sj.add(
                            if (name == "id") {
                                "AUTO_INCREMENT"
                            }
                            else {
                                ""
                            }
                          )

                    table.addColumn(name, name, null)
                }
            }

            //TODO create schema in database
        }
    }
}