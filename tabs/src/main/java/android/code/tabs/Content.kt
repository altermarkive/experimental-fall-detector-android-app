package android.code.tabs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Content extends SurfaceView implements Callback, Runnable, OnTouchListener {
    private static final PaintFlagsDrawFilter ANTIALIAS_FILTER = new PaintFlagsDrawFilter(
            1, Paint.ANTI_ALIAS_FLAG);
    private Paint foreground;
    private Paint background;
    protected String title = "";
    private static Matrix reference = null;
    private static Matrix operation = null;
    private static float deltaX = 0;
    private static float deltaY = 0;
    private static float centerX = 1;
    private static float centerY = 1;
    private static float sumX, sumY, minX, minY, maxX, maxY;
    private static float deltaXNow, deltaYNow, centerXNow, centerYNow, scaleX, scaleY;
    private final static float[] rawOnTouch = new float[9];
    private final static float[] rawOnPaint = new float[9];
    private static float limitX;
    private static float limitY;
    private static boolean gesture = false;
    private final SurfaceHolder holder = getHolder();
    private ScheduledExecutorService executor = null;

    private void init() {
        foreground = new Paint();
        foreground.setTextSize(56);
        foreground.setColor(0xFFFF0000);
        background = new Paint();
        background.setTextSize(56);
        background.setColor(0xFFFFFFFF);
        setOnTouchListener(this);
    }

    public Content(Context context) {
        super(context);
        init();
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        setOnTouchListener(this);
    }

    public Content(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        setOnTouchListener(this);
    }

    public void surfaceDraw(Canvas gfx) {
        int width = getWidth();
        int height = getHeight();
        gfx.setDrawFilter(ANTIALIAS_FILTER);
        if (null == operation || null == reference) {
            Matrix matrix = getMatrix();
            matrix.getValues(rawOnTouch);
            limitX = rawOnTouch[Matrix.MTRANS_X];
            limitY = rawOnTouch[Matrix.MTRANS_Y];
            operation = new Matrix(matrix);
            reference = new Matrix(matrix);
        }
        synchronized (this) {
            operation.getValues(rawOnPaint);
            gfx.translate(rawOnPaint[Matrix.MTRANS_X],
                    rawOnPaint[Matrix.MTRANS_Y]);
            gfx.scale(rawOnPaint[Matrix.MSCALE_X],
                    rawOnPaint[Matrix.MSCALE_Y]);
        }
        gfx.drawRect(0, 0, width, height, background);
        gfx.drawText(title, width / 2f, height / 2f, foreground);
    }

    public boolean onTouch(View content, MotionEvent event) {
        invalidate();
        if (null == operation || null == reference) {
            return (false);
        }
        int width = getWidth();
        int height = getHeight();
        synchronized (this) {
            if (event.getPointerCount() < 2) {
                return false;
            }
            pinchSpan(event);
            pinchPrepare(event, width, height);
            return (pinchProcess(event, width, height));
        }
    }

    @Override
    public void run() {
        Canvas gfx = holder.lockCanvas();
        if (gfx != null) {
            try {
                synchronized (holder) {
                    surfaceDraw(gfx);
                }
            } finally {
                holder.unlockCanvasAndPost(gfx);
            }
        }
    }

    private void pinchSpan(MotionEvent event) {
        int count = event.getPointerCount();
        sumX = 0;
        sumY = 0;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            sumX += x;
            sumY += y;
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (maxX < x) {
                maxX = x;
            }
            if (maxY < y) {
                maxY = y;
            }
        }
    }

    private void pinchPrepare(MotionEvent event, int width, int height) {
        int count = event.getPointerCount();
        deltaXNow = deltaX;
        deltaYNow = deltaY;
        centerXNow = centerX;
        centerYNow = centerY;
        if (0 < count) {
            deltaXNow = maxX - minX;
            deltaYNow = maxY - minY;
            centerXNow = sumX / (float) count;
            centerYNow = sumY / (float) count;
        }
        scaleX = 1;
        scaleY = 1;
        if (deltaX != deltaXNow && width * 0.15 < deltaX) {
            scaleX = deltaXNow / deltaX;
            scaleX = 0 == scaleX ? 1 : scaleX;
        }
        if (deltaY != deltaYNow && height * 0.15 < deltaY) {
            scaleY = deltaYNow / deltaY;
            scaleY = 0 == scaleY ? 1 : scaleY;
        }
    }

    private boolean pinchProcess(MotionEvent event, int width, int height) {
        boolean result = true;
        boolean down = false;
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                // Update the matrix
                operation.set(reference);
                operation.postTranslate(centerXNow - centerX, centerYNow
                        - centerY);
                operation.postScale(scaleX, scaleY, centerXNow, centerYNow);
                // Validate to keep within viewport
                operation.getValues(rawOnTouch);
                if (rawOnTouch[Matrix.MSCALE_X] < 1) {
                    rawOnTouch[Matrix.MSCALE_X] = 1;
                }
                if (rawOnTouch[Matrix.MSCALE_Y] < 1) {
                    rawOnTouch[Matrix.MSCALE_Y] = 1;
                }
                if (rawOnTouch[Matrix.MTRANS_X] > limitX) {
                    rawOnTouch[Matrix.MTRANS_X] = limitX;
                }
                if (rawOnTouch[Matrix.MTRANS_Y] > limitY) {
                    rawOnTouch[Matrix.MTRANS_Y] = limitY;
                }
                float thresholdX = limitX - (rawOnTouch[Matrix.MSCALE_X] - 1)
                        * width;
                float thresholdY = limitY - (rawOnTouch[Matrix.MSCALE_Y] - 1)
                        * height;
                if (rawOnTouch[Matrix.MTRANS_X] < thresholdX) {
                    rawOnTouch[Matrix.MTRANS_X] = thresholdX;
                }
                if (rawOnTouch[Matrix.MTRANS_Y] < thresholdY) {
                    rawOnTouch[Matrix.MTRANS_Y] = thresholdY;
                }
                operation.setValues(rawOnTouch);
                if (0 != (centerXNow - centerX) || 0 != (centerYNow - centerY)) {
                    gesture = true;
                }
                result = true;
                break;
            case MotionEvent.ACTION_DOWN:
                gesture = false;
            case MotionEvent.ACTION_POINTER_DOWN:
                down = true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                // Replace the matrix
                reference.set(operation);
                // Reinitialize touch controls
                deltaX = deltaXNow;
                deltaY = deltaYNow;
                centerX = centerXNow;
                centerY = centerYNow;
                // Check if there was any pinch or drag
                result = !((MotionEvent.ACTION_UP == action && !gesture) || down);
                break;
        }
        return result;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this, 0, 40, TimeUnit.MILLISECONDS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        executor.shutdown();
    }
}
