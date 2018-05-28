package org.stevenlowes.tools.strictorm.dao

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DecimalPrecision(val scale: Int, val precision: Int)