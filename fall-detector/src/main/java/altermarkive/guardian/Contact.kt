package altermarkive.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class Contact : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact)
        val edit = findViewById<View>(R.id.contact) as EditText
        edit.setText(Companion[this])
        val buttonSearch = findViewById<View>(R.id.search) as Button
        buttonSearch.setOnClickListener(this)
        val buttonOk = findViewById<View>(R.id.ok) as Button
        buttonOk.setOnClickListener(this)
        val buttonCancel = findViewById<View>(R.id.cancel) as Button
        buttonCancel.setOnClickListener(this)
    }

    private fun pickContact() {
        val intent = Intent(
            Intent.ACTION_PICK,
            ContactsContract.Contacts.CONTENT_URI
        )
        contactPicker.launch(intent)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.search -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    pickContact()
                } else {
                    Guardian.say(this, android.util.Log.ERROR, TAG, "ERROR: No permission to access contacts")
                }
            }
            R.id.ok -> {
                val edit = findViewById<View>(R.id.contact) as EditText
                val phone = edit.text.toString()
                Companion[this] = phone
                finish()
            }
            R.id.cancel -> finish()
        }
    }

    private val contactPicker = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            if (data == null) {
                Guardian.say(this, android.util.Log.ERROR, TAG, "ERROR: No data after contact selection")
            } else {
                val selection = Phone.CONTACT_ID + "=?"
                val outcome = data.data
                if (outcome == null) {
                    Guardian.say(this, android.util.Log.ERROR, TAG, "ERROR: No outcome after contact selection")
                } else {
                    val id = outcome.lastPathSegment
                    val arguments = arrayOf(id)
                    val resolver = contentResolver
                    val cursor = resolver.query(Phone.CONTENT_URI, null, selection, arguments, null)
                    if (cursor == null) {
                        Guardian.say(this, android.util.Log.ERROR, TAG, "ERROR: Failed to query contacts")
                    } else {
                        val index = cursor.getColumnIndex(Phone.DATA)
                        if (cursor.moveToFirst()) {
                            val phone = cursor.getString(index)
                            if (phone != null) {
                                val edit = findViewById<View>(R.id.contact) as EditText
                                edit.setText(phone)
                            }
                        }
                        cursor.close()
                    }
                }
            }
        }
    }

    companion object {
        internal operator fun get(context: Context): String? {
            val applicationContext: Context = context.applicationContext
            val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return preferences.getString(context.getString(R.string.contact), "")
        }

        internal operator fun set(context: Context, contact: String?) {
            val applicationContext: Context = context.applicationContext
            val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val editor = preferences.edit()
            editor.putString(context.getString(R.string.contact), contact)
            editor.apply()
        }

        internal fun check(context: Context, contact: String?): Boolean {
            val applicationContext: Context = context.applicationContext
            val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val expected = preferences.getString(context.getString(R.string.contact), "")
            return PhoneNumberUtils.compare(contact, expected)
        }

        private val TAG: String = Contact::class.java.simpleName
    }
}