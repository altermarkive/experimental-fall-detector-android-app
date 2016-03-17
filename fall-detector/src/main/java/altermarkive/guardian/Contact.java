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
