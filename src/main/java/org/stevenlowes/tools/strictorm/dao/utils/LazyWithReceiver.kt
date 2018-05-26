package org.stevenlowes.tools.strictorm.dao.utils

import java.util.*
import kotlin.reflect.KProperty

class LazyWithReceiver<in This, out Return>(val initializer:This.()->Return)
{
    private val values = WeakHashMap<This,Return>()

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef:Any,property: KProperty<*>):Return = synchronized(values)
    {
        thisRef as This
        return values.getOrPut(thisRef) {thisRef.initializer()}
    }
}