package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about how to marshal an enum constant. Unlike with almost every
 * class, where unmarshalling an object produces a new object, unmarshalling an
 * enum constant produces an existing value (the pre-existing enum constant),
 * and thus it's a special case in the marshalling system.
 *
 * @author Alex Smith
 * @param <T> The class that this knowledge is about.
 */
class EnumKnowledge<T extends Enum<?>> extends ClassKnowledge<T> {

    /**
     * Creates new class knowledge for the given enum class.
     *
     * @param about The class that this enum is about.
     */
    EnumKnowledge(Class<T> about) {
        super(about);
    }

    /**
     * Throws an exception. Enum indexes have a fixed size, so there should
     * never be a need to inspect the enum to determine what it is.
     *
     * @param value Ignored.
     * @param nullable Ignored.
     * @return Never returns.
     * @throws IllegalStateException Always
     */
    @Override
    ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable) {
        throw new IllegalStateException(
                "There should be no need to calculate a description size "
                + "of an enum; it should already be known");
    }

    /**
     * Describes this enum, writing the description to a given data output sink.
     *
     * @param sink The sink to write the enum description to.
     * @param fieldValue The enum constant to describe.
     * @param nullable Ignored; no extra space in the description is necessary
     * to represent <code>null</code>.
     * @throws IOException
     */
    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws IOException {
        if (fieldValue == null) {
            sink.writeInt(-1);
            return;
        }

        T castValue = getAbout().cast(fieldValue);
        int ordinal = castValue.ordinal();
        sink.writeInt(ordinal);
    }

    /**
     * Reproduces this enum from a given description.
     *
     * @param description The description to reproduce from.
     * @param nullable Ignored.
     * @return The enum constant encoded in the description.
     * @throws IOException If the description appears to be corrupted, or the
     * enum was recorded with an unknown ordinal (e.g. due to differences in the
     * enum definition between systems)
     */
    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws IOException {
        int ordinal = description.readInt();
        if (ordinal == -1) {
            return null;
        }
        try {
            return getAbout().getEnumConstants()[ordinal];
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
            throw new IOException("Unknown ordinal " + ordinal + " for "
                    + getAbout());
        }
    }

    /**
     * Always returns true. Assuming that the definitions on the two systems are
     * compatible, and that the enum constants aren't mutable (which is normally
     * a safe assumption), marshalling an enum constant is always safe. (If the
     * enum constants <i>are</i> mutable, it can cause problems even if no enum
     * is ever marshalled, so marking enum classes unreliable would have no real
     * benefits.)
     *
     * @param asField Ignored.
     * @return <code>true</code>.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return true;
    }

    /**
     * Always returns true. For enum constants, when used in the intended way,
     * deep, shallow and reference copies are all equivalent (meaning a copy is
     * appropriate), and creating a signpost or inheritance-based standin is
     * impossible (meaning a copy is the only option).
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return true;
    }
}
