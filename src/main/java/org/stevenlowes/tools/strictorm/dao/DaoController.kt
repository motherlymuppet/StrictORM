package org.stevenlowes.tools.strictorm.dao

import org.h2.command.dml.Delete
import org.stevenlowes.tools.strictorm.Consultant
import org.stevenlowes.tools.strictorm.dao.controllers.*
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class DaoController {
    companion object {
        fun validateDao(clazz: KClass<*>){
            val className = clazz.simpleName;

            if(clazz.annotations.map { it.annotationClass }.none { it == Dao::class }){
                throw DaoException("$className is not annotated DAO")
            }

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

        fun <T: Any> updateObject(obj: T) {
            validateDao(obj::class)
            return UpdateObjectController.update(obj)
        }

        fun <T: Any> deleteObject(obj: T) {
            validateDao(obj::class)
            return DeleteObjectController.delete(obj)
        }

        fun <T: Any> list(clazz: KClass<T>, pagination: Pagination? = null, order: Order? = null): List<T>{
            validateDao(clazz)
            return ListController.list(clazz, pagination, order)
        }
    }
}

fun main(args: Array<String>){
    DaoController.dropAll()
    DaoController.createTable(Consultant::class)
    val createObject = DaoController.createObject(Consultant(-1, "Mr. name"))
    println(createObject)

    val updateObject = Consultant(createObject.id, "Mrs. Name (2)")

    val selectObjectOne = DaoController.loadById(createObject.id, Consultant::class)
    DaoController.updateObject(updateObject)

    val selectObjectTwo = DaoController.loadById(createObject.id, Consultant::class)

    println(selectObjectOne)
    println(selectObjectTwo)

    val list = DaoController.list(Consultant::class)
    println(list)

    DaoController.deleteObject(selectObjectTwo)
    val list2 = DaoController.list(Consultant::class)
    println(list2)
}