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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.InputType;

public class Selector extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.selector);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.selector));
        for (int i = Config.MIN_TYPE; i <= Config.MAX_TYPE; i++) {
            for (int j = 0; j < 100; j++) {
                String key = String.format(getString(R.string.naming), i, j);
                String name = preferences.getString(key, null);
                if (name == null) {
                    break;
                }
                EditTextPreference preference = new EditTextPreference(this);
                key = String.format(getString(R.string.sampling), i, j);
                preference.setKey(key);
                preference.setTitle(name);
                preference.setSummary("Sampling period (in milliseconds) for this sensor");
                preference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                category.addPreference(preference);
            }
        }
    }
}
