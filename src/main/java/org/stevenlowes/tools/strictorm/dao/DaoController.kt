package org.stevenlowes.tools.strictorm.dao

import org.stevenlowes.tools.strictorm.dao.controllers.*
import org.stevenlowes.tools.strictorm.dao.initialisation.DaoInitialiser
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class DaoController {
    companion object {
        private fun <T: Dao> validateDao(clazz: KClass<T>){
            val className = clazz.simpleName

            if(!clazz.isData){
                throw DaoException("DAO is not final - $className")
            }

            if(clazz.isOpen){
                throw DaoException("DAO is open - $className")
            }

            if(!DaoInitialiser.isInitialised(clazz)){
                throw DaoException("DAO was not passed to initialised - $className")
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

        fun <T: Dao> createTable(clazz: KClass<T>){
            validateDao(clazz)
            CreateTableController.createTable(clazz)
        }

        fun dropAll(){
            Tx.execute("DROP ALL OBJECTS")
        }

        fun <T: Dao> loadById(id: Long, clazz: KClass<T>): T{
            validateDao(clazz)
            return LoadByIdController.load(id, clazz)
        }

        fun <T: Dao> createObject(obj: T): T {
            validateDao(obj::class)
            return CreateObjectController.create(obj)
        }

        fun <T: Dao> updateObject(obj: T) {
            validateDao(obj::class)
            return UpdateObjectController.update(obj)
        }

        fun <T: Dao> deleteObject(obj: T) {
            validateDao(obj::class)
            return DeleteObjectController.delete(obj)
        }

        fun <T: Dao> list(clazz: KClass<T>, pagination: Pagination? = null, order: Order? = null): List<T>{
            validateDao(clazz)
            return ListController.list(clazz, pagination, order)
        }
    }
}