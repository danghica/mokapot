package xyz.acygn.mokapot.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * A general-purpose class for immutable values consisting of exactly two
 * fields.
 *
 * @author Alex Smith
 * @param <X> The type of the first field.
 * @param <Y> The type of the second field.
 */
public class Pair<X, Y> {

    /**
     * The first value stored in the pair.
     */
    private final X first;

    /**
     * The second value stored in the pair.
     */
    private final Y second;

    /**
     * Constructs a Pair by giving each field explicitly.
     *
     * @param first The value of the first field.
     * @param second The value of the second field.
     */
    public Pair(X first, Y second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Gets the first field of the Pair.
     *
     * @return The first field of the Pair.
     */
    public X getFirst() {
        return first;
    }

    /**
     * Gets the second field of the Pair.
     *
     * @return The second field of the Pair.
     */
    public Y getSecond() {
        return second;
    }

    /**
     * Produces a new pair whose second element is produced via running a
     * specified function. The first element is unchanged.
     *
     * @param <Z> The new type of the second element.
     * @param mapper The function that produces the new second element from the
     * old second element.
     * @return The new pair.
     */
    public <Z> Pair<X, Z> mapSecond(Function<Y, Z> mapper) {
        return new Pair<>(first, mapper.apply(second));
    }

    /**
     * Calculates a value such that two pairs with equal elements will have
     * equal hash codes.
     *
     * @return A hash code for this pair.
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.first);
        hash = 37 * hash + Objects.hashCode(this.second);
        return hash;
    }

    /**
     * Element-wise equality between this pair and another.
     *
     * @param obj The object to compare to.
     * @return <code>false</code> if <code>obj</code> is not a pair; otherwise,
     * whether this pair and <code>obj</code> have equal corresponding elements.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        return (Objects.equals(this.first, other.first)
                && Objects.equals(this.second, other.second));
    }

    /**
     * Produces a string representation of the pair.
     *
     * @return A string representation of the pair: this contains the two values
     * separated by a comma, inside parentheses.
     */
    @Override
    public String toString() {
        return "(" + first + ", " + second + ')';
    }

    /**
     * Produces a function that maps each input <code>x</code> to an output Pair
     * <code>(x, f(x))</code>. The function <code>f</code> is specified as
     * input. The typical use of this would be to transform a value in the
     * middle of a stream without losing the original value.
     *
     * @param <X> The type of the input to the resulting function.
     * @param <Y> The type the input is transformed to.
     * @param f The function that transforms the input.
     * @return A function that transforms the input, and returns a pair of the
     * original and transformed input.
     */
    public static <X, Y> Function<X, Pair<X, Y>> preservingMap(
            Function<X, Y> f) {
        return (x) -> new Pair<>(x, f.apply(x));
    }
}
