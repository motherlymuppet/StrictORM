package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.initialisation.DaoInitialiser
import org.stevenlowes.tools.strictorm.dao.operations.save
import org.stevenlowes.tools.strictorm.dao.read

fun main(args: Array<String>){
    DaoInitialiser.initialise(Consultant::class)

    val consultant = Consultant("Mr. name").save()
    println(consultant) //Create a consultant and save it

    val consultant2 = Consultant("Mrs. Name", consultant.id)
    println(consultant2) //Create a consultant that is the same as consultant 1, but don't save it

    val consultant3 = Consultant::class.read(consultant.id)
    println(consultant3) //Read from database. Should be same as consultant 1

    consultant2.save() //Save consultant 2. Overwrites consultant 1.
    val consultant4 = Consultant::class.read(consultant.id)
    println(consultant4) //Read from database. Should be same as consultant 2.
}