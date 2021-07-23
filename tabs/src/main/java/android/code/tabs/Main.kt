package android.code.tabs

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import java.util.*

class Main : AppCompatActivity(), OnTabSelectedListener {
    private var tabs: TabLayout? = null
    private var content: FrameLayout? = null
    private var counter = 1

    override fun onTabSelected(tab: TabLayout.Tab) {
        val tabs = tabs ?: return
        show(tabs.selectedTabPosition)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {
        val tabs = tabs ?: return
        show(tabs.selectedTabPosition)
    }

    private class Spec(val title: String, val content: View)

    private val specs = Vector<Spec>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val tabs: TabLayout? = findViewById(R.id.tabs)
        this.tabs = tabs
        content = findViewById(R.id.content)
        for (i in 0..9) {
            add()
        }
        status("Ready")
        tabs?.addOnTabSelectedListener(this)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.add) {
            add()
            return true
        }
        if (itemId == R.id.remove) {
            remove()
            return true
        }
        return false
    }

    private fun status(text: String) {
        val status = findViewById<TextView>(R.id.status)
        runOnUiThread { status.text = text }
    }

    private fun refresh() {
        val tabs = tabs ?: return
        tabs.removeAllTabs()
        for (index in specs.indices) {
            val spec = specs[index]
            runOnUiThread {
                val firstTab = tabs.newTab()
                firstTab.text = spec.title
                tabs.addTab(firstTab, index, index == 0)
            }
        }
        select(0)
        show(0)
    }

    @Suppress("SameParameterValue")
    private fun select(index: Int) {
        tabs?.getTabAt(index)?.select()
    }

    private fun show(index: Int) {
        val content = content ?: return
        content.removeAllViews()
        content.addView(specs[index].content)
    }

    private fun add() {
        val title = "Tab " + counter++
        val content = Content(this)
        content.title = title
        content.isClickable = true
        specs.add(Spec(title, content))
        status("Added $title")
        refresh()
    }

    private fun remove() {
        val tabs = tabs ?: return
        val index = tabs.selectedTabPosition
        val title = specs[index].title
        specs.removeAt(index)
        status("Removed $title")
        refresh()
    }
}