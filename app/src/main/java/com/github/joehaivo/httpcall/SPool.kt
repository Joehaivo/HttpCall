package com.github.joehaivo.httpcall

object SPool {
    object Global {
        var adInfo by SPDelegate("adInfo", "{}")
    }

    object User {
        private const val prefix = "userId"
        var id by SPDelegate(prefix, "")
        var name by SPDelegate("${prefix}_${id}_name", "")
    }
}