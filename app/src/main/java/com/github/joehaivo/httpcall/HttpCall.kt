package com.github.joehaivo.httpcall

import androidx.annotation.Keep
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

suspend fun <R> httpCall(showErrorToast: Boolean = true, retrofitBlock: suspend ApiProvider.() -> BaseResponse<R>): BaseResponse<R>{
    val response = withContext(Dispatchers.IO) {
        try {
            val response = retrofitBlock(ApiHolder.provider)
            if (!response.isOk()) {
                if (showErrorToast) {
                    ToastUtils.showShort("${response.errorMsg}(${response.errorCode}).")
                }
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            if (showErrorToast) {
                ToastUtils.showShort("${e.message}!")
            }
            BaseResponse<R>().apply {
                exception = e
            }
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

    val provider: ApiProvider by lazy { initService().create(ApiProvider::class.java) }

    private fun initService(): Retrofit {
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
