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
