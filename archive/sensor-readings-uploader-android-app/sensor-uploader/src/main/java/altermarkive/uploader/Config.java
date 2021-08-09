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
        Context context = sampler.context();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String url = preferences.getString(context.getString(R.string.uploading), null);
        Upload.instance().initiate(url);
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
        int[] periods = new int[types.length];
        Arrays.fill(periods, 0);
        for (int i = 0; i < types.length; i++) {
            String identifier = String.format("%d, %s, %s", types[i], vendors[i], names[i]);
            if (types[i] < MIN_TYPE || MAX_TYPE < types[i] || sizes[i] == 0) {
                String message = String.format("Ignored sensor: %s", identifier);
                Log.i(TAG, message);
                continue;
            }
            // Find the period and update sensor index
            int index = indices.get(types[i]);
            periods[i] = sampling(types[i], index);
            indices.put(types[i], index + 1);
            // Update the name of the sensor
            naming(types[i], index, names[i]);
            // Clip period based on minimum delay reported
            if (0 < periods[i]) {
                periods[i] = Math.max(periods[i], delays[i] / 1000);
                String message = String.format("The period for sensor #%d (%s) is %d", i, identifier, periods[i]);
                Log.i(TAG, message);
            }
        }
        sampler.data().initiate(sizes, periods, storing());
        try {
            String report = Report.report(sampler.context(), types, vendors, names, resolutions, delays, null, null);
            Storage.writeText("device.json", report);
        } catch (JSONException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to report on device specification:\n%s", trace);
            Log.e(TAG, message);
        }
        return periods;
    }
}
