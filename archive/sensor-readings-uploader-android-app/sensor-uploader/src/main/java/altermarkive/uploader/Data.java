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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Data implements Runnable {
    private final static String TAG = Data.class.getName();
    public final static String MIME = "application/zip";

    private final ByteBuffer bytes = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
    private final Vector<Batch> queue = new Vector<Batch>();
    private int[] portions = new int[0];
    private int storing = 0;
    private long schedule = 0;

    public Data() {
        new Thread(this).start();
    }

    public void initiate(int[] sizes, int[] periods, int storing) {
        this.storing = 0;
        // Calculate the batch portions
        int[] portions = new int[sizes.length];
        Arrays.fill(portions, 0);
        for (int i = 0; i < sizes.length; i++) {
            if (periods[i] != 0) {
                portions[i] = sizes[i] * (int) Math.ceil((double) storing / (double) periods[i]);
            }
        }
        // Initiate the batch queue
        queue.clear();
        this.portions = portions;
        this.storing = storing;
        advance();
    }

    @SuppressWarnings("unused")
    public synchronized void dispatch(int type, int index, long stamp, float[] values, int axes) {
        if (storing != 0) {
            bytes.position(0);
            bytes.putLong(stamp);
            for (int i = 0; i < axes; i++) {
                bytes.putFloat(values[i]);
            }
            Batch batch = find(index);
            batch.append(index, bytes.array(), bytes.position());
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            while (storing != 0 && 1 < queue.size()) {
                Batch batch = queue.remove(0);
                zip(batch);
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException exception) {
                    String trace = Log.getStackTraceString(exception);
                    String message = String.format("Data thread was interrupted:\n%s", trace);
                    Log.d(TAG, message);
                }
            }
        }
    }

    private Batch advance() {
        Batch batch = new Batch(portions);
        queue.add(batch);
        if (schedule == 0) {
            schedule = System.currentTimeMillis();
        }
        schedule += storing;
        return batch;
    }

    private Batch find(int index) {
        Batch last = queue.lastElement();
        if (schedule < System.currentTimeMillis() || last.full(index)) {
            return advance();
        } else {
            return last;
        }
    }

    private void zip(Batch batch) {
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        ZipOutputStream stream = new ZipOutputStream(zipped);
        try {
            zipTextFile(stream, "device.json");
            zipTextFile(stream, Log.LOG_FILE);
            Storage.writeText(Log.LOG_FILE, "");
            batch.zip(stream);
            stream.close();
            byte[] content = zipped.toByteArray();
            String name = String.format("data.%016X.zip", batch.stamp());
            if (!Storage.writeBinary(name, content)) {
                Log.e(TAG, "Failed to write a ZIP file");
            }
        } catch (IOException exception) {
            Log.e(TAG, "Failed to create a ZIP file:\n" + Log.getStackTraceString(exception));
        }
    }

    private void zipTextFile(ZipOutputStream stream, String file) throws IOException {
        ZipEntry entry = new ZipEntry(file);
        String content = Storage.readText(file);
        if (content != null) {
            byte[] bytes = content.getBytes();
            entry.setSize(bytes.length);
            stream.putNextEntry(entry);
            stream.write(bytes, 0, bytes.length);
            stream.closeEntry();
        }
    }
}
