package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.Dao

@Dao
data class Consultant(val id: Long, val name: String)