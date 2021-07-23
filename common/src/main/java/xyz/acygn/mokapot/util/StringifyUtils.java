package xyz.acygn.mokapot.util;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Utilities for converting arbitrary objects to strings in a better way than
 * <code>Object#toString</code>. Used to make debug messages more readable.
 *
 * @author Alex Smith
 */
public class StringifyUtils {

    /**
     * General purpose toString operator that produces prettier output for lists
     * and arrays. In most cases, this just uses the object's
     * <code>toString</code>, but collections are shown element-wise.
     *
     * @param o The object to stringify.
     * @return A string representation of the object.
     */
    public static String toString(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof Collection || o.getClass().isArray()) {
            Object oa = o;
            if (o instanceof Collection) {
                oa = ((Collection) o).toArray();
            }
            StringBuilder sb = new StringBuilder(
                    o.getClass().getSimpleName() + "([");
            sb.append(commaSeparateArray(oa));
            return sb.append("])").toString();
        } else {
            return o.toString();
        }
    }

    /**
     * Returns a string representation of an array with no delimiters. Each
     * element of the array is converted to a string via <code>toString</code>,
     * then the elements are separated using commas.
     *
     * @param array The array to separate. This must actually be an array,
     * although it's declared as <code>Object</code> to make it more usable in
     * <code>toString</code> implementations.
     * @return The comma-separated string representation of the array.
     * @see #toString(java.lang.Object)
     */
    public static String commaSeparateArray(Object array) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < Array.getLength(array); i++) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(toString(Array.get(array, i)));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Produces a human-readable string representation of the given object. This
     * is in most cases based on the object's <code>toString</code>, but some
     * particularly common classes have special cases. Unlike
     * <code>toString</code>, this aims to make the stringification look more
     * like it would appear in source code, i.e. visually distinct and with an
     * indication of the object's type.
     *
     * @param o The object to stringify.
     * @return A stringification of the object.
     */
    public static String stringify(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Double || o instanceof Float || o instanceof Long
                || o instanceof Integer || o instanceof Short) {
            return o.toString();
        }
        if (o instanceof String) {
            return "\"" + ((String) o).replaceAll("([\"\\\\])", "\\\\$1") + "\"";
        }
        if (o.getClass().isArray()) {
            return toString(o);
        }

        return o.getClass().getSimpleName() + "{" + o.toString() + "}";
    }

    /**
     * Inaccessible constructor. Used to prevent this utility class being
     * instantiated.
     */
    private StringifyUtils() {
    }
}
