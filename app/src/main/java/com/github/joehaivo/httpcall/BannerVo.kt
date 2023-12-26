package com.github.joehaivo.httpcall


import androidx.annotation.Keep

@Keep
data class BannerVo(
    val desc: String? = "",
    val id: Int? = 666,
    val imagePath: String? = "",
    val isVisible: Int? = null,
    val order: Int? = null,
    val title: String? = "sss",
    val type: Int? = 5,
    val url: String? = ""
)
