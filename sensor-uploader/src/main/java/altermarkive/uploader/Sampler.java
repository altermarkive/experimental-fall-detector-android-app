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
package altermarkive.uploader;

import android.content.Context;
import android.os.PowerManager;

public class Sampler {
    private final static String TAG = Sampler.class.getName();

    private static volatile Sampler instance = null;

    public static synchronized Sampler instance(Context context) {
        if (instance == null) {
            instance = new Sampler(context);
        }
        return instance;
    }

    private Context context;
    private Config config;
    private Data data;

    private Sampler(Context context) {
        this.context = context;
        config = new Config(this);
        data = new Data();
    }

    public Context context() {
        return context;
    }

    @SuppressWarnings("unused")
    public Config config() {
        return config;
    }

    public Data data() {
        return data;
    }

    public void initiate(Context context) {
        if (Wear.detected()) {
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            PowerManager.WakeLock lock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            if (!lock.isHeld()) {
                lock.acquire();
            }
        }
        initiate();
    }

    private native void initiate();

    static {
        System.loadLibrary("sampler");
    }
}
