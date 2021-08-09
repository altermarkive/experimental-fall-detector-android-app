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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class Upload implements Runnable {
    private final static String TAG = Upload.class.getName();

    private final static FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".zip");
        }
    };

    private static volatile Upload instance = null;
    private String url = null;

    public static synchronized Upload instance() {
        if (instance == null) {
            instance = new Upload();
        }
        return instance;
    }

    private final Thread thread;

    private Upload() {
        thread = new Thread(this);
        thread.start();
    }

    public void initiate(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            String[] files = Storage.list(filter);
            if (files != null && 0 < files.length) {
                Arrays.sort(files);
                String item = files[0];
                int hold = 1;
                while (hold < 0x8000) {
                    try {
                        if (upload(url, Data.MIME, content(item), item)) {
                            Storage.delete(item);
                            item = null;
                            break;
                        }
                    } catch (IOException exception) {
                        String trace = Log.getStackTraceString(exception);
                        String message = String.format("Failed to upload '%s':\n%s", item, trace);
                        Log.e(TAG, message);
                    }
                    Log.i(TAG, String.format("Will retry uploading in %d minutes", hold));
                    try {
                        Thread.sleep(hold * 60000);
                    } catch (InterruptedException exception) {
                        String trace = Log.getStackTraceString(exception);
                        String message = String.format("Interrupted during a back-off:\n%s", trace);
                        Log.i(TAG, message);
                    }
                    hold <<= 1;
                }
                if (item != null) {
                    String message = String.format("Failed to upload, skipping '%s'", item);
                    Log.e(TAG, message);
                }
            } else {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException exception) {
                    String trace = Log.getStackTraceString(exception);
                    String message = String.format("Interrupted while waiting:\n%s", trace);
                    Log.i(TAG, message);
                }
            }
        }
    }

    private static byte[] content(String file) {
        long size = Storage.size(file);
        if (Integer.MAX_VALUE < size) {
            String message = String.format("Cannot read file %s into memory", file);
            Log.e(TAG, message);
            return null;
        }
        byte[] array = new byte[(int) size];
        if (!Storage.readBinary(file, array)) {
            String message = String.format("Failed to read the file %s", file);
            Log.e(TAG, message);
            return null;
        }
        return array;
    }


    private static boolean upload(String url, String mime, byte[] content, String thing) throws
            IOException {
        if (content == null) {
            String message = String.format("Failed to read the content of '%s'", thing);
            Log.e(TAG, message);
            return false;
        }
        if (url == null || url.length() == 0) {
            String message = String.format("Uploading '%s' skipped (URL not configured)", thing);
            Log.i(TAG, message);
            return false;
        }
        String message = String.format("Uploading '%s' to %s", thing, url);
        Log.i(TAG, message);
        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", mime);
        OutputStream output = connection.getOutputStream();
        output.write(content);
        InputStream input = connection.getInputStream();
        //noinspection ResultOfMethodCallIgnored
        input.skip(input.available());
        return true;
    }

    public void poke() {
        thread.interrupt();
    }
}
