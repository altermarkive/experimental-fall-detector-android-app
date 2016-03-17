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
package altermarkive.guardian;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

public class Surface extends SurfaceView implements Callback, Runnable, OnTouchListener {
    private static class Signal {
        public final String label;
        public final int color;
        public final int offset;

        public Signal(String label, int color, int offset) {
            this.label = label;
            this.color = color;
            this.offset = offset;
        }
    }

    private static class Threshold {
        public final String label;
        public final int color;
        public final double value;

        public Threshold(String label, int color, double value) {
            this.label = label;
            this.color = color;
            this.value = value;
        }
    }

    private static class Chart {
        public final String label;
        public final float min;
        public final float max;
        public final Signal[] signals;
        public final Threshold[] thresholds;

        public Chart(String label, float min, float max, Signal[] signals,
                     Threshold[] thresholds) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.signals = signals;
            this.thresholds = thresholds;
        }
    }

    private final static Chart[] CHARTS = {
            new Chart("Accelerometer", -2.0F, 2.0F,
                    new Signal[]{
                            new Signal("X", 0xFFFF0000, Detector.OFFSET_X),
                            new Signal("Y", 0xFF00FF00, Detector.OFFSET_Y),
                            new Signal("Z", 0xFF0000FF, Detector.OFFSET_Z)},
                    new Threshold[]{
                            new Threshold("+1G", 0xFFFF7F00, +1),
                            new Threshold("-1G", 0xFFFF7F00, -1)}),
            new Chart("X Axis", -2.0F, 2.0F,
                    new Signal[]{
                            new Signal("X", 0xFFFF0000, Detector.OFFSET_X),
                            new Signal("X LPF", 0xFF00FF00, Detector.OFFSET_X_LPF),
                            new Signal("X HPF", 0xFF0000FF, Detector.OFFSET_X_HPF)},
                    new Threshold[]{
                            new Threshold("+1G", 0xFFFF7F00, +1),
                            new Threshold("-1G", 0xFFFF7F00, -1)}),
            new Chart("Y Axis", -2.0F, 2.0F,
                    new Signal[]{
                            new Signal("Y", 0xFFFF0000, Detector.OFFSET_Y),
                            new Signal("Y LPF", 0xFF00FF00, Detector.OFFSET_Y_LPF),
                            new Signal("Y HPF", 0xFF0000FF, Detector.OFFSET_Y_HPF)},
                    new Threshold[]{
                            new Threshold("+1G", 0xFFFF7F00, +1),
                            new Threshold("-1G", 0xFFFF7F00, -1)}),
            new Chart("Z Axis", -2.0F, 2.0F,
                    new Signal[]{
                            new Signal("Z", 0xFFFF0000, Detector.OFFSET_Z),
                            new Signal("Z LPF", 0xFF00FF00, Detector.OFFSET_Z_LPF),
                            new Signal("Z HPF", 0xFF0000FF, Detector.OFFSET_Z_HPF)},
                    new Threshold[]{
                            new Threshold("+1G", 0xFFFF7F00, +1),
                            new Threshold("-1G", 0xFFFF7F00, -1)}),
            new Chart("Deltas", -2.0F, 2.0F,
                    new Signal[]{
                            new Signal("X D", 0xFFFF0000, Detector.OFFSET_X_D),
                            new Signal("Y D", 0xFF00FF00, Detector.OFFSET_Y_D),
                            new Signal("Z D", 0xFF0000FF, Detector.OFFSET_Z_D)},
                    new Threshold[]{
                            new Threshold("+1G", 0xFFFF7F00, +1),
                            new Threshold("-1G", 0xFFFF7F00, -1)}),
            new Chart("Thresholds", -1.0F, 6.0F,
                    new Signal[]{
                            new Signal("SV TOT", 0xFFFF0000, Detector.OFFSET_SV_TOT),
                            new Signal("SV D", 0xFF00FF00, Detector.OFFSET_SV_D),
                            new Signal("SV MAXMIN", 0xFF0000FF, Detector.OFFSET_SV_MAXMIN),
                            new Signal("Z 2", 0xFF000000, Detector.OFFSET_Z_2)},
                    new Threshold[]{
                            new Threshold("Fall: SV TOT", 0xFFFF0000, Detector.FALLING_WAIST_SV_TOT),
                            new Threshold("Impact: SV TOT, SV MAXMIN", 0xFFFF0000, Detector.IMPACT_WAIST_SV_TOT),
                            new Threshold("Impact: SV D", 0xFF00FF00, Detector.IMPACT_WAIST_SV_D),
                            new Threshold("Impact: SV TOT, SV MAXMIN", 0xFF0000FF, Detector.IMPACT_WAIST_SV_MAXMIN),
                            new Threshold("Impact: Z 2", 0xFF000000, Detector.IMPACT_WAIST_Z_2)}),
            new Chart("Events", -0.5F, 1.5F,
                    new Signal[]{
                            new Signal("Falling", 0xFF00FF00, Detector.OFFSET_FALLING),
                            new Signal("Impact", 0xFF0000FF, Detector.OFFSET_IMPACT),
                            new Signal("Lying", 0xFFFF0000, Detector.OFFSET_LYING)},
                    new Threshold[]{})
    };

    private final static PaintFlagsDrawFilter ANTIALIASING = new PaintFlagsDrawFilter(1, Paint.ANTI_ALIAS_FLAG);
    private final SurfaceHolder holder = getHolder();
    private boolean running = false;
    private Thread thread = null;
    private int selected = 0;
    private Rect bounds = new Rect();

    public Surface(Context context, AttributeSet attributes) {
        super(context, attributes);
        holder.addCallback(this);
        setOnTouchListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != thread) {
            running = false;
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int replacing = selected + CHARTS.length;
            if (event.getX() < getWidth() / 2) {
                replacing--;
            } else {
                replacing++;
            }
            selected = replacing % CHARTS.length;
        }
        return (true);
    }

    @Override
    public void run() {
        while (running) {
            Detector.acquire();
            double[] buffer = Detector.buffer();
            if (buffer != null) {
                Canvas gfx = holder.lockCanvas();
                if (gfx != null) {
                    int position = Detector.position();
                    try {
                        synchronized (holder) {
                            chart(gfx, buffer, position);
                        }
                    } finally {
                        holder.unlockCanvasAndPost(gfx);
                    }
                }
            }
            Detector.release();
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void chart(Canvas gfx, double[] block, int position) {
        int width = getWidth();
        int height = getHeight();
        gfx.drawColor(0xFFFFFFFF);
        gfx.setDrawFilter(ANTIALIASING);
        Chart chart = CHARTS[selected];
        grid(gfx, chart, width, height);
        legend(gfx, chart);
        thresholds(gfx, chart, width, height);
        signals(gfx, chart, block, position, width, height);
    }

    private void grid(Canvas gfx, Chart chart, int width, int height) {
        Paint paint = paint(0xFFDDDDDD);
        // Vertical
        for (int i = 0; i <= Detector.N; i += 1000 / Detector.INTERVAL_MS) {
            double x = x(Detector.N - 1 - i, width);
            gfx.drawLine((float) x, 0, (float) x, (float) height, paint);
        }
        // Horizontal
        double delta = chart.max - chart.min;
        double step = Math.pow(10, Math.floor(Math.log10(delta)));
        while (delta / step < 2) {
            step /= 10;
        }
        double infimum = chart.min - chart.min % step;
        double supremum = chart.max - chart.max % step;
        for (double value = infimum; value <= supremum; value += step) {
            double y = y(value, height, chart.min, chart.max);
            gfx.drawLine(0, (float) y, width, (float) y, paint);
        }
    }

    private void legend(Canvas gfx, Chart chart) {
        Paint paint = paint(0xFF000000);
        paint.getTextBounds("|", 0, 1, bounds);
        int delta = bounds.height();
        float y = delta;
        gfx.drawText(chart.label, 10, y, paint);
        y += delta;
        for (Signal signal : chart.signals) {
            paint = paint(signal.color);
            gfx.drawText(signal.label, 10, y, paint);
            y += delta;
        }
    }

    private void thresholds(Canvas gfx, Chart chart, int width, int height) {
        for (Threshold threshold : chart.thresholds) {
            Paint paint = paint(threshold.color);
            double y = y(threshold.value, height, chart.min, chart.max);
            gfx.drawText(threshold.label, 10, (float) y - 10, paint);
            gfx.drawLine(0, (float) y, width, (float) y, paint);
        }
    }

    private void signals(Canvas gfx, Chart chart, double[] block, int position, int width, int height) {
        for (Signal signal : chart.signals) {
            Paint paint = paint(signal.color);
            int offset = signal.offset;
            double lastX = Double.NaN;
            double lastY = Double.NaN;
            for (int sample = 0; sample < Detector.N; sample++) {
                double x = x(sample, width);
                double value = block[offset + ((position + 1 + sample) % Detector.N)];
                if (valid(value)) {
                    double y = y(value, height, chart.min, chart.max);
                    if (valid(lastY)) {
                        gfx.drawLine((float) lastX, (float) lastY, (float) x, (float) y, paint);
                    }
                    lastY = y;
                } else {
                    lastY = Double.NaN;
                }
                lastX = x;
            }
        }
    }

    private static boolean valid(double value) {
        return (!(Double.isInfinite(value) || Double.isNaN(value)));
    }

    private static double x(double x, double width) {
        return (x * (width - 1) / (double) (Detector.N - 1));
    }

    private static double y(double y, double height, double min, double max) {
        return ((height - 1) * (1 - (y - min) / (max - min)));
    }

    private static SparseArray<Paint> paints = new SparseArray<Paint>();

    private static Paint paint(int color) {
        Paint paint = paints.get(color);
        if (null == paint) {
            paint = new Paint();
            paint.setColor(color);
            paints.put(color, paint);
        }
        return (paint);
    }
}
