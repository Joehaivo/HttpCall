package com.github.joehaivo.httpcall

import android.annotation.SuppressLint
import android.util.Log

object CloudDeviceUtils {

    const val TAG = "CloudDeviceUtils"


    class SuProp(var su_white_list_index: Int, var packageName: String)

    /**
     * 读取persist.sys.bd.su_white_list..persist.sys.bd.su_white_list.1..persist.sys.bd.su_white_list.31的所有值
     * su_white_list 保存”all“ 或者 ”“
     * su_white_list.1 保存 拓展工具包名
     * su_white_list.2 ... su_white_list.31保存用户的app包名
     */
    fun readSuList(): List<SuProp> {
        val suList = mutableListOf<SuProp>()
        try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            val all = get.invoke(c, "persist.sys.bd.su_white_list") as String?
            suList.add(SuProp(0, all ?: ""))
            for (i in 1..31) {
                val singlePackageName = get.invoke(c, "persist.sys.bd.su_white_list.$i") as String?
                suList.add(SuProp(i, singlePackageName ?: ""))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.i(TAG, "suList $suList")
        return suList
    }

    @SuppressLint("PrivateApi")
    fun getSystemProperties(properties: String): String? {
        var systemProperties: String = ""
        try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            systemProperties = get.invoke(c, properties) as String
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.i(TAG, "systemProperties $systemProperties")
        return systemProperties
    }


    @SuppressLint("PrivateApi")
    fun getPodId(): String? {
        try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            return get.invoke(c, "ro.serialno") as String
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}