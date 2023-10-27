package com.github.joehaivo.httpcall

object SPool {
    var isDarkMode by SPDelegate("isDarkMode", false)
    var userName by SPDelegate("userName", "")
}