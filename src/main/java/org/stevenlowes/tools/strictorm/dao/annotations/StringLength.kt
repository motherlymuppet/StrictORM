package org.stevenlowes.tools.strictorm.dao.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class StringLength(val length: Int)