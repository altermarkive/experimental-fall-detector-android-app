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

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Batch {
    private final int[] portions;
    private final Buffer[] buffers;
    private final long stamp;

    public Batch(int[] portions) {
        this.portions = portions;
        buffers = new Buffer[portions.length];
        for (int i = 0; i < portions.length; i++) {
            buffers[i] = new Buffer(portions[i]);
        }
        stamp = System.currentTimeMillis();
    }

    public boolean full(int index) {
        return buffers[index].size() >= portions[index];
    }

    public void append(int index, byte[] bytes, int size) {
        buffers[index].append(bytes, size);
    }

    public long stamp() {
        return stamp;
    }

    public void zip(ZipOutputStream stream) throws IOException {
        for (int i = 0; i < buffers.length; i++) {
            byte[] array = buffers[i].array();
            int size = buffers[i].size();
            if (0 < size) {
                ZipEntry entry = new ZipEntry(String.format("data.%02d.bin", i));
                entry.setSize(size);
                stream.putNextEntry(entry);
                stream.write(array, 0, size);
                stream.closeEntry();
            }
        }
    }
}
