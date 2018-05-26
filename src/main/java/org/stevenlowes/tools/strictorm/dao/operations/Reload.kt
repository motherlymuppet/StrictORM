package org.stevenlowes.tools.strictorm.dao.operations

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoException
import org.stevenlowes.tools.strictorm.dao.read

fun <T: Dao> T.reload(): T{
    val id = this.id
    if(id == null){
        throw DaoException("Cannot reload when never saved")
    }
    else{
        return this::class.read(id)
    }
}