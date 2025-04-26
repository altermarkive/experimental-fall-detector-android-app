package altermarkive.guardian

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import android.util.AttributeSet
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.View
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class Surface(context: Context?, attributes: AttributeSet?) : Zoomable(context, attributes),
    SurfaceHolder.Callback, Runnable, TabLayout.OnTabSelectedListener {
    class Signal(val label: String, val color: Int, val buffer: Int)

    class Threshold(val label: String, val color: Int, val value: Double)

    class Chart(
        val label: String, val min: Float, val max: Float, val signals: Array<Signal>,
        val thresholds: Array<Threshold>
    )

    private val surfaceHolder = holder
    private var selected = 0
    private val bounds = Rect()
    private val buffers = Buffers(Detector.BUFFER_COUNT, Detector.N, 0, Double.NaN)

    override fun onTabSelected(tab: TabLayout.Tab) {
        val parent = tab.parent ?: return
        selected = parent.selectedTabPosition
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {
        val parent = tab.parent ?: return
        selected = parent.selectedTabPosition
    }

    override fun surfaceDraw(gfx: Canvas) {
        val parent: View = parent as View
        val live: SwitchMaterial = parent.findViewById(R.id.live)
        if (live.isChecked) {
            synchronized(Detector.singleton.buffers) {
                Detector.singleton.buffers.copyInto(buffers)
            }
        }
        val position = buffers.position
        chart(gfx, position)
    }

    private fun chart(gfx: Canvas, position: Int) {
        val width = width
        val height = height
        gfx.drawColor(-0x1)
        gfx.drawFilter = ANTIALIASING
        val chart = CHARTS[selected]
        grid(gfx, chart, width, height)
        legend(gfx, chart)
        thresholds(gfx, chart, width, height)
        signals(gfx, chart, position, width, height)
    }

    private fun grid(gfx: Canvas, chart: Chart, width: Int, height: Int) {
        val paint = paint(-0x222223)
        // Vertical
        var i = 0
        while (i <= Detector.N) {
            val x = x((Detector.N - 1 - i).toDouble(), width.toDouble())
            gfx.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
            i += 1000 / Detector.INTERVAL_MS
        }
        // Horizontal
        val delta = (chart.max - chart.min).toDouble()
        var step = 10.0.pow(floor(log10(delta)))
        while (delta / step < 2) {
            step /= 10.0
        }
        val lower = chart.min - chart.min % step
        val higher = chart.max - chart.max % step
        var value = lower
        while (value <= higher) {
            val y = y(value, height.toDouble(), chart.min.toDouble(), chart.max.toDouble())
            gfx.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
            value += step
        }
    }

    private fun legend(gfx: Canvas, chart: Chart) {
        var paint = paint(-0x1000000)
        paint.textSize = height / 40f
        paint.getTextBounds("|", 0, 1, bounds)
        val delta = bounds.height() * 1.4
        var y = delta.toFloat()
        gfx.drawText(chart.label, 10f, y, paint)
        y += delta.toFloat()
        for (signal in chart.signals) {
            paint = paint(signal.color)
            paint.textSize = height / 40f
            gfx.drawText(signal.label, 10f, y, paint)
            y += delta.toFloat()
        }
    }

    private fun thresholds(gfx: Canvas, chart: Chart, width: Int, height: Int) {
        for (threshold in chart.thresholds) {
            val paint = paint(threshold.color)
            paint.textSize = height / 40f
            val y =
                y(threshold.value, height.toDouble(), chart.min.toDouble(), chart.max.toDouble())
            gfx.drawText(threshold.label, 10f, y.toFloat() - 10, paint)
            gfx.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }
    }

    private fun signals(
        gfx: Canvas,
        chart: Chart,
        position: Int,
        width: Int,
        height: Int
    ) {
        for (signal in chart.signals) {
            val paint = paint(signal.color)
            val buffer = buffers.buffers[signal.buffer]
            var lastX = Double.NaN
            var lastY = Double.NaN
            for (sample in 0 until Detector.N) {
                val x = x(sample.toDouble(), width.toDouble())
                val value = buffer[(position + 1 + sample) % Detector.N]
                lastY = if (valid(value)) {
                    val y = y(value, height.toDouble(), chart.min.toDouble(), chart.max.toDouble())
                    if (valid(lastY)) {
                        gfx.drawLine(
                            lastX.toFloat(),
                            lastY.toFloat(),
                            x.toFloat(),
                            y.toFloat(),
                            paint
                        )
                    }
                    y
                } else {
                    Double.NaN
                }
                lastX = x
            }
        }
    }

    companion object {
        val CHARTS = arrayOf(
            Chart(
                "Acc.", -2.0f, 2.0f, arrayOf(
                    Signal("X", -0x10000, Detector.BUFFER_X),
                    Signal("Y", -0xff0100, Detector.BUFFER_Y),
                    Signal("Z", -0xffff01, Detector.BUFFER_Z)
                ), arrayOf(
                    Threshold("+1G", -0x8100, +1.0),
                    Threshold("-1G", -0x8100, -1.0)
                )
            ),
            Chart(
                "Acc. X", -2.0f, 2.0f, arrayOf(
                    Signal("X", -0x10000, Detector.BUFFER_X),
                    Signal("X LPF", -0xff0100, Detector.BUFFER_X_LPF),
                    Signal("X HPF", -0xffff01, Detector.BUFFER_X_HPF)
                ), arrayOf(
                    Threshold("+1G", -0x8100, +1.0),
                    Threshold("-1G", -0x8100, -1.0)
                )
            ),
            Chart(
                "Acc. Y", -2.0f, 2.0f, arrayOf(
                    Signal("Y", -0x10000, Detector.BUFFER_Y),
                    Signal("Y LPF", -0xff0100, Detector.BUFFER_Y_LPF),
                    Signal("Y HPF", -0xffff01, Detector.BUFFER_Y_HPF)
                ), arrayOf(
                    Threshold("+1G", -0x8100, +1.0),
                    Threshold("-1G", -0x8100, -1.0)
                )
            ),
            Chart(
                "Acc. Z", -2.0f, 2.0f, arrayOf(
                    Signal("Z", -0x10000, Detector.BUFFER_Z),
                    Signal("Z LPF", -0xff0100, Detector.BUFFER_Z_LPF),
                    Signal("Z HPF", -0xffff01, Detector.BUFFER_Z_HPF)
                ), arrayOf(
                    Threshold("+1G", -0x8100, +1.0),
                    Threshold("-1G", -0x8100, -1.0)
                )
            ),
            Chart(
                "Δ", -2.0f, 2.0f, arrayOf(
                    Signal("X D", -0x10000, Detector.BUFFER_X_MAX_MIN),
                    Signal("Y D", -0xff0100, Detector.BUFFER_Y_MAX_MIN),
                    Signal("Z D", -0xffff01, Detector.BUFFER_Z_MAX_MIN)
                ), arrayOf(
                    Threshold("+1G", -0x8100, +1.0),
                    Threshold("-1G", -0x8100, -1.0)
                )
            ),
            Chart(
                "Thr.", -1.0f, 6.0f, arrayOf(
                    Signal("SV TOT", -0x10000, Detector.BUFFER_SV_TOT),
                    Signal("SV D", -0xff0100, Detector.BUFFER_SV_D),
                    Signal("SV MAX MIN", -0xffff01, Detector.BUFFER_SV_MAX_MIN),
                    Signal("Z 2", -0x1000000, Detector.BUFFER_Z_2)
                ), arrayOf(
                    Threshold("Fall: SV TOT", -0x10000, Detector.FALLING_WAIST_SV_TOT),
                    Threshold("Impact: SV TOT, SV MAX MIN", -0x10000, Detector.IMPACT_WAIST_SV_TOT),
                    Threshold("Impact: SV D", -0xff0100, Detector.IMPACT_WAIST_SV_D),
                    Threshold(
                        "Impact: SV TOT, SV MAX MIN",
                        -0xffff01,
                        Detector.IMPACT_WAIST_SV_MAX_MIN
                    ),
                    Threshold("Impact: Z 2", -0x1000000, Detector.IMPACT_WAIST_Z_2)
                )
            ),
            Chart(
                "Evt.", -0.5f, 1.5f, arrayOf(
                    Signal("Falling", -0xff0100, Detector.BUFFER_FALLING),
                    Signal("Impact", -0xffff01, Detector.BUFFER_IMPACT),
                    Signal("Lying", -0x10000, Detector.BUFFER_LYING)
                ), arrayOf()
            )
        )
        private val ANTIALIASING = PaintFlagsDrawFilter(1, Paint.ANTI_ALIAS_FLAG)

        private fun valid(value: Double): Boolean {
            return !(java.lang.Double.isInfinite(value) || java.lang.Double.isNaN(value))
        }

        private fun x(x: Double, width: Double): Double {
            return x * (width - 1) / (Detector.N - 1).toDouble()
        }

        private fun y(y: Double, height: Double, min: Double, max: Double): Double {
            return (height - 1) * (1 - (y - min) / (max - min))
        }

        private val paints = SparseArray<Paint>()

        private fun paint(color: Int): Paint {
            var paint = paints[color]
            if (null == paint) {
                paint = Paint()
                paint.color = color
                paint.strokeWidth = 3f
                paints.put(color, paint)
            }
            return paint
        }
    }

    init {
        surfaceHolder.addCallback(this)
        setOnTouchListener(this)
    }
}