package com.github.joehaivo.httpcall.activityresult

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import com.github.joehaivo.httpcall.databinding.ActivityToBinding
import kotlinx.parcelize.Parcelize

@Parcelize
data class InExtra(val studentId: String): Parcelable

@Parcelize
data class OutExtra(val studentName: String): Parcelable

class ActivityB : AppCompatActivity() {
    private lateinit var binding: ActivityToBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvInput.text = getExtra<InExtra>().toString()

        binding.tvOk.setOnClickListener {
            val inExtra = getExtra<InExtra>()
            setResult(RESULT_OK, OutExtra(studentName = "飞羽"))
            finish()
        }

        intent.putExtra("key1", "")
    }
}