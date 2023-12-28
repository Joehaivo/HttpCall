package com.github.joehaivo.webrtc

import android.view.MotionEvent

enum class MouseEventType(val v: Int) {
    LongPress(0),
    DragStart(1),
    DragUpdate(2),
    DragUp(3),
    DragCancel(4),
    LongPressUp(5),
    KeyboardAdd(6),
    KeyboardEnter(7),
    KeyboardDel(8);
}

fun MotionEvent.toMouseEventType(): MouseEventType {
    return when(action) {
        MotionEvent.ACTION_DOWN -> MouseEventType.DragStart
        MotionEvent.ACTION_MOVE -> MouseEventType.DragUpdate
        MotionEvent.ACTION_UP -> MouseEventType.DragUp
        MotionEvent.ACTION_CANCEL -> MouseEventType.DragCancel
        else -> MouseEventType.DragCancel
    }
}