package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoCompanion
import org.stevenlowes.tools.strictorm.dao.StringLength

data class Consultant(
        @StringLength(20) val name: String,
        override val id: Long = -1
                     ) : Dao {

    companion object : DaoCompanion<Consultant>(Consultant::class)
}