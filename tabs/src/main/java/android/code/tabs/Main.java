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
package android.code.tabs;

import java.util.Vector;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class Main extends Activity {
    private TabHost tabbed;
    private int counter = 1;
    private final Vector<TabSpec> tabs = new Vector<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tabbed = (TabHost) findViewById(android.R.id.tabhost);
        tabbed.setup();
        for (int i = 0; i < 10; i++) {
            add();
        }
        status("Ready");
    }

    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return (true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                add();
                return (true);
            case R.id.remove:
                remove();
                return (true);
        }
        return (false);
    }

    private void status(final String text) {
        final TextView status = (TextView) findViewById(R.id.status);
        runOnUiThread(new Runnable() {
            public void run() {
                status.setText(text);
            }
        });
    }

    private void refresh() {
        tabbed.setCurrentTab(0);
        tabbed.clearAllTabs();
        for (final TabSpec spec : tabs) {
            runOnUiThread(new Runnable() {
                public void run() {
                    tabbed.addTab(spec);
                    tabbed.requestLayout();
                }
            });
        }
    }

    private void add() {
        String title = "Tab " + Integer.toString(counter++);
        status(title);
        final Content content = new Content(this, title);
        content.setClickable(true);
        final TabSpec spec = tabbed.newTabSpec(title);
        spec.setContent(new TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return (content);
            }
        });
        View tab = View.inflate(this, R.layout.tab, null);
        ((TextView) tab.findViewById(R.id.text)).setText(title);
        spec.setIndicator(tab);
        tabs.add(spec);
        refresh();
        tabbed.setCurrentTab(tabs.size() - 1);
    }

    private void remove() {
        int index = tabbed.getCurrentTab();
        tabs.remove(index);
        refresh();
    }
}
