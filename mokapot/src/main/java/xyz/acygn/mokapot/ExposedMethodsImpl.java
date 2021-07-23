package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import static java.lang.reflect.Modifier.isFinal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.DescriptionWriter.DESCRIBE_FIELD_INTO;
import static xyz.acygn.mokapot.DescriptionWriter.WRITE_FIELD_DESCRIPTION_TO;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Authorisation;
import xyz.acygn.mokapot.skeletons.ExposedMethods;
import xyz.acygn.mokapot.skeletons.Standin;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.SELFREF_DESCRIPTION;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Methods from the internals of the distributed system that are exposed so that
 * they can be called from generated code. This is the (singleton)
 * implementation of <code>ExposedMethods</code>.
 * <p>
 * This method is loaded into <code>ExposedMethods</code>'s singleton via the
 * static initialiser of <code>DistributedCommunicator</code>, in order to
 * ensure that it's always loaded before a standin is created.
 *
 * @author Alex Smith
 */
class ExposedMethodsImpl extends ExposedMethods {

    @Override
    public <T> void defaultStandinDescribeInto(Standin<T> standin,
            DescriptionOutput into, Authorisation auth) throws IOException {
        defaultStandinDescription(standin, DESCRIBE_FIELD_INTO, into, auth);
    }

    @Override
    public <T> void defaultStandinWriteTo(Standin<T> standin,
            DataOutput into, Authorisation auth) throws IOException {
        defaultStandinDescription(standin, WRITE_FIELD_DESCRIPTION_TO, into, auth);
    }

    /**
     * A default implementation of <code>Standin#replaceWithReproduction</code>.
     * See that method for more information.
     * <p>
     * This default implementation uses reflection to set the fields of the
     * referent individually from the description.
     *
     * @param <T> The type of the standin's referent.
     * @param standin The standin whose referent should be replaced by a
     * reproduction.
     * @param about The class of the standin's referent, i.e. the class of the
     * object that the description describes.
     * @param description The description to reproduce.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code>.
     * @throws IOException If the description appears to be corrupted
     */
    @Override
    public <T> void defaultStandinReplaceWithReproduction(Standin<T> standin,
            Class<T> about, ReadableDescription description, Authorisation auth)
            throws IOException, IllegalStateException, SecurityException {
        auth.verify();

        T referent = standin.getReferent(auth);

        if (referent == null) {
            throw new IllegalStateException(
                    "Attempt to replace a null (dropped?) referent");
        }

        ClassKnowledge<T> referentKnowledge = knowledgeForClass(about);

        for (Field f : referentKnowledge.getInstanceFieldList()) {
            Class<?> declaredType = f.getType();

            while (Standin.class.isAssignableFrom(declaredType)) {
                /* Using a standin class as a declared type is rare, but we
                   do it internally on at least one occasion. We only do this
                   on inheritance-based standins, so it's easy to determine the
                   referent class. (Note that we don't need to do this when
                   describing the field; in that case, the standin itself would
                   be doing the describing and knows what type it's describing
                   for.) */
                declaredType = declaredType.getSuperclass();
            }
            Object fValue;

            if (isFinal(declaredType.getModifiers())) {
                /* If the declared type is final, we know that the actual type
                   must be the same (or null). So we can just nullably describe
                   the field's contents. */
                ClassKnowledge<?> ck
                        = knowledgeForClass(declaredType);
                fValue = ck.reproduce(description, !declaredType.isPrimitive());
            } else {
                fValue = Marshalling.rCAOStatic(
                        description, referent, declaredType);
            }

            try {
                f.set(referent, fValue);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                String detailReason = "[" + fValue.getClass() + " extends "
                        + fValue.getClass().getSuperclass() + " implements "
                        + Arrays.toString(fValue.getClass().getInterfaces())
                        + "]";
                throw new IOException("Could not set field of type "
                        + f.getType() + " to value of type " + detailReason, ex);
            }
        }
    }

    /**
     * A default implementation of <code>Standin#describeInto</code> and
     * <code>Standin#writeTo</code>. Based on the <code>writer</code> parameter,
     * it can emulate the behaviour of either method.
     * <p>
     * This implementation works via reflection, iterating over the fields of
     * the standin's referent and writing them to the description accordingly.
     *
     * @param <T> The actual class of the standin's referent.
     * @param <U> The type of <code>into</code>.
     * @param standin The standin to describe.
     * @param writer The method via which the fields of the standin will be
     * recursively written into the description.
     * @param into The place to write the description.
     * @param auth The authorisation that allows this method to ensure it's
     * being run from inside the package; it will be verified to ensure that
     * it's valid.
     * @throws IOException If something goes wrong writing the description
     */
    public <T, U extends DataOutput> void defaultStandinDescription(
            Standin<T> standin, DescriptionWriter<U> writer,
            U into, Authorisation auth) throws IOException {
        auth.verify();
        ClassKnowledge<T> referentKnowledge = knowledgeForClass(
                standin.getReferentClass(null));
        T referent = standin.getReferent(auth);
        if (referentKnowledge instanceof SpecialCaseKnowledge
                || referentKnowledge instanceof LambdaKnowledge
                || referentKnowledge instanceof EnumKnowledge
                || referentKnowledge instanceof ArrayKnowledge) {
            /* These are a few Copiable / UnreliablyCopiable cases for which
               we wouldn't normally be using a reflective standin to describe
               the object. However, we need to describe it in the case where
               we're taking a location manager containing the object offline,
               and in that case, our normal field-wise format won't work. So
               just defer to describing a hypothetical field containing the
               object; these cases will all make a description containing the
               relevant information when this happens. */
            writer.describeTo(referentKnowledge, into, referent, false);
            return;
        }
        for (Field f : referentKnowledge.getInstanceFieldList()) {
            ObjectDescription.Size position = null;
            if (Marshalling.slowDebugOperationsEnabled()
                    && into instanceof DescriptionOutput) {
                position = ((DescriptionOutput) into).getPosition();
            }

            Class<?> declaredType = f.getType();
            Object fValue;
            try {
                fValue = f.get(referent);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                /* This shouldn't be able to happen; getInstanceFieldList() checks it */
                throw new IOException(ex);
            }
            if (isFinal(declaredType.getModifiers())) {
                /* If the declared type is final, we know that the actual type
                must be the same (or null). So we can just nullably describe
                the field's contents. */
                ClassKnowledge<?> ck = knowledgeForClass(declaredType);
                writer.describeTo(ck, into, fValue, !declaredType.isPrimitive());
            } else if (fValue == null) {
                into.write(NULL_DESCRIPTION);
            } else if (fValue == referent) {
                into.write(SELFREF_DESCRIPTION);
            } else {
                ClassKnowledge<?> ck = knowledgeForActualClass(fValue);
                into.write(ck.getClassNameDescription(declaredType));
                writer.describeTo(ck, into, fValue, false);
            }

            if (position != null) {
                ObjectDescription.Size actual = ((DescriptionOutput) into)
                        .getPosition().subtract(position);
                ObjectDescription.Size expected
                        = descriptionSizeOfField(f, referent);
                if (!actual.fitsWithin(expected)) {
                    throw new AssertionError("description size mismatch, expected "
                            + expected + ", actual " + actual);
                }
            }
        }
    }

    /**
     * Default method for calculating the size of the description of the object
     * represented by a standin. This is the method that
     * <code>Standin#descriptionSize</code> uses for its default implementation
     * (although it can be overridden). The size does not include any size
     * needed to describe the class of the object, nor any extra space needed to
     * give a representation of <code>null</code>.
     * <p>
     * This implementation uses <code>ClassKnowledge#descriptionSize</code> in
     * the cases where it works (lambdas and enums; technically also arrays,
     * classes, and strings, but you can't create standins for those). In cases
     * where it doesn't, the size is instead calculated field-by-field using
     * reflection.
     * <p>
     * A standin might override this method either because it's using some sort
     * of custom encoding (which will have a different size than expected), or
     * because it knows a more efficient way to calculate the size than a
     * field-by-field reflection.
     *
     * @param <T> The type of the standin's referent.
     * @param standin The standin whose description's size is being measured.
     * @param auth An authorisation, for namespacing and verification purposes.
     * @see Standin#descriptionSize(xyz.acygn.mokapot.skeletons.Authorisation)
     * @return The size of the description.
     */
    @Override
    public <T> ObjectDescription.Size defaultStandinDescriptionSize(
            Standin<T> standin, Authorisation auth) {
        auth.verify();

        ClassKnowledge<T> referentKnowledge
                = knowledgeForClass(
                        standin.getReferentClass(null));

        if (referentKnowledge instanceof SpecialCaseKnowledge
                || referentKnowledge instanceof LambdaKnowledge
                || referentKnowledge instanceof EnumKnowledge
                || referentKnowledge instanceof ArrayKnowledge) {
            /* See the corresponding special case in #describeInto. */
            return referentKnowledge.descriptionSize(
                    () -> standin.getReferent(auth), false);
        }

        T referent = standin.getReferent(auth);

        ObjectDescription.Size size = ObjectDescription.Size.ZERO;

        for (Field f : referentKnowledge.getInstanceFieldList()) {
            size = size.add(descriptionSizeOfField(f, referent));
        }
        return size;
    }

    /**
     * Returns the amount of space that a description will need to use to store
     * the value of a single field of an object. (Note that this is different
     * from a "describe for field" operation, although similar, as it needs to
     * record the actual class of the field's value if and only if it can't be
     * deduced from the declared class.)
     *
     * @param <T> The actual class of the object.
     * @param f The field.
     * @param referent The object that contains the field.
     * @return The amount of space that will be used to describe the field
     * (within a larger description that contains all the object's fields).
     */
    private static <T> ObjectDescription.Size descriptionSizeOfField(
            Field f, T referent) {
        ObjectDescription.Size size = ObjectDescription.Size.ZERO;
        Class<?> declaredType = f.getType();
        if (isFinal(declaredType.getModifiers())) {
            /* If the declared type is final, we know that the actual type
               must be the same (or null). So we just add the size of a
               description of a value of that type. */
            ClassKnowledge<?> knowledge
                    = knowledgeForClass(declaredType);
            size = size.add(knowledge.descriptionSize(() -> {
                try {
                    return f.get(referent);
                } catch (IllegalAccessException ex) {
                    /* This shouldn't happen because we already checked the
                       access in getInstanceFieldList(). */
                    throw new DistributedError(ex, "accessing field: " + f);
                }
            }, !declaredType.isPrimitive()));
        } else {
            Object fValue;
            try {
                fValue = f.get(referent);
            } catch (IllegalAccessException ex) {
                throw new DistributedError(ex, "accessing field: " + f);
            }
            if (fValue == null || fValue == referent) {
                size = size.addBytes(4);
            } else {
                ClassKnowledge<?> ck
                        = knowledgeForActualClass(fValue);
                size = size.add(
                        new ObjectDescription.Size(ck.getClassNameDescription(
                                declaredType).length, 0));
                size = size.add(ck.descriptionSize(() -> fValue, false));
            }
        }
        return size;
    }

    @Override
    @SuppressWarnings("Convert2Lambda")
    public Consumer<Object> getSettingConsumerFor(Object o, Authorisation auth) {
        ClassKnowledge<?> knowledge
                = knowledgeForActualClass(o);
        final Iterator<Field> iter = knowledge.getInstanceFieldList().iterator();

        return new Consumer<Object>() {
            @Override
            public void accept(Object newValue) {
                Field f = iter.next();
                try {
                    f.set(o, newValue);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new DistributedError(ex, "setting field: " + f);
                }
            }
        };
    }
}
