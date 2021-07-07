package altermarkive.guardian

import altermarkive.guardian.databinding.MainBinding
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class Main : AppCompatActivity() {
    private fun eula(context: Context) {
        // Run the guardian
        Guardian.initiate(this)
        // Load the EULA
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.eula)
        dialog.setTitle("EULA")
        val web = dialog.findViewById<View>(R.id.eula) as WebView
        web.loadUrl("file:///android_asset/eula.html")
        val accept = dialog.findViewById<View>(R.id.accept) as Button
        accept.setOnClickListener { dialog.dismiss() }
        val layout = WindowManager.LayoutParams()
        val window = dialog.window
        window ?: return
        layout.copyFrom(window.attributes)
        layout.width = WindowManager.LayoutParams.MATCH_PARENT
        layout.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = layout
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Detector.instance(this)
        val binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration =
            AppBarConfiguration(setOf(R.id.about, R.id.signals, R.id.settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        eula(this)
    }
}
