package org.stevenlowes.tools.strictorm.dao.initialisation

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.KClass

class DaoInitialiser{

    companion object {
        private lateinit var daos: Iterable<KClass<out Dao>>

        fun initialise(daos: Iterable<KClass<out Dao>>){
            this.daos = daos

            if(!databaseExists()){
                daos.forEach { dao ->
                    DaoController.createTable(dao)
                }
            }
        }

        private fun databaseExists(): Boolean{
            return Tx.execute { conn ->
                val rs = conn.metaData.catalogs
                return@execute rs.next()
            }
        }

        fun <T: Dao> isInitialised(clazz: KClass<T>): Boolean{
            return daos.contains(clazz)
        }
    }
}