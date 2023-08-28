package com.github.joehaivo.httpcall


import androidx.annotation.Keep

@Keep
data class BannerVo(
    val desc: String? = "",
    val id: Int? = null,
    val imagePath: String? = "",
    val isVisible: Int? = null,
    val order: Int? = null,
    val title: String? = "",
    val type: Int? = null,
    val url: String? = ""
)
