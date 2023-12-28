package com.github.joehaivo.webrtc

enum class DataChannelMsgType(val v: Int) {
    MouseEvent(0),
    KeyBoardEvent(1),
    ShowDirection(2),
    BackToHome(3),
    CloseGame(4),
    Inactive(5),
    Resumed(6),
    Reboot(7),
    ShowIosFloat(9),
    HideIosFloat(10),
    CopyToPhone(11),
// 飞天消息
    ShowFTFloat(101),
    HideFTFloat(102),
    FTRunning(103),
    FTStoped(104),
    ClickFTFloat(105),
    QueryFTStatus(106),
    ShowFTMessage(107),
    Unkown(108),
// 获取当前内存
    GetVmMem(200),
}