package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.DaoController
import org.stevenlowes.tools.strictorm.dao.initialisation.DaoInitialiser

fun main(args: Array<String>){
    DaoController.dropAll()

    DaoInitialiser.initialise(listOf(Consultant::class))

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