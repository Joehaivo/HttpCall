package com.github.joehaivo.webrtc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.joehaivo.httpcall.R
import com.github.joehaivo.webrtc.CallActivity
import pub.devrel.easypermissions.EasyPermissions

class RTCActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val serverEditText = findViewById<EditText>(R.id.ServerEditText)
        val roomEditText = findViewById<EditText>(R.id.RoomEditText)
        findViewById<View>(R.id.JoinRoomBtn).setOnClickListener {
            val addr = serverEditText.text.toString()
            val roomName = roomEditText.text.toString()
            if ("" != roomName) {
                val intent = Intent(this@RTCActivity, CallActivity::class.java)
                intent.putExtra("ServerAddr", addr)
                intent.putExtra("RoomName", roomName)
                startActivity(intent)
            }
        }
        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(this, "Need permissions for camera & microphone", 0, *perms)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        fun startSelf(context: Context) {
            val starter = Intent(context, RTCActivity::class.java)
            context.startActivity(starter)
        }
    }
}