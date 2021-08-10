package altermarkive.guardian

import android.util.Log
import java.util.*

object Log {
    var LOG_FILE = "runtime.log"
    fun log(priority: String?, tag: String?, entry: String?) {
        var entry = entry
        entry = String.format("%s\n%s: %s\n%s\n\n", Date().toString(), priority, tag, entry)
        Storage.appendText(LOG_FILE, entry)
    }

    fun v(tag: String?, entry: String?) {
        //android.util.Log.v(tag, entry);
        log("VERBOSE", tag, entry)
    }

    fun d(tag: String?, entry: String?) {
        //android.util.Log.d(tag, entry);
        log("DEBUG", tag, entry)
    }

    fun i(tag: String?, entry: String?) {
        //android.util.Log.i(tag, entry);
        log("INFO", tag, entry)
    }

    fun w(tag: String?, entry: String?) {
        //android.util.Log.w(tag, entry);
        log("WARN", tag, entry)
    }

    fun e(tag: String?, entry: String?) {
        //android.util.Log.e(tag, entry);
        log("ERROR", tag, entry)
    }

    fun getStackTraceString(throwable: Throwable?): String {
        return Log.getStackTraceString(throwable)
    }
}