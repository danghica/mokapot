package xyz.acygn.mokapot.wireformat;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A <code>DataInputStream</code> that pretends to conform to the
 * <code>ReadableDescription</code> API but doesn't implement any methods
 * related to noncopiable objects. This is used to read descriptions directly
 * from an input stream without needing to go via an intermediate buffer first,
 * helping to solve various chicken-and-egg problems related to marshaling (in
 * particular, the issue of needing something that isn't an object in order to
 * be able to produce the first object).
 *
 * @author Alex Smith
 */
public class FakeDescriptionStream
        extends DataInputStream implements ReadableDescription {

    /**
     * Creates a fake description stream via wrapping the given input stream.
     *
     * @param stream The input stream to wrap.
     */
    public FakeDescriptionStream(InputStream stream) {
        super(stream);
    }

    @Override
    public <T> T readNoncopiableObject(Class<T> asClass) {
        throw new UnsupportedOperationException(
                "Objects read directly from a stream must be copiable");
    }

    @Override
    public ByteBuffer readByteSlice(int length) throws IOException {
        byte[] storage = new byte[length];
        int readLength = 0;
        while (readLength < length) {
            readLength += read(storage, readLength, length - readLength);
        }
        return ByteBuffer.wrap(storage);
    }
}
