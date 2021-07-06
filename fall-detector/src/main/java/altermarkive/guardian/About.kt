package altermarkive.guardian

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color.GREEN
import android.graphics.Color.RED
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class About : AppCompatActivity(), View.OnClickListener {
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
        layout.copyFrom(dialog.window!!.attributes)
        layout.width = WindowManager.LayoutParams.MATCH_PARENT
        layout.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = layout
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Detector.instance(this)
        setContentView(R.layout.about)
        val web = findViewById<View>(R.id.about) as WebView
        web.loadUrl("file:///android_asset/about.html")
        val help = findViewById<View>(R.id.help) as Button
        help.setOnClickListener(this)
        val settings = findViewById<View>(R.id.settings) as Button
        settings.setOnClickListener(this)
        val signals = findViewById<View>(R.id.signals) as Button
        signals.setOnClickListener(this)
        eula(this)
    }

    override fun onClick(view: View) {
        val intent: Intent
        when (view.id) {
            R.id.settings -> {
                intent = Intent(this, Settings::class.java)
                startActivity(intent)
                return
            }
            R.id.help -> {
                Alarm.alert(this)
                return
            }
            R.id.signals -> {
                intent = Intent(this, Signals::class.java)
                startActivity(intent)
                return
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshPermissions(true)
    }

    private fun refreshPermissions(request: Boolean) {
        val statusText: String
        val statusColor: Int
        if (permitted(request)) {
            statusText = "Permissions: Granted"
            statusColor = GREEN
        } else {
            statusText = "Permissions: Missing"
            statusColor = RED
        }
        val status = findViewById<View>(R.id.status) as TextView
        status.text = statusText
        status.setTextColor(statusColor)
    }

    @Suppress("SameParameterValue")
    private fun permitted(request: Boolean): Boolean {
        val list: MutableList<String> = mutableListOf()
        var granted = true
        for (item: VersionedPermission in PERMISSIONS) {
            list.add(item.permission)
            if (Build.VERSION.SDK_INT >= item.version && ContextCompat.checkSelfPermission(
                    this,
                    item.permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                granted = false
            }
        }
        if (!granted) {
            if (request) {
                ActivityCompat.requestPermissions(
                    this,
                    list.toTypedArray(),
                    REQUEST_CODE_PERMISSION_EACH
                )
            }
        }
        return granted
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_EACH -> {
                if (grantResults.isNotEmpty()) {
                    for (result: Int in grantResults) {
                        if (PackageManager.PERMISSION_GRANTED != result) {
                            Guardian.say(
                                this,
                                Log.ERROR,
                                TAG,
                                "ERROR: Permissions were not granted"
                            )
                        }
                    }
                }
                refreshPermissions(false)
            }
        }
    }

    class VersionedPermission(val permission: String, val version: Int)

    companion object {
        private const val REQUEST_CODE_PERMISSION_EACH: Int = 0x0000C0DE

        private val PERMISSIONS: Array<VersionedPermission> = arrayOf(
            VersionedPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.CALL_PHONE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.CHANGE_WIFI_STATE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.INTERNET,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_CONTACTS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_PHONE_STATE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_CALL_LOG,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                "android.permission.ANSWER_PHONE_CALLS",
                Build.VERSION_CODES.O
            ),
            VersionedPermission(
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.RECEIVE_SMS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.SEND_SMS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                "android.permission.FOREGROUND_SERVICE",
                Build.VERSION_CODES.P
            ),
        )

        private val TAG: String = About::class.java.simpleName
    }
}
