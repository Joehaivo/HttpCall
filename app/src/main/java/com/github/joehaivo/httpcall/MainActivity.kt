package com.github.joehaivo.httpcall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.joehaivo.httpcall.activityresult.ActivityA
import com.github.joehaivo.httpcall.databinding.ActivityMainBinding
import com.github.joehaivo.httpcall.ipc.EchoServiceActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

//    private val json1 = Json { encodeDefaults = true }

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
                    binding.tvContent.text = "${response1.exception} #\n\n${response2.exception}"
                }
            }
        }
        binding.btnDialog.setOnClickListener {
            lifecycleScope.launch {
                showDialog("签到活动", "签到领10000币")
                showDialogOnce("新手任务", "做任务领20000币")
                showDialog("首充奖励", "首充6元送神装")
            }
        }
        binding.btnSpRead.setOnClickListener {
            binding.tvContent.text = " -${SPool.isDarkMode} - ${SPool.userName}"
//            binding.tvContent.text = "${SPool.Global.userId} - ${binding.etSpId.text.toString().toLong()} ${SPool.User.name}"

        }
        binding.btnSpWrite.setOnClickListener {
            binding.tvContent.text = ""
//            SPool.userId = binding.etSpId.text.toString().toLong()
            SPool.userName = binding.etSpName.text.toString()
            SPool.isDarkMode = !SPool.isDarkMode

            val isDarkMode = SPool.isDarkMode // 读
            SPool.isDarkMode = !SPool.isDarkMode // 写
        }
        binding.podId.setOnClickListener {
            val serialno = CloudDeviceUtils.getPodId()
            ToastUtils.showShort("podId=$serialno")
            Log.d("TAG", "onCreate: serialno     =$serialno")
        }

        binding.btnGson.setOnClickListener {
            var bannerVo: BannerVo
            var bannerJson: String
            val millis = measureTimeMillis {
                repeat(100000) { i ->
                    bannerJson = GsonUtils.toJson(BannerVo(desc = "desc$i"))
                    bannerVo = GsonUtils.fromJson(bannerJson, BannerVo::class.java)
                    if (i % 10000 == 0) {
                        Log.d("TAG", "gson encode:$bannerJson, decode:$bannerVo")
                    }
                }
            }
            ToastUtils.showLong("Gson spend ${millis}ms")
        }
        binding.btnSerial.setOnClickListener {
//            val json2 = Json { encodeDefaults = true }
            val millis = measureTimeMillis {
                var bannerVo: BannerVo
                var bannerJson: String
                repeat(100000) { i ->
//                    bannerJson = json2.encodeToString(BannerVo(desc = "desc$i"))
//                    bannerVo = json2.decodeFromString(bannerJson)
//                    if (i % 10000 == 0) {
//                        Log.d("TAG", "ktJson encode:$bannerJson, decode:$bannerVo")
//                    }
                }
            }
            ToastUtils.showLong("ktJson spend ${millis}ms")
        }
        binding.waveView.progress = 80
        binding.waveView.binding.tvName.setTextColor(if (binding.waveView.progress >= 90) getColor(R.color.white) else getColor(R.color.t1_color))
        binding.waveView.binding.tvName.text = "内存"
//        binding.waveView.binding.tvProgress.setTextColor(if (binding.waveView.progress > 10) getColor(R.color.white) else getColor(R.color.t1_color))
        binding.waveView.binding.tvProgress.text = "80%"

        binding.btnConnectService.setOnClickListener {
            EchoServiceActivity.startSelf(this)
        }
        binding.btnActivityResult.setOnClickListener {
            ActivityA.startSelf(this)
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

    suspend fun showDialogOnce(title: String, content: String) = suspendCancellableCoroutine { continuation ->
        val showed = SPUtils.getInstance().getBoolean(title)
        if (showed) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }
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
        SPUtils.getInstance().put(title, true)
    }
}