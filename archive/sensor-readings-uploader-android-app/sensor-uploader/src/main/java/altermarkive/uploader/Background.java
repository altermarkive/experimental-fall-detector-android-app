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
