package xyz.acygn.mokapot.util;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for operating on immutable sets.
 *
 * @author Alex Smith
 */
public class ImmutableSets {

    /**
     * Returns a set equivalent to a given set, but with one element added. This
     * will just return the original set if the element in question is already
     * there. <code>null</code> can be used as a substitute for the null set.
     * <p>
     * This method is not safe against concurrent modification of the given set.
     * (The intended use is where the original set is immutable and thus cannot
     * be modified at all, but the only requirement is that the original set is
     * not modified while this method is running.)
     *
     * @param <T> The type of elements stored in the set.
     * @param original The original set. May be <code>null</code>, which will be
     * interpreted as an empty set.
     * @param element The element to add.
     * @return An immutable set equivalent to <code>original</code>, except that
     * it contains <code>element</code>.
     */
    public static <T> Set<T> plusElement(Set<T> original, T element) {
        if (original == null) {
            return singleton(element);
        }
        if (original.contains(element)) {
            return original;
        }
        Set<T> rv = new HashSet<>(original);
        rv.add(element);
        return unmodifiableSet(rv);
    }

    /**
     * Returns a set equivalent to a given set, but with one element removed.
     * This will just return the original set if the element in question is
     * already missing. Note that this returns <code>null</code> if the output
     * set would be empty (even if the input set is also empty).
     * <p>
     * This method is not safe against concurrent modification of the given set.
     * (The intended use is where the original set is immutable and thus cannot
     * be modified at all, but the only requirement is that the original set is
     * not modified while this method is running.)
     *
     * @param <T> The type of elements stored in the set.
     * @param original The original set. May be <code>null</code>, which will be
     * interpreted as an empty set (and just cause a <code>null</code> return).
     * @param element The element to remove.
     * @return An immutable set equivalent to <code>original</code>, except that
     * it doesn't contains <code>element</code>. If this would cause an empty
     * return value, this method instead returns <code>null</code>.
     */
    public static <T> Set<T> minusElement(Set<T> original, T element) {
        if (original == null || original.isEmpty()) {
            return null;
        }
        if (!original.contains(element)) {
            return original;
        }
        if (original.size() == 1) {
            return null;
        }
        Set<T> rv = new HashSet<>(original);
        rv.remove(element);
        return unmodifiableSet(rv);
    }

    /**
     * Inaccessible constructor. This class contains only private methods and is
     * not intended to be instantiated.
     */
    private ImmutableSets() {
    }
}
