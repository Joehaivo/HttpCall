package com.github.joehaivo.webrtc

import android.util.Log
import com.blankj.utilcode.util.GsonUtils
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException

class RTCSignalClient {
    private var mOnSignalEventListener: OnSignalEventListener? = null
    private var mSocket: Socket? = null
    var userId: String? = null
    private var mRoomName: String? = null

    interface OnSignalEventListener {
        fun onConnected()
        fun onConnecting()
        fun onDisconnected()
        fun onRemoteUserJoined(userId: String?)
        fun onRemoteUserLeft(userId: String?)
        fun onBroadcastReceived(message: JSONObject?)
        fun onPodAnswer(sdp: SessionDescription)
        fun onPodCandidate(candidate: IceCandidate)
    }

    fun setSignalEventListener(listener: OnSignalEventListener?) {
        mOnSignalEventListener = listener
    }

    fun joinRoom(url: String, userId: String, roomName: String) {
        Log.i(TAG, "joinRoom: $url, $userId, $roomName")
        try {
            val options = IO.Options().apply {
                transports = arrayOf(WebSocket.NAME, Polling.NAME)
                query =
                    "auth=aaa&room=52994d01-344f-4f6e-93c7-8a2a9e5deb34"
//                query = GsonUtils.toJson(
//                    Query(
//                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MTExMTQ4LCJwd2QiOiIxMjM0NTY3cSIsImxvZ2luVHlwZSI6MCwiaWF0IjoxNzAzNTYyMTExLCJleHAiOjE3MTEzMzgxMTF9.TWEmunpqyP_kgtgRjs2mhE6VZ_ba6taQ_OHkg5K6Xdg",
//                        "52994d01-344f-4f6e-93c7-8a2a9e5deb34"
//                    )
//                )
            }
            mSocket = IO.socket(url, options)
            mSocket?.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        this.userId = userId
        mRoomName = roomName
        listenSignalEvents()
        try {
            val args = JSONObject()
            args.put("auth", userId)
            args.put("room", roomName)
            mSocket?.emit("join-room", args.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun leaveRoom() {
        Log.i(TAG, "leaveRoom: $mRoomName")
        if (mSocket == null) {
            return
        }
        try {
            val args = JSONObject()
            args.put("userId", userId)
            args.put("roomName", mRoomName)
            mSocket?.emit("leave-room", args.toString())
            mSocket?.close()
            mSocket = null
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendMessage(message: JSONObject) {
        Log.i(TAG, "broadcast: $message")
        if (mSocket == null) {
            return
        }
        mSocket?.emit("broadcast", message)
    }

    fun sendRtcMessage(message: String) {
        Log.i(TAG, "sendRtcMessage: $message")
        mSocket?.emit("rtcmessage", message)
    }

    private fun listenSignalEvents() {
        if (mSocket == null) {
            return
        }
        mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "onConnectError: " + GsonUtils.toJson(args))
        }
        mSocket?.on(Socket.EVENT_ERROR) { args ->
            Log.e(TAG, "onError: " + GsonUtils.toJson(args))
        }
        mSocket?.on(Socket.EVENT_CONNECT) { args ->
            val sessionId = mSocket?.id()
            Log.i(TAG, "onConnected: " + GsonUtils.toJson(args))
                mOnSignalEventListener?.onConnected()

        }
        mSocket?.on(Socket.EVENT_CONNECTING) { args ->
            Log.i(TAG, "onConnecting" + GsonUtils.toJson(args))
                mOnSignalEventListener?.onConnecting()
        }
        mSocket?.on(Socket.EVENT_DISCONNECT) { args ->
            Log.i(TAG, "onDisconnected" + GsonUtils.toJson(args))
                mOnSignalEventListener?.onDisconnected()
        }
        mSocket?.on("user-joined", object : Emitter.Listener {
            override fun call(vararg args: Any) {
                Log.i(TAG, "user-joined" + GsonUtils.toJson(args))
                val userId = args[0] as String
                if (userId != userId && mOnSignalEventListener != null) {
                    mOnSignalEventListener?.onRemoteUserJoined(userId)
                }
                Log.i(TAG, "onRemoteUserJoined: $userId")
            }
        })
        mSocket?.on("user-left", object : Emitter.Listener {
            override fun call(vararg args: Any) {
                Log.i(TAG, "user-left" + GsonUtils.toJson(args))
                val userId = args[0] as String
                if (userId != userId && mOnSignalEventListener != null) {
                    mOnSignalEventListener?.onRemoteUserLeft(userId)
                }
            }
        })
        mSocket?.on("broadcast", object : Emitter.Listener {
            override fun call(vararg args: Any) {
                Log.i(TAG, "broadcast" + GsonUtils.toJson(args))
                val msg = args[0] as JSONObject
                try {
                    val userId = msg.getString("userId")
                    if (userId != userId && mOnSignalEventListener != null) {
                        mOnSignalEventListener?.onBroadcastReceived(msg)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
        mSocket?.on("rtcmessage") { args ->
            try {
                Log.i(TAG, "onrtcmessage" + args.firstOrNull())
                val rtcCmd = GsonUtils.fromJson(args.firstOrNull().toString(), RtcCmd::class.java)
                when (rtcCmd.cmd) {
                    "answer" -> {
                        val cmdSd = GsonUtils.fromJson(rtcCmd.data.toString(), CmdSd::class.java)
                        val description = SessionDescription(SessionDescription.Type.ANSWER, cmdSd.sdp)
                        mOnSignalEventListener?.onPodAnswer(description)
                    }
                    "candidate" -> {
                        val cmdCandidate = GsonUtils.fromJson(rtcCmd.data.toString(), CmdCandidate::class.java)
                        mOnSignalEventListener?.onPodCandidate(IceCandidate(cmdCandidate.id, cmdCandidate.label, cmdCandidate.candidate))
                    }
                    "bye" -> {}
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    data class RtcCmd<T>(val cmd: String, val data: T)

    data class CmdSd(val type: String, val sdp: String)

    data class CmdCandidate(val id: String, val label: Int, val candidate: String, val type: String)

    companion object {
        private const val TAG = "RTCSignalClient"
        const val MESSAGE_TYPE_OFFER = 0x01
        const val MESSAGE_TYPE_ANSWER = 0x02
        const val MESSAGE_TYPE_CANDIDATE = 0x03
        const val MESSAGE_TYPE_HANGUP = 0x04
        private var mInstance: RTCSignalClient? = null

        @JvmStatic
        val instance: RTCSignalClient?
            get() {
                synchronized(RTCSignalClient::class.java) {
                    if (mInstance == null) {
                        mInstance = RTCSignalClient()
                    }
                }
                return mInstance
            }
    }
}