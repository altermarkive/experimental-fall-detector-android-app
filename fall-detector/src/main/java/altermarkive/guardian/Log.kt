package altermarkive.uploader;

import java.util.Date;

public class Log {
    public static String LOG_FILE = "runtime.log";

    public static void log(String priority, String tag, String entry) {
        entry = String.format("%s\n%s: %s\n%s\n\n", new Date().toString(), priority, tag, entry);
        Storage.appendText(LOG_FILE, entry);
    }

    @SuppressWarnings("unused")
    public static void v(String tag, String entry) {
        //android.util.Log.v(tag, entry);
        log("VERBOSE", tag, entry);
    }

    @SuppressWarnings("unused")
    public static void d(String tag, String entry) {
        //android.util.Log.d(tag, entry);
        log("DEBUG", tag, entry);
    }

    @SuppressWarnings("unused")
    public static void i(String tag, String entry) {
        //android.util.Log.i(tag, entry);
        log("INFO", tag, entry);
    }

    @SuppressWarnings("unused")
    public static void w(String tag, String entry) {
        //android.util.Log.w(tag, entry);
        log("WARN", tag, entry);
    }

    @SuppressWarnings("unused")
    public static void e(String tag, String entry) {
        //android.util.Log.e(tag, entry);
        log("ERROR", tag, entry);
    }

    public static String getStackTraceString(Throwable throwable) {
        return android.util.Log.getStackTraceString(throwable);
    }
}
