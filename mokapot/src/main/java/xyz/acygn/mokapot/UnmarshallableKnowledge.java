package xyz.acygn.mokapot;

import java.io.DataOutput;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * A class representing knowledge about classes that cannot be marshalled via
 * any known method. These will be avoided in marshalling as much as possible,
 * and throw an exception if an attempt is made to marshal them.
 * <p>
 * This category is fairly disappointing, but needs to exist due to classes such
 * as <code>SecurityManager</code>, which clearly cannot be marshalled, neither
 * by copy nor by reference.
 *
 * @author Alex Smith
 * @param <T> The class that this knowledge is about.
 */
class UnmarshallableKnowledge<T> extends ClassKnowledge<T> {

    /**
     * The reason that objects of this class can't be created by loading the
     * class.
     */
    private final Exception whyNotLoadable;

    /**
     * *
     * The reason that objects of this class can't be created via a factory
     * method.
     */
    private final Exception whyNotFactoryConstructable;

    /**
     * Creates new knowledge that the specified class cannot be marshalled.
     *
     * @param about The class that this knowledge is about.
     * @param whyNotLoadable Why this class cannot be unmarshalled via loading
     * the class from disk.
     * @param whyNotFactoryConstructable Why this class cannot be unmarshalled
     * via constructing new objects using a factory method.
     */
    UnmarshallableKnowledge(Class<T> about, Exception whyNotLoadable,
            Exception whyNotFactoryConstructable) {
        super(about);
        this.whyNotLoadable = whyNotLoadable;
        this.whyNotFactoryConstructable = whyNotFactoryConstructable;
    }

    /**
     * Always returns false. This object does not unmarshal reliably; in fact,
     * it never unmarshals correctly, which is the exact opposite of always
     * unmarshalling correctly.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return false;
    }

    /**
     * Always returns false. Unmarshalling this object does not create a copy
     * because it does not succeed at all.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return false;
    }

    @Override
    ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable) {
        throw new DistributedError(whyNotLoadable,
                "Attempted to describe an unmarshallable object of "
                + this.getAbout());
    }

    @Override
    void writeFieldDescriptionTo(DataOutput sink,
            Object fieldValue, boolean nullable) {
        throw new DistributedError(whyNotLoadable,
                "Attempted to describe an unmarshallable object of "
                + this.getAbout());
    }

    @Override
    T reproduce(ReadableDescription description, boolean nullable) {
        throw new DistributedError(whyNotLoadable,
                "Attempted to reproduce an unmarshallable object of "
                + this.getAbout());
    }
}
