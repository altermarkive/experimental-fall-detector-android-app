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
