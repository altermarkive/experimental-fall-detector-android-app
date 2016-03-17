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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings.Secure;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Positioning implements LocationListener {
    private static Positioning singleton = null;
    private Object lock = new Object();
    private final Context context;
    private Location gps;
    private Location network;
    private long once = 0;
    private boolean replied = true;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public static void trigger() {
        if (null != singleton) {
            singleton.run();
        }
    }

    private Positioning(Context context) {
        this.context = context;
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        reset();
    }

    public static void initiate(Context context) {
        if (null == singleton) {
            singleton = new Positioning(context);
        }
    }

    private void run() {
        enforce(context);
        synchronized (lock) {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            once = System.currentTimeMillis();
            replied = false;
        }
    }

    private void reset() {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        manager.removeUpdates(this);
        int meters10 = 10;
        int minutes10 = 10 * 60 * 1000;
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minutes10, meters10, this);
    }

    private static void enforce(Context context) {
        enforceWiFi(context);
        enforceGPS(context);
    }

    private static void enforceWiFi(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(true);
    }

    @SuppressWarnings("deprecation")
    private static void enforceGPS(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return;
        }
        boolean stealth = false;
        try {
            PackageManager packages = context.getPackageManager();
            PackageInfo info = packages.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
            if (info != null) {
                for (ActivityInfo receiver : info.receivers) {
                    if (receiver.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && receiver.exported) {
                        stealth = true;
                    }
                }
            }
        } catch (NameNotFoundException ignored) {
        }
        if (stealth) {
            String provider = Secure.getString(context.getContentResolver(), Secure.LOCATION_PROVIDERS_ALLOWED);
            if (!provider.contains("gps")) {
                Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private double accuracy(Location location) {
        if (null != location && location.hasAccuracy()) {
            return (location.getAccuracy());
        } else {
            return (Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        enforce(context);
        synchronized (lock) {
            if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                if (accuracy(location) < accuracy(gps)) {
                    gps = location;
                }
            }
            if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
                if (accuracy(location) < accuracy(network)) {
                    gps = location;
                }
            }
            long deadline = once + 120000;
            long now = System.currentTimeMillis();
            if (deadline <= now && !replied) {
                int battery = Battery.level(context);
                String message;
                if (Double.isInfinite(accuracy(gps)) && Double.isInfinite(accuracy(network))) {
                    message = "Battery: %d%%; Location unknown";
                    message = String.format(Locale.US, message, battery);
                } else {
                    if (accuracy(gps) < accuracy(network)) {
                        location = gps;
                    } else {
                        location = network;
                    }
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    int accuracy = (int) location.getAccuracy();
                    int altitude = (int) location.getAltitude();
                    int bearing = (int) location.getBearing();
                    int speed = (int) (location.getSpeed() * 60.0 * 60.0 / 1000.0);
                    String time = format.format(new Date(location.getTime()));
                    message = "Battery: %d%% Location: %s %.5f %.5f ~%dm ^%dm %ddeg %dkm/h http://maps.google.com/?q=%.5f,%.5f";
                    message = String.format(Locale.US, message, battery, time, lat, lon, accuracy, altitude, bearing, speed, lat, lon);
                }
                Messenger.sms(Contact.get(context), message);
                reset();
                replied = true;
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        enforce(context);
    }

    @Override
    public void onProviderEnabled(String provider) {
        enforce(context);
    }

    @Override
    public void onProviderDisabled(String provider) {
        enforce(context);
    }
}
