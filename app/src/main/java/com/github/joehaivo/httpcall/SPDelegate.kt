package com.github.joehaivo.httpcall

import com.blankj.utilcode.util.SPUtils
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SPDelegate<T>(private val key: String, private val defaultValue: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when (defaultValue) {
            is Int -> SPUtils.getInstance().getInt(key, defaultValue)
            is Long -> SPUtils.getInstance().getLong(key, defaultValue)
            is Float -> SPUtils.getInstance().getFloat(key, defaultValue)
            is Boolean -> SPUtils.getInstance().getBoolean(key, defaultValue)
            is String -> SPUtils.getInstance().getString(key, defaultValue)
            is Set<*> -> SPUtils.getInstance().getStringSet(key, defaultValue as? Set<String>)
            else -> ""
        } as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        when (value) {
            is Int -> SPUtils.getInstance().put(key, value)
            is Long -> SPUtils.getInstance().put(key, value)
            is Float -> SPUtils.getInstance().put(key, value)
            is Boolean -> SPUtils.getInstance().put(key, value)
            is String -> SPUtils.getInstance().put(key, value)
            is Set<*> -> SPUtils.getInstance().put(key, value.map { it.toString() }.toHashSet())
            else -> ""
        }
    }
}