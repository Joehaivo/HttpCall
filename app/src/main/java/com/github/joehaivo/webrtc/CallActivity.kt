package com.github.joehaivo.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.GsonUtils
import com.github.joehaivo.httpcall.R
import com.github.joehaivo.webrtc.RTCSignalClient.Companion.instance
import com.github.joehaivo.webrtc.RTCSignalClient.OnSignalEventListener
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import java.util.UUID

class CallActivity : AppCompatActivity() {
    private var mLogcatView: TextView? = null
    private var mStartCallBtn: Button? = null
    private var mEndCallBtn: Button? = null
    private var mRootEglBase: EglBase? = null
    private var mPeerConnection: PeerConnection? = null
    private var mPeerConnectionFactory: PeerConnectionFactory? = null
    private var mSurfaceTextureHelper: SurfaceTextureHelper? = null
    private var mLocalSurfaceView: SurfaceViewRenderer? = null
    private var mRemoteSurfaceView: SurfaceViewRenderer? = null
    private var mVideoTrack: VideoTrack? = null
    private var mAudioTrack: AudioTrack? = null
    private var mVideoCapturer: VideoCapturer? = null

    private var sendChannel: DataChannel? = null
    private var  roomName: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        mLogcatView = findViewById(R.id.LogcatView)
        mStartCallBtn = findViewById(R.id.StartCallButton)
        mEndCallBtn = findViewById(R.id.EndCallButton)
        instance?.setSignalEventListener(mOnSignalEventListener)
        val serverAddr = intent.getStringExtra("ServerAddr")
        roomName = intent.getStringExtra("RoomName")
        instance?.joinRoom(serverAddr!!, UUID.randomUUID().toString(), roomName!!)
        mRootEglBase = EglBase.create()
        mLocalSurfaceView = findViewById(R.id.LocalSurfaceView)
        mRemoteSurfaceView = findViewById(R.id.RemoteSurfaceView)
        mLocalSurfaceView?.init(mRootEglBase?.getEglBaseContext(), null)
        mLocalSurfaceView?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        mLocalSurfaceView?.setMirror(true)
        mLocalSurfaceView?.setEnableHardwareScaler(false /* enabled */)
        mRemoteSurfaceView?.init(mRootEglBase?.getEglBaseContext(), null)
        mRemoteSurfaceView?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        mRemoteSurfaceView?.setMirror(false)
        mRemoteSurfaceView?.setEnableHardwareScaler(true /* enabled */)
        mRemoteSurfaceView?.setZOrderMediaOverlay(true)
        val videoSink = ProxyVideoSink()
        videoSink.setTarget(mLocalSurfaceView)
        mPeerConnectionFactory = createPeerConnectionFactory(this)

        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
        mVideoCapturer = createVideoCapturer()
        mSurfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", mRootEglBase?.getEglBaseContext())
        val videoSource = mPeerConnectionFactory?.createVideoSource(false)
        mVideoCapturer?.initialize(
            mSurfaceTextureHelper,
            applicationContext,
            videoSource?.capturerObserver
        )
        mVideoTrack = mPeerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        mVideoTrack?.setEnabled(true)
        mVideoTrack?.addSink(videoSink)
        val audioSource = mPeerConnectionFactory?.createAudioSource(MediaConstraints())
        mAudioTrack = mPeerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        mAudioTrack?.setEnabled(true)

        mRemoteSurfaceView?.setOnTouchListener { v, event ->

            return@setOnTouchListener true
        }
    }

    override fun onResume() {
        super.onResume()
        mVideoCapturer?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS)
    }

    override fun onPause() {
        super.onPause()
        try {
            mVideoCapturer?.stopCapture()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        doEndCall()
        mLocalSurfaceView?.release()
        mRemoteSurfaceView?.release()
        mVideoCapturer?.dispose()
        mSurfaceTextureHelper?.dispose()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
        instance?.leaveRoom()
    }

    class ProxyVideoSink : VideoSink {
        private var mTarget: VideoSink? = null
        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (mTarget == null) {
                Log.d(TAG, "Dropping frame in proxy because target is null.")
                return
            }
            mTarget?.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink?) {
            mTarget = target
        }
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            Log.i(TAG, "SdpObserver: onCreateSuccess !")
        }

        override fun onSetSuccess() {
            Log.i(TAG, "SdpObserver: onSetSuccess")
        }

        override fun onCreateFailure(msg: String) {
            Log.e(TAG, "SdpObserver onCreateFailure: $msg")
        }

        override fun onSetFailure(msg: String) {
            Log.e(TAG, "SdpObserver onSetFailure: $msg")
        }
    }

    fun onClickStartCallButton(v: View?) {
        doStartCall()
    }

    fun onClickEndCallButton(v: View?) {
        doEndCall()
    }

    private fun updateCallState(idle: Boolean) {
        runOnUiThread {
            if (idle) {
                mStartCallBtn?.visibility = View.VISIBLE
                mEndCallBtn?.visibility = View.GONE
                mRemoteSurfaceView?.visibility = View.GONE
            } else {
                mStartCallBtn?.visibility = View.GONE
                mEndCallBtn?.visibility = View.VISIBLE
                mRemoteSurfaceView?.visibility = View.VISIBLE
            }
        }
    }

    data class CmdOfferData(val type: String, val sdp: String)
    data class CmdOffer(val cmd: String, val data: CmdOfferData)
    fun doStartCall() {
        logcatOnUI("Start Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
//        mediaConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        mPeerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.i(TAG, "Create local offer success: \n" + sessionDescription.description)
                mPeerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
//                val message = JSONObject()
                try {
//                    message.put("userId", instance?.userId)
//                    message.put("msgType", RTCSignalClient.MESSAGE_TYPE_OFFER)
//                    message.put("sdp", sessionDescription.description)

                    val cmdOfferData = CmdOfferData("offer", sessionDescription.description)
                    val offer = CmdOffer("offer", cmdOfferData)
                    instance?.sendRtcMessage(GsonUtils.toJson(offer))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, mediaConstraints)
    }

    fun doEndCall() {
        logcatOnUI("End Call, Wait ...")
        hanup()
        val message = JSONObject()
        try {
            message.put("userId", instance?.userId)
            message.put("msgType", RTCSignalClient.MESSAGE_TYPE_HANGUP)
            instance?.sendMessage(message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun doAnswerCall() {
        logcatOnUI("Answer Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        val sdpMediaConstraints = MediaConstraints()
        Log.i(TAG, "Create answer ...")
        mPeerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.i(TAG, "Create answer success !")
                mPeerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("userId", instance?.userId)
                    message.put("msgType", RTCSignalClient.MESSAGE_TYPE_ANSWER)
                    message.put("sdp", sessionDescription.description)
                    instance?.sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, sdpMediaConstraints)
        updateCallState(false)
    }

    private fun hanup() {
        logcatOnUI("Hanup Call, Wait ...")
        if (mPeerConnection == null) {
            return
        }
        mPeerConnection?.close()
        mPeerConnection = null
        logcatOnUI("Hanup Done.")
        updateCallState(true)
    }

    fun createPeerConnection(): PeerConnection? {
        Log.i(TAG, "Create PeerConnection ...")
        val defaultStunServer: PeerConnection.IceServer by lazy {
            PeerConnection.IceServer
                .builder("turn:202.104.173.101:3478")
                .setUsername("1703679448:heyxiaogui")
                .setPassword("l2TTtDogSXOBJ7q8U6rLMWYMdII=")
                .createIceServer()
        }
        val configuration = RTCConfiguration(arrayListOf(defaultStunServer))
        val connection =
            mPeerConnectionFactory?.createPeerConnection(configuration, mPeerConnectionObserver)
        if (connection == null) {
            Log.e(TAG, "Failed to createPeerConnection !")
            return null
        }
        connection.addTrack(mVideoTrack)
        connection.addTrack(mAudioTrack)
        return connection
    }

    fun createPeerConnectionFactory(context: Context?): PeerConnectionFactory {
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            mRootEglBase?.eglBaseContext, false /* enableIntelVp8Encoder */, true
        )
        decoderFactory = DefaultVideoDecoderFactory(mRootEglBase?.eglBaseContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
        builder.setOptions(null)
        return builder.createPeerConnectionFactory()
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     **/
    private fun createVideoCapturer(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(this)) {
            createCameraCapturer(Camera2Enumerator(this))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    data class CmdCandidateData(val id: String, val label: Int, val candidate: String, val type: String)

    data class CmdCandidate(val cmd: String, val data: CmdCandidateData)

    private val mPeerConnectionObserver: PeerConnection.Observer =
        object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: SignalingState) {
                Log.i(TAG, "onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                Log.i(TAG, "onIceConnectionChange: $iceConnectionState")
                if (iceConnectionState == IceConnectionState.CONNECTED) {
                    sendChannel = mPeerConnection?.createDataChannel("fileTransfer$roomName", DataChannel.Init())
                    Log.i(TAG, "createDataChannel: $sendChannel")
                }
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                Log.i(TAG, "onIceConnectionChange: $b")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                Log.i(TAG, "onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                Log.i(TAG, "onIceCandidate: $iceCandidate")
                try {
                    val candidateData = CmdCandidateData(
                        iceCandidate.sdpMid,
                        iceCandidate.sdpMLineIndex,
                        iceCandidate.sdp,
                        "candidate"
                    )
                    val candidate = CmdCandidate("candidate", candidateData)
//                    val message = JSONObject()
//                    message.put("userId", instance?.userId)
//                    message.put("msgType", RTCSignalClient.MESSAGE_TYPE_CANDIDATE)
//                    message.put("label", iceCandidate.sdpMLineIndex)
//                    message.put("id", iceCandidate.sdpMid)
//                    message.put("candidate", iceCandidate.sdp)
                    instance?.sendRtcMessage(GsonUtils.toJson(candidate))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                for (i in iceCandidates.indices) {
                    Log.i(TAG, "onIceCandidatesRemoved: " + iceCandidates[i])
                }
                mPeerConnection?.removeIceCandidates(iceCandidates)

            }

            override fun onAddStream(mediaStream: MediaStream) {
                Log.i(TAG, "onAddStream: " + mediaStream.videoTracks.size)
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                Log.i(TAG, "onRemoveStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                Log.i(TAG, "onDataChannel + $dataChannel")

            }

            override fun onRenegotiationNeeded() {

                Log.i(TAG, "onRenegotiationNeeded")
            }

            override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
                val track = rtpReceiver.track()
                if (track is VideoTrack) {
                    Log.i(TAG, "onAddVideoTrack")
                    val remoteVideoTrack = track
                    remoteVideoTrack.setEnabled(true)
                    val videoSink = ProxyVideoSink()
                    videoSink.setTarget(mRemoteSurfaceView)
                    remoteVideoTrack.addSink(videoSink)
                }
            }
        }
    private val mOnSignalEventListener: OnSignalEventListener = object : OnSignalEventListener {
        override fun onConnected() {
            logcatOnUI("Signal Server Connected !")
        }

        override fun onConnecting() {
            logcatOnUI("Signal Server Connecting !")
        }

        override fun onDisconnected() {
            logcatOnUI("Signal Server Connecting !")
        }

        override fun onRemoteUserJoined(userId: String?) {
            logcatOnUI("Remote User Joined: $userId")
        }

        override fun onRemoteUserLeft(userId: String?) {
            logcatOnUI("Remote User Leaved: $userId")
        }

        override fun onBroadcastReceived(message: JSONObject?) {
            Log.i(TAG, "onBroadcastReceived: $message")
            try {
                val userId = message?.getString("userId")
                val type = message?.getInt("msgType")
                when (type) {
                    RTCSignalClient.MESSAGE_TYPE_OFFER -> onRemoteOfferReceived(userId!!, message)
                    RTCSignalClient.MESSAGE_TYPE_ANSWER -> onRemoteAnswerReceived(userId!!, message)
                    RTCSignalClient.MESSAGE_TYPE_CANDIDATE -> onRemoteCandidateReceived(
                        userId!!,
                        message
                    )

                    RTCSignalClient.MESSAGE_TYPE_HANGUP -> onRemoteHangup(userId!!)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onPodAnswer(sdp: SessionDescription) {
            logcatOnUI("Receive Remote Call ...")
            if (mPeerConnection == null) {
                mPeerConnection = createPeerConnection()
            }
            try {
                mPeerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
//                doAnswerCall()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onPodCandidate(candidate: IceCandidate) {
            mPeerConnection?.addIceCandidate(candidate)
        }

        private fun onRemoteOfferReceived(userId: String, message: JSONObject?) {
            logcatOnUI("Receive Remote Call ...")
            if (mPeerConnection == null) {
                mPeerConnection = createPeerConnection()
            }
            try {
                val description = message?.getString("sdp")
                mPeerConnection?.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.OFFER, description)
                )
                doAnswerCall()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        private fun onRemoteAnswerReceived(userId: String, message: JSONObject?) {
            logcatOnUI("Receive Remote Answer ...")
            try {
                val description = message?.getString("sdp")
                mPeerConnection?.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.ANSWER, description)
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            updateCallState(false)
        }

        private fun onRemoteCandidateReceived(userId: String, message: JSONObject?) {
            logcatOnUI("Receive Remote Candidate ...")
            try {
                val remoteIceCandidate = IceCandidate(
                    message?.getString("id"),
                    message?.getInt("label")!!,
                    message?.getString("candidate")
                )
                mPeerConnection?.addIceCandidate(remoteIceCandidate)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        private fun onRemoteHangup(userId: String) {
            logcatOnUI("Receive Remote Hanup Event ...")
            hanup()
        }
    }

    private fun logcatOnUI(msg: String) {
        Log.i(TAG, msg)
        runOnUiThread {
            val output = """
                ${mLogcatView?.text}
                $msg
                """.trimIndent()
            mLogcatView?.text = output
        }
    }

    companion object {
        private const val VIDEO_RESOLUTION_WIDTH = 1280
        private const val VIDEO_RESOLUTION_HEIGHT = 720
        private const val VIDEO_FPS = 30
        private const val TAG = "CallActivity"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}