package com.github.joehaivo.httpcall

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintLayout
import com.blankj.utilcode.util.ColorUtils
import com.github.joehaivo.httpcall.databinding.ProgressWaveViewBinding
import kotlin.math.sin

/**
 * 一个带有水波纹的进度条 类似竖向的Progressbar
 */
class ProgressWaveView : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        private val TAG = ProgressWaveView::class.java.simpleName
    }

    val binding = ProgressWaveViewBinding.inflate(LayoutInflater.from(context), this, true)

    @IntRange(from = 0, to = 100)
    var progress: Int = 0
        set(value) {
            field = value
            postInvalidate()
        }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            gravityX = event?.values?.getOrNull(0) ?: 0f
            postInvalidate()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }
    private val wavePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = ColorUtils.getColor(R.color.blue_200)
        style = Paint.Style.FILL
    }

    private val wavePath1 = Path()
    private val wavePath2 = Path()
    private val drawFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var mOffset1 = 0.0f
    private var mOffset2 = 0.0f
    private val mSpeed1 = 0.01f
    private val mSpeed2 = 0.02f
    private var gravityX = 0f
    private val matrix = Matrix()

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sensorManager?.registerListener(sensorEventListener, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawFilter = drawFilter
        wavePath1.reset()
        wavePath2.reset()
        wavePath1.moveTo(-width * 3.toFloat(), height * 3.toFloat())
        wavePath2.moveTo(-width * 3.toFloat(), height * 3.toFloat())
        for (x in 0..width step 2) {
            if (!isAttachedToWindow) break
            // y = A * sin( wx + b) + h ; A： 浪高； w：周期；b：初相；h: progress
            val y = 20 * sin(2 * Math.PI / width * x + mOffset1) + (100 - progress) / 100f * height
            wavePath1.lineTo(x.toFloat(), y.toFloat())
            val y2 = 25 * sin(2 * Math.PI / width * x + mOffset2) + (100 - progress) / 100f * height
            wavePath2.lineTo(x.toFloat(), y2.toFloat())
        }
        wavePath1.lineTo(width * 3.toFloat(), height * 3.toFloat())
        wavePath2.lineTo(width * 3.toFloat(), height * 3.toFloat())
        // 跟随重力方向旋转倾斜
        matrix.setRotate(gravityX * 2.5f, width / 2f, height / 2f)
        wavePath1.transform(matrix)
        wavePath2.transform(matrix)
        canvas?.drawPath(wavePath1, wavePaint)
        canvas?.drawPath(wavePath2, wavePaint)

        if (mOffset1 > Float.MAX_VALUE - 1) { //防止数值超过浮点型的最大值
            mOffset1 = 0F
        }
        mOffset1 += mSpeed1

        if (mOffset2 > Float.MAX_VALUE - 1) { //防止数值超过浮点型的最大值
            mOffset2 = 0F
        }
        mOffset2 += mSpeed2
        if (isAttachedToWindow) {
            postInvalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager?.unregisterListener(sensorEventListener, gravitySensor)
    }
}