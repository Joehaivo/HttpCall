package com.github.joehaivo.httpcall

import android.util.Log

object KVPool {
    object Global {
        var adInfo by MMKVDelegate("adInfo", "{}")
    }

    class UserClz {
        val prefix = "userId"
        var id by MMKVDelegate(prefix, "")
        var name by MMKVDelegate("${prefix}_${user.id}_name", "")

    }

    var user: UserClz = UserClz()

}

