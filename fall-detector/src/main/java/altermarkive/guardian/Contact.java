/*
This code is free  software: you can redistribute it and/or  modify it under the
terms of the GNU Lesser General Public License as published by the Free Software
Foundation,  either version  3 of  the License,  or (at  your option)  any later
version.

This code  is distributed in the  hope that it  will be useful, but  WITHOUT ANY
WARRANTY; without even the implied warranty  of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Lesser  General Public License for more details.

You should have  received a copy of the GNU  Lesser General Public License along
with code. If not, see http://www.gnu.org/licenses/.
*/
package altermarkive.guardian;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Contact extends Activity implements View.OnClickListener {
    public static String get(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (preferences.getString(context.getString(R.string.contact), ""));
    }

    public static void set(Context context, String contact) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = preferences.edit();
        editor.putString(context.getString(R.string.contact), contact);
        editor.apply();
    }

    public static boolean check(Context context, String contact) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String expected = preferences.getString(context.getString(R.string.contact), "");
        return (PhoneNumberUtils.compare(contact, expected));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact);
        EditText edit = (EditText) findViewById(R.id.contact);
        edit.setText(get(this));
        Button button;
        button = (Button) findViewById(R.id.search);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.ok);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.cancel);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search:
                Intent intent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 0);
                break;
            case R.id.ok:
                EditText edit = (EditText) findViewById(R.id.contact);
                String phone = edit.getText().toString();
                set(this, phone);
            case R.id.cancel:
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (RESULT_OK == resultCode) {
            String selection = Phone.CONTACT_ID + "=?";
            Uri result = data.getData();
            String id = result.getLastPathSegment();
            String[] arguments = new String[]{id};
            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(Phone.CONTENT_URI, null, selection, arguments, null);
            int index = cursor.getColumnIndex(Phone.DATA);
            if (cursor.moveToFirst()) {
                String phone = cursor.getString(index);
                set(this, phone);
                EditText edit = (EditText) findViewById(R.id.contact);
                edit.setText(phone);
            }
            cursor.close();
        }
    }
}
