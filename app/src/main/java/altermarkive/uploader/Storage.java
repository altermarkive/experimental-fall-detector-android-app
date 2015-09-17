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

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Storage {
    private final static String TAG = Storage.class.getName();
    private final static File PREFIX = prefix();

    private static File prefix() {
        String self = Storage.class.getPackage().getName();
        File root = Environment.getExternalStorageDirectory();
        File directory = new File(root, "Data" + File.separator + self);
        if (!directory.exists() && !directory.mkdirs()) {
            String message = String.format("Failed to make path for '%s'", directory);
            android.util.Log.d(TAG, message);
            return null;
        }
        return directory;
    }

    public static long size(String file) {
        File path = new File(PREFIX, file);
        return path.length();
    }

    public static String readText(String file) {
        long size = size(file);
        if (Integer.MAX_VALUE < size) {
            android.util.Log.d(TAG, "Cannot read file into memory");
            return null;
        }
        byte[] buffer = new byte[(int) size];
        if (readBinary(file, buffer)) {
            return new String(buffer);
        } else {
            return null;
        }
    }

    public static String writeText(String file, String content) {
        return writeBinary(file, content.getBytes()) ? content : null;
    }

    public static String appendText(String file, String content) {
        return appendBinary(file, content.getBytes()) ? content : null;
    }

    public static boolean readBinary(String file, byte[] content) {
        File path = new File(PREFIX, file);
        InputStream input;
        try {
            input = new FileInputStream(path);
        } catch (FileNotFoundException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to open '%s' for reading:\n%s", file, trace);
            android.util.Log.d(TAG, message);
            return false;
        }
        boolean result = true;
        try {
            if (input.read(content) < content.length) {
                String message = String.format("Mismatched buffer size for '%s'", file);
                android.util.Log.d(TAG, message);
                result = false;
            }
        } catch (IOException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to read '%s':\n%s", file, trace);
            android.util.Log.d(TAG, message);
            result = false;
        }
        try {
            input.close();
        } catch (IOException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to close '%s':\n%s", file, trace);
            android.util.Log.d(TAG, message);
            result = false;
        }
        return result;
    }

    public static boolean writeBinary(String file, byte[] content) {
        return writeBinary(file, content, false);
    }

    public static boolean appendBinary(String file, byte[] content) {
        return writeBinary(file, content, true);
    }

    private static boolean writeBinary(String file, byte[] content, boolean append) {
        File path = new File(PREFIX, file);
        if (PREFIX.getFreeSpace() < content.length) {
            String message = String.format("No space left for '%s'", file);
            android.util.Log.d(TAG, message);
            return false;
        }
        OutputStream output;
        try {
            output = new FileOutputStream(path, append);
        } catch (FileNotFoundException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to open '%s' for writing:\n%s", file, trace);
            android.util.Log.d(TAG, message);
            return false;
        }
        boolean result = true;
        try {
            output.write(content);
        } catch (IOException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to write '%s':\n%s", file, trace);
            android.util.Log.d(TAG, message);
            result = false;
        }
        try {
            output.close();
        } catch (IOException exception) {
            String trace = Log.getStackTraceString(exception);
            String message = String.format("Failed to close '%s':\n%s", file, trace);
            android.util.Log.d(TAG, message);
            result = false;
        }
        return result;
    }

    public static boolean rename(String ante, String post) {
        File anteFile = new File(PREFIX, ante);
        File postFile = new File(PREFIX, post);
        if (!anteFile.renameTo(postFile)) {
            String message = String.format("Failed to rename a file from '%s' to '%s'", ante, post);
            android.util.Log.d(TAG, message);
            return false;
        }
        return true;
    }

    public static boolean delete(String file) {
        if (!new File(PREFIX, file).delete()) {
            String message = String.format("Failed to delete '%s'", file);
            android.util.Log.d(TAG, message);
            return false;
        }
        return true;
    }

    public static String[] list() {
        return PREFIX.list();
    }

    public static String[] list(FilenameFilter filter) {
        return PREFIX.list(filter);
    }
}
