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
