package altermarkive.uploader;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class Main extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Toast.makeText(this, "You must restart the devices for the changed settings to take effect", Toast.LENGTH_LONG).show();
        Background.initiate(this);
    }
}
