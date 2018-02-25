package org.stevenlowes.tools.strictorm.dao.utils

import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class ResultSetUtils{
    companion object {
        fun <T : Any> readObject(rs: ResultSet, clazz: KClass<T>): T {
            return readList(rs, clazz).first()
        }

        fun <T: Any> readList(rs: ResultSet, clazz: KClass<T>): List<T> {
            val constructor = clazz.primaryConstructor!!
            val tableName = clazz.simpleName
            val columns = clazz.memberProperties.map { "$tableName.${it.name}" }

            return buildSequence {
                while(rs.next()){
                    val params = buildSequence {
                        columns.forEach {
                            yield(rs.getObject(it))
                        }
                    }.toList()

                    yield(constructor.call(*params.toTypedArray()))
                }
            }.toList()
        }
    }
}