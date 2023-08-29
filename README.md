# HttpCall
retrofit封装函数
## 0. 使用示例


![carbon (8).png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4b457540bef74de48f6a2fb7511dbe73~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=1386&h=670&e=png&b=273339)


## 1. 大道至简
- 不绑定`coroutineScope`,因为网络调用会出现在Activity、Fragment、ViewModel、Dialog甚至GlobalScope中, 至于原因吗..
- 采用成熟的`Retrofit式API接口`定义是相当清晰和高效的, 所以无需过度封装, 切勿走上手拼url和param的道路
- 不要相信后端, 定义实体类时任何字段都应该是可空类型
- 对于多数据源的项目,我建议定义`HttpCall2`, `HttpCall3`方法, 愚蠢但够清晰
- 不要写回调或者DSL，协程的挂起恢复机制可以替代它，否则又走上了老路


## 1. 封装方法
```kotlin
// 就一个`HttpCall.kt`文件, 复制到自己项目中即可
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
```
## 2. 源代码

> [Github](https://github.com/Joehaivo/HttpCall)
