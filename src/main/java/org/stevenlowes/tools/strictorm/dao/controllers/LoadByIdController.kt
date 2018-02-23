package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.database.Tx
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class LoadByIdController {
    companion object {
        fun <T : Any> load(id: Long, clazz: KClass<T>): T{
            if (clazz.annotations.none { it.javaClass == Dao::javaClass }) {
                //Is not annotated as a dao
                throw DaoException("${clazz.simpleName} is not annoted Dao");
            }

            val sql = "SELECT * FROM ${clazz.simpleName} WHERE id = ?"

            return Tx.executeQuery(sql, { readObject(it, clazz) }, id)
        }

        private fun <T : Any> readObject(rs: ResultSet, clazz: KClass<T>): T {
            val constructor = clazz.primaryConstructor!!
            val params = readParams(rs, clazz)
            return constructor.call(params)
        }

        private fun <T : Any> readParams(rs: ResultSet, clazz: KClass<T>): List<Any> {
            val tableName = clazz.simpleName

            val memberProperties = clazz.memberProperties

            return buildSequence {
                memberProperties.forEach { prop ->
                    val columnname = "$tableName.${prop.name}"

                    yield(rs.getObject(columnname))
                }
            }.toList()
        }
    }
}