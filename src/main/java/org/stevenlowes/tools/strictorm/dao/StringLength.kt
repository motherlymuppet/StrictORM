package org.stevenlowes.tools.strictorm.dao

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class StringLength(val length: Int)