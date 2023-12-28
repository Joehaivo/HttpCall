package com.github.joehaivo.webrtc

import androidx.annotation.Keep

@Keep
data class MouseMsg(
    val type: Int = DataChannelMsgType.MouseEvent.v,
    val data: MouseData
) {
    @Keep
    data class MouseData(
        val ActiveType: Int? = MouseEventType.DragCancel.v,
        val x: Float?,
        val y: Float?,
        val InterfaceWidth: Int?,
        val InterfaceHigh: Int?,
    )
}