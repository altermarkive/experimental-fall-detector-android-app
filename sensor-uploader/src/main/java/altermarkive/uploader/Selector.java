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
