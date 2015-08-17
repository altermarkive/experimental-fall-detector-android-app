/*
The MIT License (MIT)

Copyright (c) 2016

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package android.code.tabs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

@SuppressWarnings("SynchronizeOnNonFinalField")
@SuppressLint("ClickableViewAccessibility")
public class Content extends View implements OnTouchListener {
    private static final PaintFlagsDrawFilter ANTIALIAS_FILTER = new PaintFlagsDrawFilter(
            1, Paint.ANTI_ALIAS_FLAG);
    private Paint foreground;
    private Paint background;
    private String title = "";
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
    }

    public Content(Context context, String title) {
        this(context);
        this.title = title;
    }

    public void onDraw(Canvas gfx) {
        int width = getWidth();
        int height = getHeight();
        gfx.setDrawFilter(ANTIALIAS_FILTER);
        if (null == operation || null == reference) {
            @SuppressWarnings("deprecation")
            Matrix matrix = gfx.getMatrix();
            matrix.getValues(rawOnTouch);
            limitX = rawOnTouch[Matrix.MTRANS_X];
            limitY = rawOnTouch[Matrix.MTRANS_Y];
            operation = new Matrix(matrix);
            reference = new Matrix(matrix);
        }
        synchronized (operation) {
            operation.getValues(rawOnPaint);
            if (Build.VERSION.SDK_INT <= 10) {
                gfx.setMatrix(operation);
            } else {
                gfx.translate(rawOnPaint[Matrix.MTRANS_X],
                        rawOnPaint[Matrix.MTRANS_Y]);
                gfx.scale(rawOnPaint[Matrix.MSCALE_X],
                        rawOnPaint[Matrix.MSCALE_Y]);
            }
        }
        gfx.drawRect(0, 0, width, height, background);
        gfx.drawText(title, width / 2, height / 2, foreground);
    }

    public boolean onTouch(View content, MotionEvent event) {
        invalidate();
        if (null == operation || null == reference) {
            return (false);
        }
        int width = getWidth();
        int height = getHeight();
        synchronized (operation) {
            pinchSpan(event);
            pinchPrepare(event, width, height);
            return (pinchProcess(event, width, height));
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
}
