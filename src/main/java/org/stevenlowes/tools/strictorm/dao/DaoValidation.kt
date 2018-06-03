package org.stevenlowes.tools.strictorm.dao

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

class DaoValidation{
    companion object {
        fun <T : Dao> verify(clazz: KClass<T>) {
            val name = clazz.qualifiedName
                    ?: throw DaoException("Passed a DAO that was local / anonymous. No name to report. Sorry that this error is pretty useless.")

            if (!clazz.isData)
                throw DaoException("$name is not a data class")

            if (clazz.isOpen)
                throw DaoException("$name is open")

            if (clazz.isAbstract)
                throw DaoException("$name is abstract")

            if (clazz.visibility != KVisibility.PUBLIC)
                throw DaoException("$name is not public")

            if (clazz.isCompanion)
                throw DaoException("$name is a companion class")

            if (clazz.isSealed)
                throw DaoException("$name is a sealed class")

            if (clazz.isInner)
                throw DaoException("$name is an inner class")

            if (clazz.typeParameters.isNotEmpty())
                throw DaoException("$name has type parameters")

            if (clazz.supertypes.size != 2){
                throw DaoException("$name extends something other than \"Dao\" and \"Any\"")
            }

            val constructor = clazz.primaryConstructor
                    ?: throw DaoException("$name does not have a primary constructor")

            val properties = clazz.declaredMemberProperties

            properties.firstOrNull { it.name == "id" && it.returnType == Int::class.starProjectedType && !it.returnType.isMarkedNullable }
                    ?: throw DaoException("There must be a property \"val id: Int\".")

            verifyConstructor(constructor, name)

            properties.forEach { prop ->
                verifyProperty(prop, name)
            }
        }

        private fun <T : Dao> verifyProperty(property: KProperty1<T, *>, daoName: String) {
            val name = property.name

            if (property.isAbstract)
                throw DaoException("$name declares an abstract property (in $daoName)")

            if (property.isOpen && property.name != "id")
                throw DaoException("$name declares an open property (in $daoName)")

            if (property.isLateinit)
                throw DaoException("$name declares a lateinit property (in $daoName)")

            if (property.visibility != KVisibility.PUBLIC)
                throw DaoException("$name must be public in $daoName")

            if (name.endsWith("_otm"))
                throw DaoException("$name ends with \"_otm\", which is not allowed (in $daoName)")

            if(!(ValidTypes.isValid(property.returnType)))
                throw DaoException("$name in $daoName is not a valid type")
        }

        private fun <T : Dao> verifyConstructor(constructor: KFunction<T>,
                                                daoName: String) {
            if (constructor.isExternal)
                throw DaoException("The primary constructor in $daoName is external")

            if (constructor.isInfix)
                throw DaoException("The primary constructor in $daoName is infix")

            if (constructor.isInline)
                throw DaoException("The primary constructor in $daoName is inline")

            if (constructor.isAbstract)
                throw DaoException("The primary constructor in $daoName is abstract")

            if (constructor.isOpen)
                throw DaoException("The primary constructor in $daoName is open")

            if (constructor.typeParameters.isNotEmpty())
                throw DaoException("The primary constructor in $daoName takes type parameters. Is that even possible?! Don't do that.")

            if (constructor.visibility != KVisibility.PUBLIC)
                throw DaoException("The primary constructor in $daoName is not public")
        }

        private fun <T : Dao> verifyIdProp(property: KProperty1<T, *>, daoName: String) {
            if (property.name != "id")
                throw DaoException("$daoName does not declare ID property last")

            if (property.returnType != Int::class.starProjectedType)
                throw DaoException("The ID property in $daoName is not a Int")

            if (property.returnType.isMarkedNullable)
                throw DaoException("The ID property in $daoName is nullable")
        }
    }
}