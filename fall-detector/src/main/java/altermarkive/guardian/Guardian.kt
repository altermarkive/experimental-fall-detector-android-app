package altermarkive.guardian;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class Guardian extends Service {
    @Override
    public void onCreate() {
        Context context = getApplicationContext();
        Positioning.initiate(context);
        Detector.initiate(context);
    }

    public static void initiate(Context context) {
        Intent intent = new Intent(context, Guardian.class);
        context.startService(intent);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        long now = System.currentTimeMillis();
        Notification notification = new Notification(
                android.R.drawable.stat_sys_warning, "Fall Detector is active.", now);
        Intent about = new Intent(this, About.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, about, 0);
        notification.setLatestEventInfo(this, "Fall Detector", "Fall Detector is active", pending);
        startForeground(1, notification);
        return (START_STICKY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (null);
    }
}
