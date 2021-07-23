package xyz.acygn.mokapot.test.bytecode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.IntSupplier;
import xyz.acygn.mokapot.markers.Copiable;

/**
 * A program that generates two lambdas which are identical, except for their
 * marker interfaces. The aim is to see how their serialisations compare.
 * <p>
 * At the time of writing, at least some Java compilers serialise the two
 * lambdas (which are almost but not quite identical) into the same sequence of
 * bits, thus causing one of them to deserialise incorrectly.
 *
 * @author Alex Smith
 */
public class LambdaMarkers {

    /**
     * Main function. Creates two lambdas and compares them.
     *
     * @param args Ignored.
     * @throws IOException If something goes wrong serialising the lambdas
     * @throws ClassNotFoundException If the serialised lambdas contain invalid
     * class information
     */
    public static void main(String[] args)
            throws IOException, ClassNotFoundException {
        IntSupplier s1 = (IntSupplier & Serializable) LambdaMarkers::lambdaBody;
        IntSupplier s2 = (IntSupplier & Serializable & Copiable) LambdaMarkers::lambdaBody;
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        ObjectOutputStream serializer = new ObjectOutputStream(serialized);
        serializer.writeObject(s1);
        serializer.writeObject(s2);

        ByteArrayInputStream fromSerialized
                = new ByteArrayInputStream(serialized.toByteArray());
        ObjectInputStream deserializer = new ObjectInputStream(fromSerialized);
        Object s1d = deserializer.readObject();
        Object s2d = deserializer.readObject();

        System.out.println(s1 instanceof Copiable);
        System.out.println(s2 instanceof Copiable);
        System.out.println(s1d instanceof Copiable);
        System.out.println(s2d instanceof Copiable);
    }

    /**
     * Body of the lambdas. Not very interesting, but exists to allow all the
     * lambdas to be implemented via the same underlying code.
     *
     * @return The constant integer 5.
     */
    private static int lambdaBody() {
        return 5;
    }

    /**
     * Inaccessible constructor. This class is not intended to have instances.
     */
    private LambdaMarkers() {
    }
}
