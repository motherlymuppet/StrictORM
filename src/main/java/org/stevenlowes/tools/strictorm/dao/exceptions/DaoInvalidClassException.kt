package org.stevenlowes.tools.strictorm.dao.exceptions

open class DaoInvalidClassException(message: String? = null,
                                    cause: Throwable? = null,
                                    enableSuppression: Boolean = true,
                                    writableStackTrace: Boolean = false) : DaoException(message,
                                                                                     cause,
                                                                                     enableSuppression,
                                                                                     writableStackTrace)

class DaoInvalidColumnTypeException(message: String? = null,
                                    cause: Throwable? = null,
                                    enableSuppression: Boolean = true,
                                    writableStackTrace: Boolean = false) : DaoInvalidClassException(message,
                                                                                                    cause,
                                                                                                    enableSuppression,
                                                                                                    writableStackTrace)