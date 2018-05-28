package org.stevenlowes.tools.strictorm.examples.a_mvp

import org.stevenlowes.tools.strictorm.dao.*

data class Consultant(
        @StringLength(20) val name: String,
        override val id: Long = -1
                     ) : Dao {

    companion object : DaoCompanion<Consultant>(Consultant::class)
}

fun main(args: Array<String>){
    DaoInitialiser.initialise(Consultant::class) //Run this before everything else. Pass it the DAOs. TODO scan automatically for DAOs
    //DaoInitialiser.createTables() //Creates the database. Only do this on first run.

    val consultant = Consultant(name = "Mr. name").save()
    println(consultant) //Create a consultant and save it

    val consultant2 = Consultant("Mrs. Name", consultant.id)
    println(consultant2) //Create a consultant that is the same as consultant 1, but don't save it

    val consultant3 = Consultant.read(consultant.id)
    println(consultant3) //Read from database. Should be same as consultant 1

    consultant2.save() //Save consultant 2. Overwrites consultant 1.
    val consultant4 = Consultant.read(consultant.id)
    println(consultant4) //Read from database. Should be same as consultant 2.
}

//TODO STEP 1 - Allow for DAO as field
//TODO STEP 1a - Validation should check that fields are valid types
//TODO STEP 1b - This includes that they should be registered DAOs
//TODO STEP 1c - SELECT should do an inner join
//TODO STEP 1d - What do we do about foreign keys? What about on delete/update? Do we let the user decide? Hopefully not.
//TODO step 1e - INSERT needs to recursively update children
//TODO step 1f - How do we tell whether the children have been inserted already? Maybe everything should just use REPLACE instead of separate INSERT / UPDATE

//TODO STEP 2 - Allow for multiple layers of nested DAO

//TODO STEP 3 - Allow DAO A to have DAO B as parameter which has DAO A as parameter
//TODO STEP 3a. Think this requires table aliases be used

//TODO STEP 4 - MANY-TO-MANY