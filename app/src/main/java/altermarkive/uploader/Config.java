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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
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

    public final static int MIN_TYPE = 1;
    public final static int MAX_TYPE = 21;
    private final Sampler sampler;

    public Config(Sampler sampler) {
        this.sampler = sampler;
    }

    private void naming(int type, int index, String name) {
        Context context = sampler.context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = String.format(context.getString(R.string.naming), type, index);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, name);
        editor.commit();
    }

    private int sampling(int type, int index) {
        Context context = sampler.context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = String.format(context.getString(R.string.sampling), type, index);
        int sampling = Integer.parseInt(preferences.getString(key, "0"));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, Integer.toString(sampling));
        editor.commit();
        return sampling;
    }

    private int storing() {
        Context context = sampler.context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.storing);
        int storing = Integer.parseInt(preferences.getString(key, "0"));
        return storing;
    }

    @SuppressWarnings("unused")
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
            // Update the name of the sensor
            naming(types[i], index, names[i]);
            // Clip interval based on minimum delay reported
            if (0 < intervals[i]) {
                intervals[i] = Math.max(intervals[i], delays[i] / 1000);
                String message = String.format("The interval for sensor #%d (%s) is %d", i, identifier, intervals[i]);
                Log.i(TAG, message);
            }
        }
        sampler.data().initiate(sizes, intervals, storing());
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
