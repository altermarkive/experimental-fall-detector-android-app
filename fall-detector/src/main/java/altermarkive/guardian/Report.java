package altermarkive.uploader;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.provider.Settings.Secure;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Report {
    private final static String TAG = Report.class.getName();

    @SuppressWarnings("unused")
    public static String probe(Context context) throws JSONException {
        SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> list = manager.getSensorList(Sensor.TYPE_ALL);
        int[] types = new int[list.size()];
        String[] vendors = new String[list.size()];
        String[] names = new String[list.size()];
        float[] resolutions = new float[list.size()];
        int[] delays = new int[list.size()];
        float[] ranges = new float[list.size()];
        float[] powers = new float[list.size()];
        int i = 0;
        for (Sensor sensor : list) {
            types[i] = sensor.getType();
            vendors[i] = sensor.getVendor();
            names[i] = sensor.getName();
            resolutions[i] = sensor.getResolution();
            delays[i] = sensor.getMinDelay();
            ranges[i] = sensor.getMaximumRange();
            powers[i] = sensor.getPower();
            i++;
        }
        return report(context, types, vendors, names, resolutions, delays, ranges, powers);
    }

    public static String report(Context context, int[] types, String[] vendors, String[] names, float[] resolutions, int[] delays, float[] ranges, float[] powers) throws JSONException {
        JSONObject report = new JSONObject();
        JSONObject device = reportDevice(context);
        report.put("device", device);
        JSONArray sensors = new JSONArray();
        device.put("sensors", sensors);
        int i = 0;
        do {
            JSONObject sensor = new JSONObject();
            putInt(types, i, sensor, "type");
            putString(vendors, i, sensor, "vendor");
            putString(names, i, sensor, "name");
            putFloat(resolutions, i, sensor, "resolution");
            putInt(delays, i, sensor, "delay");
            putFloat(ranges, i, sensor, "range");
            putFloat(powers, i, sensor, "power");
            if (0 < sensor.length()) {
                sensors.put(sensor);
            }
            i++;
        } while (i == sensors.length());
        return report.toString();
    }

    private static void putInt(int[] array, int index, JSONObject values, String name) throws JSONException {
        if (array != null && index < array.length) {
            values.put(name, array[index]);
        }
    }

    private static void putFloat(float[] array, int index, JSONObject values, String name) throws JSONException {
        if (array != null && index < array.length) {
            values.put(name, reportFloat(array[index]));
        }
    }

    private static void putString(String[] array, int index, JSONObject values, String name) throws JSONException {
        if (array != null && index < array.length) {
            values.put(name, array[index]);
        }
    }

    private static JSONObject reportFloat(float value) throws JSONException {
        JSONObject hifi = new JSONObject();
        long bits = Float.floatToRawIntBits(value);
        hifi.put("sign", bits >> 31);
        bits &= 0x7FFFFFFF;
        hifi.put("exponent", bits >> 23);
        bits &= 0x007FFFFF;
        hifi.put("mantissa", bits);
        return hifi;
    }

    private static JSONObject reportDevice(Context context) throws JSONException {
        JSONObject device = new JSONObject();
        device.put("id", hash(id(context)));
        device.put("manufacturer", Build.MANUFACTURER);
        device.put("brand", Build.BRAND);
        device.put("product", Build.PRODUCT);
        device.put("model", Build.MODEL);
        device.put("design", Build.DEVICE);
        device.put("board", Build.BOARD);
        device.put("hardware", Build.HARDWARE);
        device.put("build", Build.FINGERPRINT);
        return device;
    }

    public static String id(Context context) {
        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    private static String hash(String text) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes());
            BigInteger integer = new BigInteger(1, md.digest());
            hash = String.format("%1$032X", integer);
        } catch (NoSuchAlgorithmException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("MD5 is not available:\n%s", trace);
            Log.e(TAG, message);
        }
        return hash;
    }
}
