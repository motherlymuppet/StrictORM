package org.stevenlowes.tools.strictorm.dao.controllers

import org.stevenlowes.tools.strictorm.dao.utils.ResultSetUtils
import org.stevenlowes.tools.strictorm.database.Tx
import kotlin.reflect.KClass

class ListController{
    companion object {
        fun <T: Any> list(clazz: KClass<T>, pagination: Pagination?, order: Order?): List<T> {
            val tableName = clazz.simpleName

            val orderString = order?.string ?: ""
            val paginationString = pagination?.string ?: ""

            val sql = "SELECT * FROM $tableName $orderString $paginationString;"

            return Tx.executeQuery(sql, { ResultSetUtils.readList(it, clazz) })
        }
    }
}

data class Pagination(val page: Int, val countPerPage: Int){
    val string: String get() = "LIMIT $countPerPage OFFSET $offset"

    val offset: Int = page * countPerPage
}

data class Order(val fieldName: String, val direction: OrderDirection){
    val string: String get() = "ORDER BY $fieldName ${direction.string}"
}

enum class OrderDirection(val string: String){
    ASCENDING("ASC"),
    DESCENDING("DESC");
}