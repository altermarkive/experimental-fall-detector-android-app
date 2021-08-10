package altermarkive.guardian

import android.os.Environment
import android.util.Log
import java.io.*

object Storage {
    private val TAG = Storage::class.java.name
    private val PREFIX = prefix()
    private fun prefix(): File? {
        val self = Storage::class.java.getPackage().name
        val root = Environment.getExternalStorageDirectory()
        val directory = File(root, "Data" + File.separator + self)
        if (!directory.exists() && !directory.mkdirs()) {
            val message = String.format("Failed to make path for '%s'", directory)
            Log.d(TAG, message)
            return null
        }
        return directory
    }

    fun size(file: String?): Long {
        val path = File(PREFIX, file)
        return path.length()
    }

    fun readText(file: String?): String? {
        val size = size(file)
        if (Int.MAX_VALUE < size) {
            Log.d(TAG, "Cannot read file into memory")
            return null
        }
        val buffer = ByteArray(size.toInt())
        return if (readBinary(file, buffer)) {
            String(buffer)
        } else {
            null
        }
    }

    fun writeText(file: String?, content: String): String? {
        return if (writeBinary(file, content.toByteArray())) content else null
    }

    fun appendText(file: String?, content: String): String? {
        return if (appendBinary(file, content.toByteArray())) content else null
    }

    fun readBinary(file: String?, content: ByteArray): Boolean {
        val path = File(PREFIX, file)
        val input: InputStream
        input = try {
            FileInputStream(path)
        } catch (exception: FileNotFoundException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to open '%s' for reading:\n%s", file, trace)
            Log.d(TAG, message)
            return false
        }
        var result = true
        try {
            if (input.read(content) < content.size) {
                val message = String.format("Mismatched buffer size for '%s'", file)
                Log.d(TAG, message)
                result = false
            }
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to read '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            input.close()
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to close '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        return result
    }

    fun writeBinary(file: String?, content: ByteArray): Boolean {
        return writeBinary(file, content, false)
    }

    fun appendBinary(file: String?, content: ByteArray): Boolean {
        return writeBinary(file, content, true)
    }

    private fun writeBinary(file: String?, content: ByteArray, append: Boolean): Boolean {
        val path = File(PREFIX, file)
        if (PREFIX!!.freeSpace < content.size) {
            val message = String.format("No space left for '%s'", file)
            Log.d(TAG, message)
            return false
        }
        val output: OutputStream
        output = try {
            FileOutputStream(path, append)
        } catch (exception: FileNotFoundException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to open '%s' for writing:\n%s", file, trace)
            Log.d(TAG, message)
            return false
        }
        var result = true
        try {
            output.write(content)
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to write '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        try {
            output.close()
        } catch (exception: IOException) {
            val trace: String = Log.getStackTraceString(exception)
            val message = String.format("Failed to close '%s':\n%s", file, trace)
            Log.d(TAG, message)
            result = false
        }
        return result
    }

    fun delete(file: String?): Boolean {
        if (!File(PREFIX, file).delete()) {
            val message = String.format("Failed to delete '%s'", file)
            Log.d(TAG, message)
            return false
        }
        return true
    }

    fun list(filter: FilenameFilter?): Array<String> {
        return PREFIX!!.list(filter)
    }
}