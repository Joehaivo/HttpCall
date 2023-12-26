package com.github.joehaivo.httpcall.activityresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.joehaivo.httpcall.databinding.ActivityFromBinding

class ActivityA : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun startSelf(context: Context) {
            val starter = Intent(context, ActivityA::class.java)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityFromBinding

    val activityLauncher = registerResult<InExtra, OutExtra>(ActivityB::class.java) { resultCode, out ->

    }

    val activityLauncher2 = registerResult<InExtra, OutExtra>(ActivityB::class.java) { resultCode, out ->

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFromBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tv1.setOnClickListener {
            activityLauncher.launch(InExtra(studentId = "123"))
        }
    }
}