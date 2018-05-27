package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.CreateTableQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import org.stevenlowes.tools.strictorm.database.execute
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
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

        fun createTables(){
            createTables(tables.values)
        }

        private fun createTables(tables: Iterable<Table>){
            tables.forEach {
                CreateTableQuery(it, true).execute()
            }
        }

        fun initialise(daos: List<KClass<out Dao>>) {
            daos.forEach {
                initialise(it)
            }
        }

        fun <T : Dao> initialise(dao: KClass<T>) {
            val tableName = dao.simpleName!!.toLowerCase()
            val table = schema.addTable(tableName)

            val columns = dao.declaredMemberProperties
                    .filter { it.name != "id" }
                    .map{ addColumn(table, it) to it }

            val idColumn = table.addColumn("id", "IDENTITY", null)
            idColumn.notNull()
            idColumn.primaryKey()
            idColumns[dao] = idColumn

            tables[dao] = table
            this.columns[dao] = columns
        }

        private fun <T : Dao> addColumn(table: DbTable, property: KProperty1<T, *>): DbColumn {
            val name = property.name.toLowerCase()

            val sj = StringJoiner(" ")
            val type = property.returnType

            sj.add(
                    when (type) {
                        String::class.starProjectedType -> "VARCHAR"
                        BigDecimal::class.starProjectedType -> "DECIMAL"
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

            val column = when (type) {
                String::class.starProjectedType -> addStringColumn(property, table, name, sj.toString())
                BigDecimal::class.starProjectedType -> addDecimalColumn(property, table, name, sj.toString())
                else -> table.addColumn(name, sj.toString(), null)
            }

            if(!type.isMarkedNullable){
                column.notNull()
            }

            return column
        }

        private fun <T : Dao> addDecimalColumn(property: KProperty1<T, *>,
                                               table: DbTable,
                                               name: String,
                                               type: String): DbColumn {
            val decimalAnnotation = property.annotations.firstOrNull { it.annotationClass == DecimalPrecision::class } as DecimalPrecision?
            val scale = decimalAnnotation?.scale ?: 21
            val precision = decimalAnnotation?.precision ?: 7
            return table.addColumn(name, type, precision, scale)
        }

        private fun <T : Dao> addStringColumn(property: KProperty1<T, *>,
                                              table: DbTable,
                                              name: String,
                                              type: String): DbColumn {
            val lengthAnnotation = property.annotations.firstOrNull { it.annotationClass == StringLength::class } as StringLength?
            val length = lengthAnnotation?.length ?: 250
            return table.addColumn(name, type, length)
        }
    }
}