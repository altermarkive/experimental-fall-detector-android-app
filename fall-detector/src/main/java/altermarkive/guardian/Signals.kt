package altermarkive.guardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class Signals : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.signals, container, false)
        val tabs: TabLayout = view.findViewById(R.id.tabs)
        activity?.runOnUiThread {
            for (index in Surface.CHARTS.indices) {
                val tab = tabs.newTab()
                tab.text = Surface.CHARTS[index].label
                tabs.addTab(tab, index, index == 0)
            }
        }
        tabs.addOnTabSelectedListener(view.findViewById(R.id.surface))
        return view
    }
}