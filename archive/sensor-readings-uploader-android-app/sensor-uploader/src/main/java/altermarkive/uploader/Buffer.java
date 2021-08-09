package altermarkive.uploader;

public class Buffer {
    private byte[] array;
    private int size;

    public Buffer(int initial) {
        array = new byte[initial];
        size = 0;
    }

    public byte[] array() {
        return array;
    }

    public int size() {
        return size;
    }

    public void append(byte[] bytes, int size) {
        if (array.length - this.size < size) {
            byte[] extended = new byte[this.size - size];
            System.arraycopy(array, 0, extended, 0, this.size);
            array = extended;
        }
        System.arraycopy(bytes, 0, array, this.size, size);
        this.size += size;
    }
}
