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
