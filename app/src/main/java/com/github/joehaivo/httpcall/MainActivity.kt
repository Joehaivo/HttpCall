package com.github.joehaivo.httpcall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ImageUtils
import com.github.joehaivo.httpcall.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.internal.wait
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSingle.setOnClickListener {
            lifecycleScope.launch {
                val response = httpCall { getBanners() }
                if (response.isOk()) {
                    binding.tvContent.text = response.data.toString()
                } else {
                    binding.tvContent.text = response.exception?.toString()
                }
            }
        }
        binding.btnAsync.setOnClickListener {
            lifecycleScope.launch {
                val async1 = async { httpCall { getBanners() } }
                val async2 = async { httpCall { getBanners() } }
                val response1 = async1.await()
                val response2 = async2.await()
                if (response1.isOk() && response2.isOk()) {
                    binding.tvContent.text = "${response1.data} #\n\n${response2.data}"
                } else {
                    binding.tvContent.text ="${response1.exception} #\n\n${response2.exception}"
                }
            }
        }
        binding.btnDialog.setOnClickListener {
            lifecycleScope.launch {
                showDialog("签到活动", "签到领10000币")
                showDialog("新手任务", "做任务领20000币")
                showDialog("首充奖励", "首充6元送神装")
            }
        }
    }

    suspend fun showDialog(title: String, content: String) = suspendCancellableCoroutine { continuation ->
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("我知道了") { dialog, which ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                continuation.resume(Unit)
            }
            .show()
    }
}