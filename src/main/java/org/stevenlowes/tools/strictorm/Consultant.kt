package org.stevenlowes.tools.strictorm

import org.stevenlowes.tools.strictorm.dao.Dao

data class Consultant(val name: String, override val id: Long = -1): Dao