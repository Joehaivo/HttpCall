package com.github.joehaivo.httpcall

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.Utils
import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * MMKV委托代理
 * @param context Context
 * @param name
 * @param defaultValue 默认值
 * @param key 存取数据时对应的key
 */
class MMKVDelegate<T>(
    private val key: String? = null,
    private val defaultValue: T,
) : ReadWriteProperty<Any?, T> {
    companion object {
        val TAG = MMKVDelegate::class.simpleName
    }

    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val finalKey = key ?: property.name
        return when (defaultValue) {
            is Int -> mmkv.getInt(finalKey, defaultValue)
            is Long -> mmkv.getLong(finalKey, defaultValue)
            is Float -> mmkv.getFloat(finalKey, defaultValue)
            is Boolean -> mmkv.getBoolean(finalKey, defaultValue)
            is String -> mmkv.getString(finalKey, defaultValue)
            is Set<*> -> mmkv.getStringSet(finalKey, defaultValue as? Set<String>)
            else -> throw IllegalStateException("Unsupported type")
        } as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val finalKey = key ?: property.name
        when (value) {
            is Int -> mmkv.putInt(finalKey, value)
            is Long -> mmkv.putLong(finalKey, value)
            is Float -> mmkv.putFloat(finalKey, value)
            is Boolean -> mmkv.putBoolean(finalKey, value)
            is String -> mmkv.putString(finalKey, value)
            is Set<*> -> mmkv.putStringSet(finalKey, value.map { it.toString() }.toHashSet())
            else -> throw IllegalStateException("Unsupported type")
        }
    }
}