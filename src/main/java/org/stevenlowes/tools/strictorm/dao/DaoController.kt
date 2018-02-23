package org.stevenlowes.tools.strictorm.dao

import org.stevenlowes.tools.strictorm.Consultant
import org.stevenlowes.tools.strictorm.dao.controllers.CreateObjectController
import org.stevenlowes.tools.strictorm.dao.controllers.CreateTableController
import org.stevenlowes.tools.strictorm.dao.controllers.LoadByIdController
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class DaoController {
    companion object {
        fun validateDao(clazz: KClass<*>){
            val className = clazz.simpleName;

            if(!clazz.isData){
                throw DaoException("DAO is not final - $className")
            }

            if(clazz.isOpen){
                throw DaoException("DAO is open - $className")
            }

            val visibility = clazz.visibility
            if(visibility != KVisibility.PUBLIC){
                throw DaoException("DAO is not public - $className")
            }

            val fields = clazz.memberProperties;
            if(fields.any { it.isOpen }){
                throw DaoException("DAO contains non-final fields - $className")
            }

            if(fields.none { it.name == "id" && it.returnType == Long::class.starProjectedType}){
                throw DaoException("DAO does not contain \"val id: Long\" field")
            }
        }

        fun createTable(clazz: KClass<*>){
            validateDao(clazz)
            CreateTableController.createTable(clazz)
        }

        fun dropAll(){
            Tx.execute("DROP ALL OBJECTS")
        }

        fun <T: Any> loadById(id: Long, clazz: KClass<T>): T{
            validateDao(clazz)
            return LoadByIdController.load(id, clazz)
        }

        fun <T: Any> createObject(obj: T): T {
            validateDao(obj::class)
            return CreateObjectController.create(obj)
        }
    }
}

fun main(args: Array<String>){
    DaoController.dropAll()
    DaoController.createTable(Consultant::class)
    val createObject = DaoController.createObject(Consultant(-1, "Name"))
    DaoController.loadById(createObject.id, Consultant::class)
}