package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;

/**
 * An interface that parameterises over
 * <code>ClassKnowledge#describeFieldInto</code> and
 * <code>ClassKnowledge#writeFieldDescriptionTo</code>, allowing both to be
 * implemented via calling a common function.
 *
 * @param <T> <code>DataOutput</code> or <code>DescriptionOutput</code>.
 */
@FunctionalInterface
interface DescriptionWriter<T extends DataOutput> {

    /**
     * <code>ClassKnowledge#describeFieldInto</code>, expressed as a
     * <code>descriptionWriter</code>.
     */
    static final DescriptionWriter<DescriptionOutput> DESCRIBE_FIELD_INTO
            = ClassKnowledge::describeFieldInto;
    /**
     * <code>ClassKnowledge#writeFieldDescriptionTo</code>, expressed as a
     * <code>descriptionWriter</code>.
     */
    static final DescriptionWriter<DataOutput> WRITE_FIELD_DESCRIPTION_TO
            = ClassKnowledge::writeFieldDescriptionTo;

    /**
     * Describes the given object to the given destination, using a function
     * specified by this object.
     *
     * @param knowledge The knowledge for <code>fieldValue</code>'s actual
     * class.
     * @param to The place to store the description.
     * @param fieldValue The object to describe.
     * @param nullable Whether to use a format that's capable of storing
     * <code>null</code>.
     * @throws IOException If something goes wrong writing the description
     */
    void describeTo(ClassKnowledge<?> knowledge, T to,
            Object fieldValue, boolean nullable) throws IOException;
}
