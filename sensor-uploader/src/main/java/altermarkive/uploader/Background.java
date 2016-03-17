/*
This code is free  software: you can redistribute it and/or  modify it under the
terms of the GNU Lesser General Public License as published by the Free Software
Foundation,  either version  3 of  the License,  or (at  your option)  any later
version.

This code  is distributed in the  hope that it  will be useful, but  WITHOUT ANY
WARRANTY; without even the implied warranty  of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Lesser  General Public License for more details.

You should have  received a copy of the GNU  Lesser General Public License along
with code. If not, see http://www.gnu.org/licenses/.
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
