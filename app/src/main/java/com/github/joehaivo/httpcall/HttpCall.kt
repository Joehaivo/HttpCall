package com.github.joehaivo.httpcall

import android.util.Log
import androidx.annotation.Keep
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.net.SocketTimeoutException

suspend fun <R> httpCall(
    showErrorToast: Boolean = true,
    apiExceptionHandler: ApiExceptionHandler? = object : ApiExceptionHandler{
        override fun getShowErrorToast(): Boolean  = showErrorToast
    },
    retrofitBlock: suspend ApiProvider.() -> BaseResponse<R>
): BaseResponse<R> {
    val response = withContext(Dispatchers.IO) {
        try {
            val response = retrofitBlock(ApiHolder.provider)
            if (!response.isOk()) {
                apiExceptionHandler?.handle(response)
            }
            response
        } catch (e: Exception) {
            val defaultErrorResponse = BaseResponse<R>().apply {
                exception = e
            }
            apiExceptionHandler?.handle(defaultErrorResponse)
            defaultErrorResponse
        }
    }
    return response
}

@Keep
class BaseResponse<T>(
    val data: T? = null,
    val errorCode: Int = ErrorCode.INTERNAL_ERROR.code,
    val errorMsg: String? = ""
) {

    @Keep
    enum class ErrorCode(val code: Int) {
        INTERNAL_ERROR(-1),
        SUCCESS(0),
        TOKEN_INVALID(-1001),
    }

    var exception: Exception? = null

    fun isOk() = errorCode == ErrorCode.SUCCESS.code
}

@Keep
interface ApiProvider {
    @GET("/banner/json")
    suspend fun getBanners(): BaseResponse<List<BannerVo>>
}

object ApiHolder {
    private const val BASE_URL = "https://www.wanandroid.com"

    val provider: ApiProvider by lazy { initRetrofit().create(ApiProvider::class.java) }

    private fun initRetrofit(): Retrofit {
        val mBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            mBuilder.addInterceptor(interceptor)
        }
        val builder: Retrofit.Builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(mBuilder.build())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
        return builder.build()
    }
}

// 默认的错误处理程序
interface ApiExceptionHandler {
    val TAG: String
        get() = ApiExceptionHandler::class.java.simpleName

    fun getShowErrorToast(): Boolean

    fun handle(response: BaseResponse<*>) {
        response.exception?.printStackTrace()
        Log.d(TAG, "handle: $response, ${response.exception}")
        if (getShowErrorToast()) {
            if (response.exception != null) {
                ToastUtils.showShort("${response.exception?.localizedMessage}!")
            } else {
                ToastUtils.showShort("${response.errorMsg}(${response.errorCode}).")
            }
        }
        when (response.errorCode) {
            BaseResponse.ErrorCode.INTERNAL_ERROR.code -> {
                when (response.exception) {
                    is SocketTimeoutException -> {

                    }

                    is IOException -> {

                    }
                }
            }

            BaseResponse.ErrorCode.TOKEN_INVALID.code -> {

            }

            else -> {}
        }
    }
}