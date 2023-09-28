package com.github.joehaivo.httpcall

import android.app.Application
import com.tencent.mmkv.MMKV

class APP: Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}