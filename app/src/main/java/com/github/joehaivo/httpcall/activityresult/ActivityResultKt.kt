package com.github.joehaivo.httpcall.activityresult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

const val INTENT_EXTRA = "_extra_"

fun <PI : Parcelable?, PO : Parcelable?> ComponentActivity.registerResult(
    activityClass: Class<*>,
    onResult: (resultCode: Int, out: PO?) -> Unit
): ActivityResultLauncher<PI?> {
    return registerForActivityResult(StartActivityForResultContract(activityClass)) { r ->
        val ext = r.data?.getParcelableExtra(INTENT_EXTRA) as? PO
        onResult(r.resultCode, ext)
    }
}

class StartActivityForResultContract<T : Parcelable?>(private val activityClass: Class<*>) :
    ActivityResultContract<T, ActivityResult>() {
    override fun createIntent(context: Context, input: T): Intent {
        val intent = Intent(context, activityClass)
        intent.putExtra(INTENT_EXTRA, input)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult = ActivityResult(resultCode, intent)
}

fun <PI : Parcelable?> ComponentActivity.getExtra(): PI? = intent.getParcelableExtra(INTENT_EXTRA) as? PI

fun <PO : Parcelable> ComponentActivity.setResult(resultCode: Int, out: PO? = null) {
    val intent = Intent()
    intent.putExtra(INTENT_EXTRA, out)
    setResult(resultCode, intent)
}