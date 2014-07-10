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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Data implements Runnable {
    public class FileItem extends Upload.Item {
        private final String file;

        public FileItem(String url, String mime, String file) {
            super(url, mime);
            this.file = file;
        }

        public byte[] content() {
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

        public void terminate() {
            Storage.delete(file);
        }

        public String toString() {
            return file;
        }
    }

    private final static String TAG = Data.class.getName();
    private final static String MIME = "application/zip";

    private final Sampler sampler;
    private final Vector<Batch> queue = new Vector<Batch>();
    private int[] portions = new int[0];
    private int storing = 0;
    private long schedule = 0;

    public Data(Sampler sampler) {
        this.sampler = sampler;
        new Thread(this).start();
    }

    public void initiate(int[] sizes, int[] intervals, int storing) {
        this.storing = 0;
        // Calculate the batch portions
        int[] portions = new int[sizes.length];
        Arrays.fill(portions, 0);
        for (int i = 0; i < sizes.length; i++) {
            if (intervals[i] != 0) {
                portions[i] = sizes[i] * (int) Math.ceil((double) storing / (double) intervals[i]);
            }
        }
        // Initiate the batch queue
        queue.clear();
        this.portions = portions;
        this.storing = storing;
        advance();
    }

    @SuppressWarnings("unused")
    public void dispatch(int index, byte[] exchange, int size) {
        if (storing != 0) {
            Batch batch = find(index);
            batch.append(index, exchange, size);
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
            zipTextFile(stream, "preferences.json");
            zipTextFile(stream, Log.LOG_FILE);
            Storage.writeText(Log.LOG_FILE, "");
            batch.zip(stream);
            stream.close();
            byte[] content = zipped.toByteArray();
            String name = String.format("data.%016X.zip", batch.stamp());
            if (Storage.writeBinary(name, content)) {
                sampler.upload().enqueue(new FileItem(sampler.config().url(), MIME, name));
            } else {
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
