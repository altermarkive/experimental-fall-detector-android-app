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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Upload implements Runnable {
    public abstract static class Item {
        private final String url;
        private final String mime;

        public Item(String url, String mime) {
            this.url = url;
            this.mime = mime;
        }

        public String url() {
            return url;
        }

        public String mime() {
            return mime;
        }

        public abstract byte[] content();

        public abstract void terminate();
    }

    private final static String TAG = Upload.class.getName();

    private static volatile Upload instance = null;

    public static synchronized Upload instance() {
        if (instance == null) {
            instance = new Upload();
        }
        return instance;
    }

    private final BlockingQueue<Item> queue = new LinkedBlockingQueue<Item>();
    private final Thread thread;

    private Upload() {
        thread = new Thread(this);
        thread.start();
    }

    public void enqueue(Item item) {
        while (true) {
            try {
                queue.put(item);
                break;
            } catch (InterruptedException exception) {
                String trace = Log.getStackTraceString(exception);
                String message = String.format("Interrupted during upload enqueue:\n%s", trace);
                Log.d(TAG, message);
            }
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            Item item;
            try {
                item = queue.take();
            } catch (InterruptedException exception) {
                String trace = Log.getStackTraceString(exception);
                String message = String.format("Interrupted during upload dequeue:\n%s", trace);
                Log.d(TAG, message);
                continue;
            }
            int hold = 1;
            while (hold < 0x8000) {
                try {
                    upload(item);
                    item = null;
                    break;
                } catch (IOException exception) {
                    String trace = Log.getStackTraceString(exception);
                    String message = String.format("Failed to upload '%s':\n%s", item.toString(), trace);
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
                String message = String.format("Failed to upload, skipping '%s'", item.toString());
                Log.e(TAG, message);
            }
        }
    }

    private static void upload(Item item) throws IOException {
        String url = item.url();
        String mime = item.mime();
        String thing = item.toString();
        byte[] content = item.content();
        if (url == null || url.length() == 0) {
            String message = String.format("Uploading '%s' skipped (URL not configured)", thing);
            Log.i(TAG, message);
            return;
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
        item.terminate();
    }

    public void poke() {
        thread.interrupt();
    }
}