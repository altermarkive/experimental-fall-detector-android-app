package android.code.tabs;

import java.util.Objects;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

public class Main extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private TabLayout tabs;
    private FrameLayout content;
    private int counter = 1;

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        show(tabs.getSelectedTabPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        show(tabs.getSelectedTabPosition());
    }

    private static class Spec {
        public final String title;
        public final View content;

        public Spec(String title, View content) {
            this.title = title;
            this.content = content;
        }
    }

    private final Vector<Spec> specs = new Vector<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tabs = findViewById(R.id.tabs);
        content = findViewById(R.id.content);
        for (int i = 0; i < 10; i++) {
            add();
        }
        status("Ready");
        tabs.addOnTabSelectedListener(this);
    }

    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return (true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add) {
            add();
            return (true);
        }
        if (itemId == R.id.remove) {
            remove();
            return (true);
        }
        return (false);
    }

    private void status(final String text) {
        final TextView status = findViewById(R.id.status);
        runOnUiThread(() -> status.setText(text));
    }

    private void refresh() {
        tabs.removeAllTabs();
        for (int index = 0; index < specs.size(); index++) {
            Spec spec = specs.get(index);
            final int position = index;
            runOnUiThread(() -> {
                TabLayout.Tab firstTab = tabs.newTab();
                firstTab.setText(spec.title);
                tabs.addTab(firstTab, position, position == 0);
            });
        }
        select(0);
        show(0);
    }

    @SuppressWarnings("SameParameterValue")
    private void select(int index) {
        Objects.requireNonNull(tabs.getTabAt(index)).select();
    }

    private void show(int index) {
        content.removeAllViews();
        content.addView(specs.get(index).content);
    }

    private void add() {
        String title = "Tab " + counter++;
        final Content content = new Content(this);
        content.title = title;
        content.setClickable(true);
        specs.add(new Spec(title, content));
        status("Added " + title);
        refresh();
    }

    private void remove() {
        int index = tabs.getSelectedTabPosition();
        String title = specs.get(index).title;
        specs.remove(index);
        status("Removed " + title);
        refresh();
    }
}
