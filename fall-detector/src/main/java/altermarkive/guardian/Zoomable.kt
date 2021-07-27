package altermarkive.guardian

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

abstract class Zoomable(context: Context?, attributes: AttributeSet?) :
    SurfaceView(context, attributes), SurfaceHolder.Callback, Runnable, OnTouchListener {
    companion object {
        private var reference: Matrix? = null
        private var operation: Matrix? = null
        private var deltaX = 0f
        private var deltaY = 0f
        private var centerX = 1f
        private var centerY = 1f
        private var sumX = 0f
        private var sumY = 0f
        private var minX = 0f
        private var minY = 0f
        private var maxX = 0f
        private var maxY = 0f
        private var deltaXNow = 0f
        private var deltaYNow = 0f
        private var centerXNow = 0f
        private var centerYNow = 0f
        private var scaleX = 0f
        private var scaleY = 0f
        private val rawOnTouch = FloatArray(9)
        private val rawOnPaint = FloatArray(9)
        private var limitX = 0f
        private var limitY = 0f
        private var gesture = false
    }

    private val surfaceHolder = holder
    private var executor: ScheduledExecutorService? = null

    private fun surfaceDrawInternal(gfx: Canvas) {
        if (null == operation || null == reference) {
            val matrix = matrix
            matrix.getValues(rawOnTouch)
            limitX = rawOnTouch[Matrix.MTRANS_X]
            limitY = rawOnTouch[Matrix.MTRANS_Y]
            operation = Matrix(matrix)
            reference = Matrix(matrix)
        }
        val operation = operation ?: return
        synchronized(this) {
            operation.getValues(rawOnPaint)
            gfx.translate(
                rawOnPaint[Matrix.MTRANS_X],
                rawOnPaint[Matrix.MTRANS_Y]
            )
            gfx.scale(
                rawOnPaint[Matrix.MSCALE_X],
                rawOnPaint[Matrix.MSCALE_Y]
            )
        }
        surfaceDraw(gfx)
    }

    abstract fun surfaceDraw(gfx: Canvas)

    override fun onTouch(content: View, event: MotionEvent): Boolean {
        invalidate()
        if (null == operation || null == reference) {
            return false
        }
        val width = width
        val height = height
        synchronized(this) {
            if (event.pointerCount < 2) {
                return true
            }
            pinchSpan(event)
            pinchPrepare(event, width, height)
            return pinchProcess(event, width, height)
        }
    }

    override fun run() {
        val gfx = surfaceHolder.lockCanvas()
        if (gfx != null) {
            try {
                synchronized(surfaceHolder) { surfaceDrawInternal(gfx) }
            } finally {
                surfaceHolder.unlockCanvasAndPost(gfx)
            }
        }
    }

    private fun pinchSpan(event: MotionEvent) {
        val count = event.pointerCount
        sumX = 0f
        sumY = 0f
        minX = Float.MAX_VALUE
        minY = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        maxY = Float.MIN_VALUE
        for (i in 0 until count) {
            val x = event.getX(i)
            val y = event.getY(i)
            sumX += x
            sumY += y
            if (x < minX) {
                minX = x
            }
            if (y < minY) {
                minY = y
            }
            if (maxX < x) {
                maxX = x
            }
            if (maxY < y) {
                maxY = y
            }
        }
    }

    private fun pinchPrepare(event: MotionEvent, width: Int, height: Int) {
        val count = event.pointerCount
        deltaXNow = deltaX
        deltaYNow = deltaY
        centerXNow = centerX
        centerYNow = centerY
        if (0 < count) {
            deltaXNow = maxX - minX
            deltaYNow = maxY - minY
            centerXNow = sumX / count.toFloat()
            centerYNow = sumY / count.toFloat()
        }
        Companion.scaleX = 1f
        Companion.scaleY = 1f
        if (deltaX != deltaXNow && width * 0.15 < deltaX) {
            Companion.scaleX = deltaXNow / deltaX
            Companion.scaleX = if (0f == Companion.scaleX) 1f else Companion.scaleX
        }
        if (deltaY != deltaYNow && height * 0.15 < deltaY) {
            Companion.scaleY = deltaYNow / deltaY
            Companion.scaleY = if (0f == Companion.scaleY) 1f else Companion.scaleY
        }
    }

    private fun pinchProcess(event: MotionEvent, width: Int, height: Int): Boolean {
        var result = true
        var down = false
        val operation = operation ?: return false
        val reference = reference ?: return false
        when (val action = event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                // Update the matrix
                operation.set(reference)
                operation.postTranslate(
                    centerXNow - centerX, centerYNow
                            - centerY
                )
                operation.postScale(Companion.scaleX, Companion.scaleY, centerXNow, centerYNow)
                // Validate to keep within viewport
                operation.getValues(rawOnTouch)
                if (rawOnTouch[Matrix.MSCALE_X] < 1f) {
                    rawOnTouch[Matrix.MSCALE_X] = 1f
                }
                if (rawOnTouch[Matrix.MSCALE_Y] < 1f) {
                    rawOnTouch[Matrix.MSCALE_Y] = 1f
                }
                if (rawOnTouch[Matrix.MTRANS_X] > limitX) {
                    rawOnTouch[Matrix.MTRANS_X] = limitX
                }
                if (rawOnTouch[Matrix.MTRANS_Y] > limitY) {
                    rawOnTouch[Matrix.MTRANS_Y] = limitY
                }
                val thresholdX = limitX - (rawOnTouch[Matrix.MSCALE_X] - 1) * width
                val thresholdY = limitY - (rawOnTouch[Matrix.MSCALE_Y] - 1) * height
                if (rawOnTouch[Matrix.MTRANS_X] < thresholdX) {
                    rawOnTouch[Matrix.MTRANS_X] = thresholdX
                }
                if (rawOnTouch[Matrix.MTRANS_Y] < thresholdY) {
                    rawOnTouch[Matrix.MTRANS_Y] = thresholdY
                }
                operation.setValues(rawOnTouch)
                if (0f != centerXNow - centerX || 0f != centerYNow - centerY) {
                    gesture = true
                }
                result = true
            }
            MotionEvent.ACTION_DOWN -> {
                gesture = false
                down = true
                // Replace the matrix
                reference.set(operation)
                // Reinitialize touch controls
                deltaX = deltaXNow
                deltaY = deltaYNow
                centerX = centerXNow
                centerY = centerYNow
                // Check if there was any pinch or drag
                result = !(MotionEvent.ACTION_UP == action && !gesture || down)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                down = true
                reference.set(operation)
                deltaX = deltaXNow
                deltaY = deltaYNow
                centerX = centerXNow
                centerY = centerYNow
                result = !(MotionEvent.ACTION_UP == action && !gesture || down)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                reference.set(operation)
                deltaX = deltaXNow
                deltaY = deltaYNow
                centerX = centerXNow
                centerY = centerYNow
                result = !(MotionEvent.ACTION_UP == action && !gesture || down)
            }
        }
        return result
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.scheduleAtFixedRate(this, 0, 40, TimeUnit.MILLISECONDS)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        executor?.shutdown()
    }
}