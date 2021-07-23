package xyz.acygn.mokapot.util;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Static methods for operations on objects. This mostly exists to cut down on
 * repetitive code that would otherwise have to be repeated across many
 * different object implementations.
 *
 * @author Alex Smith
 */
public class ObjectUtils {

    /**
     * Adapter for <code>Object#equals</code>. This method implements all the
     * usual pre-checks required for an unknown object to be equal to a known
     * object, and does a more in-depth comparison only if they have the same
     * class.
     *
     * @param <T> The type of the known object.
     * @param known The known object, i.e. the object that's definitely not
     * <code>null</code> and has type <code>T</code>. This is probably the
     * <code>this</code> of the caller.
     * @param unknown The unknown object that the caller is being compared to.
     * This might be <code>null</code>, and might have any type.
     * @param comparer A function that compares two objects that are known to
     * not be <code>null</code> and to have type <code>T</code> for value
     * equality.
     * @return <code>true</code> if <code>known == unknown</code>;
     * <code>false</code> if <code>unknown == null</code>; <code>false</code> if
     * <code>unknown</code> and <code>known</code> belong to different classes;
     * <code>comparer(known, unknown)</code> otherwise.
     * @see Object#equals(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean equals(
            T known, Object unknown, BiPredicate<T, T> comparer) {
        if (known == unknown) {
            return true;
        }
        if (unknown == null || !unknown.getClass().equals(known.getClass())) {
            return false;
        }
        /* This is a checked cast - we checked it in the previous if statement -
           but Java doesn't recognise that particular shape of test as a class
           test and thus believes we haven't checked it. */
        return comparer.test(known, (T) unknown);
    }

    /**
     * Adapter for <code>Object#equals</code>. This method implements all the
     * usual pre-checks required for an unknown object to be equal to a known
     * object. If they have the same class, both the known and unknown objects
     * will be processed via a given function, then the return values of the
     * functions will be compared via <code>.equals()</code>.
     * <p>
     * This version of <code>equals</code> is most useful for comparing objects
     * that have only one field.
     *
     * @param <T> The type of the known object.
     * @param <U> The type of the values that the objects are being compared on.
     * @param known The known object, i.e. the object that's definitely not
     * <code>null</code> and has type <code>T</code>. This is probably the
     * <code>this</code> of the caller.
     * @param unknown The unknown object that the caller is being compared to.
     * This might be <code>null</code>, and might have any type.
     * @param extractor A function that extracts a value of type <code>U</code>
     * from values of type <code>T</code>, such that the values of type
     * <code>T</code> are equal if and only if the values of type <code>U</code>
     * are both equal or both null. (Note that there should not be "extraction
     * loops", i.e. attempting to compare objects via the extraction process
     * should not lead to an infinite regress.)
     * @return <code>true</code> if <code>known == unknown</code>;
     * <code>false</code> if <code>unknown == null</code>; <code>false</code> if
     * <code>unknown</code> and <code>known</code> belong to different classes;
     * otherwise <code>true</code> if the extractor gives the same result (as
     * determined via <code>Objects#equals</code>) on each object.
     * @see Object#equals(java.lang.Object)
     */
    public static <T, U> boolean equals(
            T known, Object unknown, Function<T, U> extractor) {
        return equals(known, unknown, (BiPredicate<T, T>) (t, u)
                -> Objects.equals(extractor.apply(t), extractor.apply(u)));
    }

    /**
     * Adapter for <code>Object#equals</code>. This method implements all the
     * usual pre-checks required for an unknown object to be equal to a known
     * object. If they have the same class, a series of tests will be run on the
     * objects, with the objects being considered equal if each of the tests
     * produces the same result.
     * <p>
     * This version of <code>equals</code> is most useful for comparing objects
     * that have multiple fields; typically, each test would extract a single
     * field from the object, and thus the objects would compare equal if all
     * their fields were equal.
     *
     * @param <T> The type of the known object.
     * @param known The known object, i.e. the object that's definitely not
     * <code>null</code> and has type <code>T</code>. This is probably the
     * <code>this</code> of the caller.
     * @param unknown The unknown object that the caller is being compared to.
     * This might be <code>null</code>, and might have any type.
     * @param extractors A list of functions that extract a value from values of
     * type <code>T</code>, such that the values of type <code>T</code> are
     * equal if and only if the extracted values are both equal or both null.
     * (Note that there should not be "extraction loops", i.e. attempting to
     * compare objects via the extraction process should not lead to an infinite
     * regress.)
     * @return <code>true</code> if <code>known == unknown</code>;
     * <code>false</code> if <code>unknown == null</code>; <code>false</code> if
     * <code>unknown</code> and <code>known</code> belong to different classes;
     * otherwise <code>true</code> if, for every extractor, it gives the same
     * result (as determined via <code>Objects#equals</code>) on each object.
     * @see Object#equals(java.lang.Object)
     */
    @SafeVarargs
    public static <T> boolean equals(
            T known, Object unknown, Function<T, Object>... extractors) {
        return equals(known, unknown, (BiPredicate<T, T>) (t, u)
                -> Stream.of(extractors).allMatch(
                        (x) -> Objects.equals(x.apply(t), x.apply(u))));
    }

    /**
     * Inaccessible constructor. This is a utility class not meant to be
     * instantiated.
     */
    private ObjectUtils() {
    }
}
