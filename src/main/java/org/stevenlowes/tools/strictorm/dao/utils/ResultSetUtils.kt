package org.stevenlowes.tools.strictorm.dao.utils

import com.healthmarketscience.sqlbuilder.dbspec.Column
import org.stevenlowes.tools.strictorm.dao.DaoException
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class ResultSetUtils{
    companion object {
        fun <T : Any> readObject(rs: ResultSet, clazz: KClass<T>, columns: List<Column>): T {
            return readList(rs, clazz, columns).first()
        }

        fun <T: Any> readList(rs: ResultSet, clazz: KClass<T>, columns: List<Column>): List<T> {
            val constructor = clazz.primaryConstructor ?: throw DaoException("DAO does not have primary constructor")
            return buildSequence {
                while(rs.next()){
                    val params = buildSequence {
                        columns.forEach { column ->
                            yield(rs.getObject(column.columnNameSQL))
                        }
                    }.toList().toTypedArray()

                    yield(constructor.call(*params))
                }
            }.toList()
        }
    }
}