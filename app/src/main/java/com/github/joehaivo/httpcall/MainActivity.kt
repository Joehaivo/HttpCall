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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.internal.wait

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

    }
}