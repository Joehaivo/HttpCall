package com.github.joehaivo.httpcall.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.android.ext.echoservice.IEchoAidlInterface
import com.blankj.utilcode.util.ToastUtils
import com.github.joehaivo.httpcall.R
import com.github.joehaivo.httpcall.databinding.ActivityEchoServiceBinding

class EchoServiceActivity : AppCompatActivity() {
    private val TAG = EchoServiceActivity::class.java.simpleName

    companion object {
        @JvmStatic
        fun startSelf(context: Context) {
            val starter = Intent(context, EchoServiceActivity::class.java)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityEchoServiceBinding
    private var echoInterface: IEchoAidlInterface? = null

    private val echoConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binding.tvState.text = "已绑定"
            echoInterface = IEchoAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binding.tvState.text = "已断开"
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEchoServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnectService.setOnClickListener {
            val remotePkg = "com.android.ext.echoservice"
            val intent = Intent("$remotePkg.IEchoAidlInterface")
            intent.setPackage(remotePkg)
            bindService(intent, echoConn, Context.BIND_AUTO_CREATE)
        }

        binding.btnSend.setOnClickListener {
            val slowEcho = echoInterface?.slowEcho(binding.et.text.toString())
            ToastUtils.showLong(slowEcho)
            Log.d(TAG, "onCreate: $slowEcho")
//            echoInterface?.basicTypes(100, 1, false, 23.4f, 13.4, "kkkk")

            val pong = echoInterface?.echo(binding.et.text.toString())
            Log.d(TAG, "onCreate: $pong")
            ToastUtils.showLong(pong)

        }
    }
}