package org.stevenlowes.tools.strictorm.dao

import com.healthmarketscience.sqlbuilder.CreateTableQuery
import com.healthmarketscience.sqlbuilder.dbspec.Column
import com.healthmarketscience.sqlbuilder.dbspec.Table
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbForeignKeyConstraint
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable
import org.stevenlowes.tools.strictorm.dao.annotations.DecimalPrecision
import org.stevenlowes.tools.strictorm.dao.annotations.StringLength
import org.stevenlowes.tools.strictorm.dao.exceptions.DaoException
import org.stevenlowes.tools.strictorm.dao.exceptions.DaoInvalidColumnTypeException
import org.stevenlowes.tools.strictorm.extensions.*
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

class DaoInitialiser {
    companion object {
        private val spec = DbSpec()
        private val schema = spec.addDefaultSchema()

        private val tables: MutableMap<KClass<out Dao>, DbTable> = mutableMapOf()
        private val columns: MutableMap<KClass<out Dao>, List<Pair<Column, KProperty1<out Dao, *>>>> = mutableMapOf()
        private val idColumns: MutableMap<KClass<out Dao>, Column> = mutableMapOf()
        private val parseTreeBuilders: MutableMap<KClass<out Dao>, ParseTreeBuilder<out Dao>> = mutableMapOf()

        internal fun <T : Dao> getTable(clazz: KClass<T>): DbTable {
            return tables[clazz]
                    ?: throw DaoException("Table not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")
        }

        internal fun <T : Dao> getColumns(clazz: KClass<T>): List<Pair<Column, KProperty1<T, *>>> {
            val columns = columns[clazz]
                    ?: throw DaoException("Columns not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")

            @Suppress("UNCHECKED_CAST")
            return columns as List<Pair<Column, KProperty1<T, *>>>
        }

        internal fun <T : Dao> getIdColumn(clazz: KClass<T>): Column {
            return idColumns[clazz]
                    ?: throw DaoException("ID Column not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")
        }

        internal fun <T : Dao> getParseTree(clazz: KClass<T>): ParseTree<T> {
            val builder = parseTreeBuilders[clazz]
                    ?: throw DaoException("Parse tree not found for class ${clazz.simpleName}. Check that it was passed to the initialisation call.")

            @Suppress("UNCHECKED_CAST")
            return (builder as ParseTreeBuilder<T>).get()
        }

        fun createTables() {
            createTables(tables.values)
        }

        private fun createTables(tables: Iterable<Table>) {
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

            while (list.isNotEmpty()) {
                val toAdd = list.filter {
                    it.declaredMemberProperties.all {
                        val returnType = it.returnType.withNullability(false)
                        returnType.isDataType() || returnType in created
                    }
                }

                if (toAdd.isEmpty()) {
                    throw DaoException("Cannot initialise DAOs due to dependency loop in the following DAOs: ${list.map { it.qualifiedName }}")
                }

                toAdd.forEach {
                    initialise(it)
                }

                list.removeAll(toAdd)
                created.addAll(toAdd.map { it.starProjectedType })
            }
        }

        private fun <T : Dao> initialise(dao: KClass<T>) {
            DaoValidation.verify(dao)

            val tableName = dao.simpleName!!.toLowerCase()
            val table = schema.addTable(tableName)

            val columns = dao.declaredMemberProperties
                    .filter { it.name != "id" }
                    .map { addColumn(table, it) to it }

            val idColumn = table.addColumn("id", "IDENTITY", null)
            idColumn.notNull()
            idColumn.primaryKey()
            idColumns[dao] = idColumn

            tables[dao] = table
            this.columns[dao] = columns

            parseTreeBuilders[dao] = ParseTreeBuilder.generate(dao)
        }

        private fun <T : Dao> addColumn(table: DbTable, property: KProperty1<T, *>): DbColumn {
            return when {
                property.returnType.isDataType() -> addDataColumn(table, property)
                property.returnType.isDao() -> addOneToManyColumn(table, property)
                else -> throw DaoInvalidColumnTypeException(
                        "DAO property is not a valid data type or DAO: Property ${property.name} is type ${property.returnType} in ${table.name}")
            }
        }

        private fun <T : Dao> addOneToManyColumn(table: DbTable, property: KProperty1<T, *>): DbColumn {
            val name = property.name.toLowerCase() + "_id_otm"
            val column = table.addColumn(name, "BIGINT", null)

            if (!property.returnType.isMarkedNullable) {
                column.notNull()
            }

            val daoProperty = property.toDaoClass()
            val otherTable = daoProperty.dbTable
            val foreignKeyName = "foreign_key_${table.name}_${otherTable.tableNameSQL}_$name"
            val foreignKey = DbForeignKeyConstraint(column, foreignKeyName, otherTable, "id")
            column.addConstraint(foreignKey)

            return column
        }

        private fun <T : Dao> addDataColumn(table: DbTable, property: KProperty1<T, *>): DbColumn {
            val name = property.name.toLowerCase()

            val type = property.returnType
            val columnType = type.toColumnTypeString()

            val column = when (type) { //This is where the column actually gets added to the table
                String::class.starProjectedType -> addStringColumn(property, table, name, columnType)
                BigDecimal::class.starProjectedType -> addDecimalColumn(property, table, name, columnType)
                else -> table.addColumn(name, columnType, null)
            }

            if (!type.isMarkedNullable) {
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