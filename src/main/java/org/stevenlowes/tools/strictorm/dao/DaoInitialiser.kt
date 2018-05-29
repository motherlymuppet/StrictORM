package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.CreateTableQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbForeignKeyConstraint
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import javafx.scene.control.Tab
import org.stevenlowes.tools.strictorm.database.execute
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

//TODO how to map properties not appearing in the same order as constructor arguments

class DaoInitialiser {
    companion object {
        private val spec = DbSpec()
        private val schema = spec.addDefaultSchema()

        private val tables: MutableMap<KClass<out Dao>, DbTable> = mutableMapOf()
        private val columns: MutableMap<KClass<out Dao>, List<Pair<Column, KProperty1<out Dao, *>>>> = mutableMapOf()
        private val idColumns: MutableMap<KClass<out Dao>, Column> = mutableMapOf()

        internal fun <T : Dao> getTable(clazz: KClass<T>): DbTable {
            return tables[clazz] ?: throw DaoException("Table not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")
        }

        internal fun <T : Dao> getColumns(clazz: KClass<T>): List<Pair<Column, KProperty1<T, *>>> {
            val columns = columns[clazz] ?: throw DaoException("Columns not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")
            @Suppress("UNCHECKED_CAST")
            return columns as List<Pair<Column, KProperty1<T, *>>>
        }

        internal fun <T : Dao> getIdColumn(clazz: KClass<T>): Column {
            return idColumns[clazz] ?: throw DaoException("ID Column not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")
        }

        fun createTables(){
            createTables(tables.values)
        }

        private fun createTables(tables: Iterable<Table>){
            tables.forEach {
                CreateTableQuery(it, true).execute()
            }
        }

        fun initialise(vararg daos: KClass<out Dao>) {
            initialise(daos.toList())
        }

        fun initialise(daos: Iterable<KClass<out Dao>>) {
            val list = daos.toMutableList()
            val created: MutableList<KType> = mutableListOf()

            while(list.isNotEmpty()){
                val toAdd = list.filter {
                    it.declaredMemberProperties.all {
                        val returnType = it.returnType
                        ValidTypes.isDataType(returnType) || returnType in created
                    }
                }

                if(toAdd.isEmpty()){
                    throw DaoException("Cannot initialise DAOs due to dependency loop")
                }

                toAdd.forEach {
                    initialise(it)
                }

                list.removeAll(toAdd)
                created.addAll(toAdd.map { it.starProjectedType })
            }
        }

        fun <T : Dao> initialise(dao: KClass<T>) {
            DaoValidation.verify(dao)

            val tableName = dao.simpleName!!.toLowerCase()
            val table = schema.addTable(tableName)

            //TESTING
            val params = dao.primaryConstructor!!.parameters
            params.map { it.name }

            val props = dao.declaredMemberProperties
            props.map { it.name }

            //END TEST

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

        private fun <T: Dao> addColumn(table: DbTable, property: KProperty1<T, *>): DbColumn{
            return if(ValidTypes.isDataType(property.returnType)){
                addDataColumn(table, property)
            }
            else{
                addOneToManyColumn(table, property)
            }
        }

        private fun <T: Dao> addOneToManyColumn(table: DbTable, property: KProperty1<T, *>): DbColumn{
            val name = property.name.toLowerCase() + "_id_otm"
            val column = table.addColumn(name, "BIGINT", null)

            if(!property.returnType.isMarkedNullable){
                column.notNull()
            }

            val erasure = property.returnType.jvmErasure as KClass<out Dao>
            val otherTable = erasure.dbTable
            val foreignKeyName = "foreign_key_${table.name}_${otherTable.tableNameSQL}_$name"
            val foreignKey = DbForeignKeyConstraint(column, foreignKeyName, otherTable, "id")
            column.addConstraint(foreignKey)

            return column
        }

        private fun <T : Dao> addDataColumn(table: DbTable, property: KProperty1<T, *>): DbColumn {
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