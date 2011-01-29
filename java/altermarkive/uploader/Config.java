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

import android.content.res.Resources;
import android.util.SparseArray;
import android.util.SparseIntArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

public class Config {
    private final static String TAG = Config.class.getName();

    private final static int MIN_TYPE = 1;
    private final static int MAX_TYPE = 20;
    private final Sampler sampler;
    private SparseArray<Integer[]> sampling;
    private int storing;
    private String url;

    public Config(Sampler sampler) {
        this.sampler = sampler;
        reset();
        String preferences = Storage.readText("preferences.json");
        if (preferences == null) {
            preferences = defaults(sampler);
            if (preferences == null) {
                return;
            }
        }
        JSONObject json;
        try {
            json = new JSONObject(preferences);
            JSONObject sensors = json.getJSONObject("sensors");
            Iterator<String> keys = sensors.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = sensors.getJSONArray(key);
                Integer[] intervals = new Integer[array.length()];
                for (int i = 0; i < intervals.length; i++) {
                    JSONObject item = array.getJSONObject(i);
                    intervals[i] = item.getInt("interval");
                }
                sampling.put(Integer.valueOf(key), intervals);
            }
            JSONObject storage = json.getJSONObject("storage");
            if (storage != null) {
                storing = storage.getInt("interval");
                storing = storing < 60000 ? 60000 : storing;
                storage.put("interval", storing);
            }
            JSONObject upload = json.getJSONObject("upload");
            if (storage != null) {
                url = upload.getString("url");
            }
            Storage.writeText("preferences.json", json.toString());
        } catch (JSONException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to read preferences:\n%s", trace);
            Log.e(TAG, message);
            reset();
        }
    }

    private String defaults(Sampler sampler) {
        Resources resource = sampler.context().getResources();
        InputStream input = resource.openRawResource(R.raw.preferences);
        try {
            int size = input.available();
            byte[] bytes = new byte[size];
            if (input.read(bytes) != size) {
                Log.e(TAG, "Size mismatch while reading default preferences");
                return null;
            }
            input.close();
            return new String(bytes);
        } catch (IOException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to read resources:\n%s", trace);
            Log.e(TAG, message);
            return null;
        }
    }

    private void reset() {
        sampling = new SparseArray<Integer[]>();
        storing = 0;
        url = "";
    }

    private int sampling(int type, int index) {
        Integer[] intervals = sampling.get(type);
        if (intervals == null || intervals.length <= index) {
            return 0;
        }
        return intervals[index];
    }

    public String url() {
        return url;
    }

    public int[] initiate(int[] types, String[] vendors, String[] names, int[] delays, float[] resolutions, int[] sizes) {
        SparseIntArray indices = new SparseIntArray();
        for (int i = MIN_TYPE; i <= MAX_TYPE; i++) {
            indices.put(i, 0);
        }
        int[] intervals = new int[types.length];
        Arrays.fill(intervals, 0);
        for (int i = 0; i < types.length; i++) {
            String identifier = String.format("%d, %s, %s", types[i], vendors[i], names[i]);
            if (types[i] < MIN_TYPE || MAX_TYPE < types[i] || sizes[i] == 0) {
                String message = String.format("Ignored sensor: %s", identifier);
                Log.i(TAG, message);
                continue;
            }
            // Find the interval and update sensor index
            int index = indices.get(types[i]);
            intervals[i] = sampling(types[i], index);
            indices.put(types[i], index + 1);
            // Clip interval based on minimum delay reported
            if (0 < intervals[i]) {
                intervals[i] = Math.max(intervals[i], delays[i] / 1000);
                String message = String.format("The interval for sensor #%d (%s) is %d", i, identifier, intervals[i]);
                Log.i(TAG, message);
            }
        }
        sampler.data().initiate(sizes, intervals, storing);
        try {
            String report = Report.report(sampler.context(), types, vendors, names, resolutions, delays, null, null);
            Storage.writeText("device.json", report);
        } catch (JSONException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to report on device specification:\n%s", trace);
            Log.e(TAG, message);
        }
        return intervals;
    }
}
