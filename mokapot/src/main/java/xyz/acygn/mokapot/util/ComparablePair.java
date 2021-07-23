package xyz.acygn.mokapot.util;

import java.util.function.Function;

/**
 * A class implementing generic objects consisting of exactly two fields, with a
 * lexicographical comparison order. In other words, a pair is smaller than
 * another if its first element is smaller, or if the first element is equal and
 * the second element is smaller.
 *
 * @author Alex Smith
 * @param <X> The type of the first field.
 * @param <Y> The type of the second field.
 */
public class ComparablePair<X extends Comparable<X>, Y extends Comparable<Y>>
        extends Pair<X, Y> implements Comparable<ComparablePair<X, Y>> {

    /**
     * Creates a new comparable pair object.
     *
     * @param first The value of the first field.
     * @param second The value of the second field.
     */
    public ComparablePair(X first, Y second) {
        super(first, second);
    }

    /**
     * Compares this pair to another. If the first elements of the pairs differ,
     * that's considered to decide the comparison. If the first elements are
     * equal, the second element is used as a tie-break.
     *
     * @param o The pair to compare to.
     * @return negative if this pair is less; positive if this pair is greater;
     * zero if the two pairs are equal.
     */
    @Override
    public int compareTo(ComparablePair<X, Y> o) {
        int rv = getFirst().compareTo(o.getFirst());
        if (rv != 0) {
            return rv;
        }
        return getSecond().compareTo(o.getSecond());
    }

    /**
     * Produces a new pair whose second element is produced via running a
     * specified function. The first element is unchanged.
     * <p>
     * Unlike <code>mapSecond</code>, which can produce pairs whose second
     * element is of an incomparable type (and thus are incomparable), this
     * method always produces comparable pairs and thus requires a comparable
     * return type for the mapper.
     *
     * @param <Z> The new type of the second element. Must be comparable with
     * itself.
     * @param mapper The function that produces the new second element from the
     * old second element.
     * @return The new comparable pair.
     */
    public <Z extends Comparable<Z>> ComparablePair<X, Z> comparableMapSecond(Function<Y, Z> mapper) {
        return new ComparablePair<>(getFirst(), mapper.apply(getSecond()));
    }
}
