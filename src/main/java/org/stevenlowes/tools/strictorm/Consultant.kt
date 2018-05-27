package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.Dao
import org.stevenlowes.tools.strictorm.dao.DaoCompanion

data class Consultant(val name: String, override val id: Long = -1): Dao{
    companion object: DaoCompanion<Consultant>(Consultant::class)
}