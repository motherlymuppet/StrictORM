package org.stevenlowes.tools.strictorm.dao.exceptions

open class DaoException(message: String? = null,
                        cause: Throwable? = null,
                        enableSuppression: Boolean = true,
                        writableStackTrace: Boolean = false) : Exception(message,
                                                                         cause,
                                                                         enableSuppression,
                                                                         writableStackTrace)