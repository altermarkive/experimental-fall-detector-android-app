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
package altermarkive.uploader;

public class Wear {
    private final static String TAG = Wear.class.getName();
    private final static String CLASS = "android.support.wearable.R";

    public static boolean detected() {
        try {
            Class.forName(CLASS);
            Log.i(TAG, "Android Wear detected");
            return true;
        } catch (ClassNotFoundException exception) {
            Log.i(TAG, "Android Wear not detected\n" + Log.getStackTraceString(exception));
            return false;
        }
    }
}
