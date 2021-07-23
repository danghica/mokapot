package xyz.acygn.mokapot.wireformat;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import static xyz.acygn.mokapot.wireformat.ObjectDescription.IDENTIFIER_CHARSET;

/**
 * Static methods for handling class name descriptions. A class name description
 * is used to uniquely identify a class in serialised output. It consists of a
 * number (encoded as a sequence of four bytes), possibly plus a sequence of
 * arbitrary bytes; if the number is positive, it refers to the number of bytes,
 * and if the number is negative, there are no additional bytes.
 * <p>
 * Note that this class is "pure" in the sense that the output of the functions
 * is recalculated each time. Caching the output of <code>describe</code> is
 * recommended.
 *
 * @author Alex Smith
 */
public class ClassNameDescriptions {

    /**
     * The description of the "class" that <code>null</code> belongs to. In
     * serialised output, it's common to write the class of the object, followed
     * by its fields. This can be used to replace the class of an object in the
     * case where the object turns out to be <code>null</code>.
     */
    public static final byte[] NULL_DESCRIPTION
            = intBytewise(-0x0A000000);
    /**
     * The integer used to generate <code>NULL_DESCRIPTION</code>.
     */
    public static final int NULL_DESCRIPTION_INT = -0x0A000000;

    /**
     * A description indicating that the class the object belongs to was
     * generated at runtime. (In other words, the class does not have a fixed
     * name on disk.) Objects with a class like this can't be serialised in
     * terms of their fields, as there's no way to construct an object of a
     * nonexistent/unknown class, and thus must instead be serialised as the
     * parameters to a factory method.
     */
    public static final byte[] SYNTHETIC_DESCRIPTION
            = intBytewise(-0x0A000001);

    /**
     * A special case description indicating a reference from an object to an
     * object that references it. Used to serialise cycles of size 2 in
     * serialised output in some special cases.
     * <p>
     * This "class name" exists in order to resolve an otherwise problematic
     * case involving <code>Throwable</code> (which is used heavily in error
     * handling code and thus might need to be serialised even when much of the
     * existing code is not working), reducing the number of dependencies
     * required to serialise and deserialise such an object correctly.
     */
    public static final byte[] SELFREF_DESCRIPTION
            = intBytewise(-0x0A000002);

    /**
     * A description indicating the class that the caller believes is "most
     * probable" in context. This would typically be the declared class of the
     * field into which the object described by the class will be deserialised.
     * Its purpose is to avoid the need to parse the name of the class in the
     * common case, whilst still allowing for other actual classes that can fit
     * in the field. (If only one actual class is possible, the class name
     * probably wouldn't need to be described at all.)
     */
    public static final byte[] EXPECTED_DESCRIPTION
            = intBytewise(-0x0A000003);

    /**
     * A map of classes that have special (terse) name descriptions consisting
     * of a single negative integer, to those integers.
     */
    private static final Map<Class<?>, Integer> SPECIAL_DESCRIPTIONS;
    /**
     * The reversed of SPECIAL_DESCRIPTIONS: a map that converts no-added-data
     * descriptions back to the original class.
     */
    private static final Map<Integer, Class<?>> SPECIAL_DESCRIPTIONS_REVERSE;

    /**
     * Adds a new special case for producing class name descriptions. In order
     * to prevent a class name being inconsistently described, this must only be
     * called if the class in question has never been described. Typically, a
     * class would add a special-case description from its own static
     * initialiser.
     *
     * @param description The desired special-case description. To avoid clashes
     * with other descriptions, this must be less than -0x0D000000.
     * @param described The class that is described by that description.
     * @throws IllegalArgumentException If the description in question is
     * insufficiently negative or already in use
     */
    public static void addNewSpecialCaseDescription(
            int description, Class<?> described)
            throws IllegalArgumentException {
        if (description >= -0x0D000000) {
            throw new IllegalArgumentException(
                    "description " + description
                    + " is too large and thus cannot be used for "
                    + described);
        }
        if (SPECIAL_DESCRIPTIONS_REVERSE.containsKey(description)) {
            throw new IllegalArgumentException(
                    "description " + description + " is already in use for "
                    + SPECIAL_DESCRIPTIONS_REVERSE.get(description)
                    + " and cannot be used for " + described);
        }
        internalAddNewSpecialCaseDescription(description, described);
    }

    /**
     * Adds a new special case for producing class name descriptions. In order
     * to prevent a class name being inconsistently described, this must only be
     * called if the class in question has never been described. Typically, a
     * class would add a special-case description from its own static
     * initialiser.
     * <p>
     * This internal version of the function allows out-of-range descriptions,
     * thus allowing it to be used to set up descriptions for things like
     * primitives. It also allows two classes to be given the same description
     * (e.g. <code>Boolean</code> and <code>bool</code>).
     *
     * @param description The desired special-case description.
     * @param described The class that is described by that description.
     */
    private static void internalAddNewSpecialCaseDescription(
            int description, Class<?> described) {
        SPECIAL_DESCRIPTIONS.put(described, description);
        SPECIAL_DESCRIPTIONS_REVERSE.put(description, described);
    }

    static {
        SPECIAL_DESCRIPTIONS = new HashMap<>();
        SPECIAL_DESCRIPTIONS_REVERSE = new HashMap<>();

        BiConsumer<Integer, Class<?>> r
                = ClassNameDescriptions::internalAddNewSpecialCaseDescription;

        /* Primitives each need a special description. */
        r.accept(-0x0B000001, Boolean.class);
        r.accept(-0x0B000002, Byte.class);
        r.accept(-0x0B000003, Character.class);
        r.accept(-0x0B000004, Double.class);
        r.accept(-0x0B000005, Float.class);
        r.accept(-0x0B000006, Integer.class);
        r.accept(-0x0B000007, Long.class);

        r.accept(-0x0B000001, boolean.class);
        r.accept(-0x0B000002, byte.class);
        r.accept(-0x0B000003, char.class);
        r.accept(-0x0B000004, double.class);
        r.accept(-0x0B000005, float.class);
        r.accept(-0x0B000006, int.class);
        r.accept(-0x0B000007, long.class);

        /* Some very commonly used classes also benefit from descriptions.
           Here are some standard API classes which are widely used either in
           general, or by this library in particular. */
        r.accept(-0x0C000001, String.class);
        r.accept(-0x0C000002, Class.class);
        r.accept(-0x0C000003, Inet4Address.class);
        r.accept(-0x0C000004, Inet6Address.class);
        r.accept(-0x0C000005, SerializedLambda.class);
        r.accept(-0x0C000006, BigInteger.class);
    }

    /**
     * Produces a description of the name of a class. Each class has a different
     * name description, and the process can be reversed to convert a class name
     * description into the name of the corresponding class.
     * <p>
     * This method recalculates the description from scratch each time. If you
     * want to call this repeatedly on potentially the same class, it's a good
     * idea to cache the results.
     *
     * @param c The class whose name should be described. This can be any type
     * of class, including an array or unboxed primitive class.
     * @return A description of that class's name. This will be a fresh byte
     * array for each call to this method.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public static byte[] describe(Class<?> c) {
        if (SPECIAL_DESCRIPTIONS.containsKey(c)) {
            return intBytewise(SPECIAL_DESCRIPTIONS.get(c));
        }

        /* Calculate a binary name for the class. Array classes don't have
           binary names, so we precede the name by a [ for each level of
           nesting. */
        Class<?> aboutComponent = c;
        StringBuilder binaryName = new StringBuilder();
        while (aboutComponent.isArray()) {
            aboutComponent = aboutComponent.getComponentType();
            binaryName.append('[');
        }
        if (aboutComponent.isPrimitive()) {
            binaryName.append(-SPECIAL_DESCRIPTIONS.get(aboutComponent));
        } else {
            binaryName.append(aboutComponent.getName());
        }

        byte[] descriptionTail = binaryName.toString().getBytes(IDENTIFIER_CHARSET);
        byte[] rv = new byte[descriptionTail.length + 4];
        ByteBuffer description = ByteBuffer.wrap(rv);
        description.putInt(descriptionTail.length);
        description.put(descriptionTail);
        return rv;
    }

    /**
     * Translates a negative integer into the corresponding class. Note that
     * this method cannot be used on descriptions that do not refer to a
     * particular class (e.g. "null", "the expected class", "a synthetic class")
     * because those don't have a corresponding <code>Class</code> object; the
     * caller will need to recognise those special cases itself.
     *
     * @param description The integer to translate.
     * @return The class referred to by that integer.
     * @throws IllegalArgumentException If the integer is not recognised as
     * referring to any particular class
     */
    public static Class<?> specialCasedClass(int description)
            throws IllegalArgumentException {
        Class<?> c = SPECIAL_DESCRIPTIONS_REVERSE.get(description);
        if (c == null) {
            throw new IllegalArgumentException(description
                    + " is not a number that identifies a class");
        }
        return c;
    }

    /**
     * Convert an integer to its representation in an ObjectDescription.
     *
     * @param i The integer to convert.
     * @return A 4-byte array containing the bytes of that integer. This is a
     * fresh array for each call to this method.
     */
    private static byte[] intBytewise(Integer i) {
        byte[] rv = new byte[4];
        ByteBuffer b = ByteBuffer.wrap(rv);
        b.putInt(i);
        return rv;
    }

    /**
     * A cache of the return value of <code>Class.forName</code>. This is used
     * to avoid the time spent in doing a class lookup every time we receive a
     * reference to a class we hadn't previously seen.
     */
    private static final Map<String, Class<?>> FOR_NAME_CACHE
            = Collections.synchronizedMap(new LinkedHashMap<String, Class<?>>() {
                private static final long serialVersionUID = 0x94b44be218d82f0fL;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Class<?>> eldest) {
                    return this.size() > 400;
                }
            });

    /**
     * Converts the trailing portion of a class name description back into a
     * class.
     *
     * @param b A byte buffer holding the trailing portion of the description.
     * @return The class matching that description.
     * @throws ClassNotFoundException If the class name encoded in the portion
     * in question does not appear to be a real class
     */
    public static Class<?> fromByteSlice(ByteBuffer b)
            throws ClassNotFoundException {
        String className = IDENTIFIER_CHARSET
                .decode(b).toString();

        int arrayLevels = 0;
        while (className.startsWith("[")) {
            arrayLevels++;
            className = className.substring(1);
        }

        Class<?> rv = null;

        if (arrayLevels > 0) {
            /* We only get an int as a class name for arrays of primitives. If
               we aren't dealing with an array, don't check it; according to
               a profiler, it's surprisingly time-consuming. */
            try {
                rv = specialCasedClass(-Integer.parseInt(className));
            } catch (NumberFormatException ex) {
                rv = null;
            }
        }
        if (rv == null) {
            /* The trouble needed to propagate one checked exception... */
            rv = FOR_NAME_CACHE.computeIfAbsent(className, (c) -> {
                try {
                    return Class.forName(c);
                } catch (ClassNotFoundException ex) {
                    return null;
                }
            });
            if (rv == null) {
                throw new ClassNotFoundException("Could not load class " + className);
            }
        }

        while (arrayLevels > 0) {
            /* Ugh, this surely can't be the easiest way to get at an array
               class with a specific component type? */
            rv = Array.newInstance(rv, 0).getClass();
            arrayLevels--;
        }

        return rv;
    }

    /**
     * Private constructor. This is a utility class not meant to be
     * instantiated.
     */
    private ClassNameDescriptions() {
    }
}
