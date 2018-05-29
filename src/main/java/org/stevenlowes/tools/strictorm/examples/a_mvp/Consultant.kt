package org.stevenlowes.tools.strictorm.examples.a_mvp

import org.stevenlowes.tools.strictorm.dao.*

data class Consultant(@StringLength(20) val name: String, override val id: Long = -1) : Dao {
    companion object : DaoCompanion<Consultant>(Consultant::class)
}

fun main(args: Array<String>){
    DaoInitialiser.initialise(Consultant::class)
    DaoInitialiser.createTables()
    val consultant = Consultant("Mr. name").save()
    val consultant2 = Consultant.read(consultant.id)
}