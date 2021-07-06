package altermarkive.guardian;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class About extends Activity implements View.OnClickListener {
    private void eula(Context context) {
        // Run the service
        Guardian.initiate(this);
        // Load the EULA
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.eula);
        dialog.setTitle("EULA");
        WebView web = (WebView) dialog.findViewById(R.id.eula);
        web.loadUrl("file:///android_asset/eula.html");
        Button accept = (Button) dialog.findViewById(R.id.accept);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Detector.initiate(this);
        setContentView(R.layout.about);
        WebView web = (WebView) findViewById(R.id.about);
        web.loadUrl("file:///android_asset/about.html");
        Button help = (Button) findViewById(R.id.help);
        help.setOnClickListener(this);
        Button settings = (Button) findViewById(R.id.settings);
        settings.setOnClickListener(this);
        Button signals = (Button) findViewById(R.id.signals);
        signals.setOnClickListener(this);
        eula(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.settings:
                intent = new Intent(this, Settings.class);
                startActivity(intent);
                return;
            case R.id.help:
                Alarm.call(this);
                return;
            case R.id.signals:
                intent = new Intent(this, Signals.class);
                startActivity(intent);
                return;
        }
    }
}