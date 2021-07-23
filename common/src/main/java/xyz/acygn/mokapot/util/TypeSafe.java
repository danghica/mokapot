package xyz.acygn.mokapot.util;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class containing type-safe re-implementations of some of Java's
 * non-type-safe reflection APIs.
 * <p>
 * Java's <code>Object#getClass()</code> always returns a
 * <code>Class&lt;?&gt;</code>, thus destroying information about what class the
 * object actually had, and typically needing an unchecked cast or run-time
 * check to allow the program to type-check correctly. Likewise, array creation
 * via reflection does not return an object of the array type, leading to
 * similar problems.
 * <p>
 * This class contains wrappers for these operations that centralise the
 * unchecked casts in a single location. The wrappers use a type-safe interface
 * (i.e. for most of the wrappers, it is impossible to call the method in a way
 * that will cause a <code>ClassCastException</code> unless at least one of the
 * parameters has an actual type that contradicts its declared type;
 * <code>valueCast</code> is an exception), meaning that type mistakes in the
 * code are much easier to detect (as they will be caught by the compiler).
 *
 * @author Alex Smith
 */
public class TypeSafe {

    /**
     * Returns the actual class of an object that's stored via a reference with
     * a given declared class. This is type-safe, in the sense that the actual
     * class is always known to extend the declared class (or in the case where
     * the declared class is actually an interface, implement it).
     *
     * @param <T> The declared class of the object.
     * @param object The object itself.
     * @return The actual class of the object.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getActualClass(T object) {
        return (Class<? extends T>) object.getClass();
    }

    /**
     * Returns a list consisting of the interfaces directly implemented by the
     * given class, plus its direct superclass. Unlike
     * <code>Class#getSuperClass()</code> and <code>Class#getInterfaces</code>,
     * this returns a value of the most precise possible type. In the case where
     * the argument is an interface, no superclass is returned (just the parent
     * interfaces); in this situation, it's possible for the return value to be
     * an empty array.
     *
     * @param <T> The class to analyse.
     * @param c <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @return An immutable list of interfaces and the superclass.
     * @throws IllegalArgumentException If <code>c</code> is
     * <code>Object.class</code> or a primitive (which don't have superclasses)
     */
    public static <T> List<Class<? super T>>
            getInterfacesAndSuperclass(Class<T> c)
            throws IllegalArgumentException {
        if (c.equals(Object.class) || c.isPrimitive()) {
            throw new IllegalArgumentException("Object has no superclass");
        }
        @SuppressWarnings("unchecked")
        List<Class<? super T>> rv = Arrays.asList(
                (Class<? super T>[]) c.getInterfaces());

        if (c.isInterface()) {
            return rv;
        }
        Class<? super T> superClass = c.getSuperclass();
        return new ExtendedList<>(rv, superClass);
    }

    /**
     * Attempts to cast a <code>Class</code> object, such that it represents
     * only subtypes of a given type. If doing so is impossible, returns a given
     * default value instead.
     *
     * @param <T> The type which the class should represent a subtype of, for
     * the cast to succeed.
     * @param classObject The object to be cast.
     * @param castTo <code>T.class</code>, given explicitly due to Java's type
     * erasure rules
     * @param defaultReturn The value to return if the cast fails.
     * @return <code>(Class&lt;? extends T&gt;)classObject</code>, or
     * <code>defaultReturn</code> if <code>classObject</code> does not have the
     * type <code>Class&lt;? extends T&gt;</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> classCast(
            Class<?> classObject, Class<T> castTo,
            Class<? extends T> defaultReturn) {
        if (castTo.isAssignableFrom(classObject)) {
            return (Class<? extends T>) classObject;
        } else {
            return defaultReturn;
        }
    }

    /**
     * Cast a value from one class to another. This is basically
     * <code>Class#cast</code>, but correctly handles the case where
     * <code>toClass</code> represents a primitive.
     *
     * @param <T> The class to casts the value into.
     * @param value The value to cast.
     * @param toClass <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @return <code>(T) value</code>
     * @throws ClassCastException If <code>value</code> cannot be stored in a
     * <code>T</code> (note that primitives can be stored in the corresponding
     * wrapper type and wrappers can be stored in the corresponding primitive
     * type)
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueCast(Object value, Class<T> toClass)
            throws ClassCastException {
        if (toClass.equals(boolean.class)) {
            return (T) Boolean.class.cast(value);
        } else if (toClass.equals(byte.class)) {
            return (T) Byte.class.cast(value);
        } else if (toClass.equals(char.class)) {
            return (T) Character.class.cast(value);
        } else if (toClass.equals(double.class)) {
            return (T) Double.class.cast(value);
        } else if (toClass.equals(float.class)) {
            return (T) Float.class.cast(value);
        } else if (toClass.equals(int.class)) {
            return (T) Integer.class.cast(value);
        } else if (toClass.equals(long.class)) {
            return (T) Long.class.cast(value);
        } else {
            return toClass.cast(value);
        }
    }

    /**
     * Inaccessible constructor. As a utility class, this class is not meant to
     * be instantiated.
     */
    private TypeSafe() {
    }
}
