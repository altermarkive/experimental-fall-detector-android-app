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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class Background extends Service {
    public static void initiate(Context context) {
        Intent intent = new Intent(context, Background.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        String app = getResources().getString(R.string.app);
        String info = app + " is active.";
        long now = System.currentTimeMillis();
        //noinspection deprecation
        Notification notification = new Notification(
                android.R.drawable.stat_sys_warning, info, now);
        Intent about = new Intent(this, Main.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, about, 0);
        //noinspection deprecation
        notification.setLatestEventInfo(this, app, info, pending);
        startForeground(1, notification);
        return (START_STICKY);
    }

    @Override
    public void onCreate() {
        Context context = getApplicationContext();
        Sampler.instance(this).initiate(context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (null);
    }
}
