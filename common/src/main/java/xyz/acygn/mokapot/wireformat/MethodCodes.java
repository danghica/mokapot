package xyz.acygn.mokapot.wireformat;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static xyz.acygn.mokapot.util.ObjectMethodDatabase.methodSignature;
import static xyz.acygn.mokapot.wireformat.ObjectDescription.IDENTIFIER_CHARSET;

/**
 * Utility routines for working with method codes. A method code is a 64-bit
 * integer that identifies a pair of {method, actual class}, such that all
 * methods callable on objects of a given actual class have distinct codes.
 * (It's possible for two different actual classes to have codes with the same
 * value, either with the same or different methods.) The assignment of method
 * codes to methods is part of this library's API, and thus needs to be
 * consistent for all participants within a single distributed communication.
 * Eventually, there's likely to be some method of specifying an assignment of
 * codes statically (so that the codes can stay stable in all possible
 * circumstances). However, in instances where method codes aren't statically
 * assigned, this class aims to produce numbers that are as consistent as
 * possible without clashing.
 * <p>
 * This is accomplished via hashing; the first 64 bits of a salted hash of a
 * string representation of the method are used, in order to reduce collisions
 * as far as possible (any collisions which do remain can be removed by varying
 * the salt). This means that, so long as the same salt is chosen on each
 * machine in the communication, the codes will be consistent between machines.
 * The class knowledge for the actual class in question will store a value for
 * the salt to be used with that class (calculated as the smallest positive
 * integer that does not cause a collision); the only way that different salts
 * could be chosen between different machines would be if a collision existed on
 * one machine but not another, which is vanishingly unlikely given the odds of
 * a collision in the 64-bit space and the relative similarity between the
 * machines' classpaths. Problems are still theoretically possible, but the
 * probability is very low.
 *
 * @author Alex Smith
 */
public class MethodCodes {

    /**
     * The (hardcoded) method code of <code>Object#finalize</code>.
     *
     * @see Object#finalize()
     */
    public static final long FINALIZE = 1;

    /**
     * The (hardcoded) method code of <code>Object#clone</code>.
     *
     * @see Object#clone()
     */
    public static final long CLONE = 2;

    /**
     * Method codes from 0 inclusive to this value exclusive should be avoided
     * (via changing to a different value when choosing a default, or via
     * choosing appropriate values if assigning handling manually). This allows
     * methods that need special handling to have dedicated codes, allowing them
     * to be easily recognised.
     */
    public static final long FIRST_NORMAL = 3;

    /**
     * Produces a "method signature" for a method, combined with an integer
     * salt. The result can be hashed in order to produce a hash that's always
     * the same for the same method and salt, but nearly always changes if the
     * salt or method signature is changed.
     *
     * @param m The method to calculate the signature of.
     * @param salt The salt to append to the signature.
     * @return The method's signature.
     */
    private static String saltedMethodSignature(Method m, int salt) {
        /* Special-case 1, as it's nearly always the salt in practice */
        if (salt == 1) {
            return methodSignature(m);
        } else {
            return methodSignature(m) + " " + salt;
        }
    }

    /**
     * Produces the "default" method code for the given method. This requires a
     * "salt" parameter which is stored in the class knowledge of the class
     * which forms the "actual class" part of the code, and of course the method
     * in question. The value returned is the value that would be chosen as the
     * method's code in the absence of other information.
     * <p>
     * In nearly all cases, the default code is chosen via hashing. A few
     * methods which need special handling (such as
     * <code>Object#finalize</code>) have well-known, dedicated codes of their
     * own.
     *
     * @param m The method to obtain the code of.
     * @param salt The salt to use in the default code generation algorithm.
     * @return The default method code.
     */
    public static long defaultMethodCode(Method m, int salt) {
        /* Special cases. */
        if (m.getName().equals("finalize") && m.getParameterCount() == 0) {
            return FINALIZE;
        }
        if (m.getName().equals("clone") && m.getParameterCount() == 0) {
            return CLONE;
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update((saltedMethodSignature(m, salt)).getBytes(IDENTIFIER_CHARSET));
        } catch (NoSuchAlgorithmException ex) {
            /* Because we're using a hardcoded algorithm, this shouldn't
               happen. */
            throw new RuntimeException(ex);
        }
        byte[] digest = md.digest();
        long rv = digest[0];
        rv <<= 8;
        rv += digest[1];
        rv <<= 8;
        rv += digest[2];
        rv <<= 8;
        rv += digest[3];
        rv <<= 8;
        rv += digest[4];
        rv <<= 8;
        rv += digest[5];
        rv <<= 8;
        rv += digest[6];
        rv <<= 8;
        rv += digest[7];

        if (rv >= 0 && rv < FIRST_NORMAL) {
            /* Pick a different value that's not in the abnormal range. */
            return -1 - rv;
        }

        return rv;
    }

    /**
     * Private constructor. This is a utility class not meant to be
     * instantiated.
     */
    private MethodCodes() {
    }

    /**
     * Main function. This allows this class to be used as a command line tool
     * to look up the method codes for all methods in a given class.
     *
     * @param args An array consisting of a single string, the name of the class
     * to look up.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(
                    "Usage: java xyz.acygn.mokapot.MethodCodes classname");
            System.exit(64);
        }

        Class<?> about;
        try {
            about = Class.forName(args[0]);
        } catch (ClassNotFoundException ex) {
            System.err.println(
                    "Error: cannot load " + args[0] + ": " + ex.getMessage());
            System.exit(66);
            /* Java's control flow analysis doesn't know that System.exit never
               returns, so we need a dead return; to give it the hint. Otherwise
               the code fails to compile because it looks to the compiler like
               about is not correctly initialised. */
            return;
        }

        ObjectWireFormat<?> db = new ObjectWireFormat<>(about);
        int salt = db.getMethodCodeSalt();
        db.getMethods().stream().forEach((m) -> {
            long code = defaultMethodCode(m, salt);
            System.out.println(m + " [" + saltedMethodSignature(m, salt)
                    + "]: 0x" + Long.toHexString(code) + "L (bottom 32 bits: 0x"
                    + Integer.toHexString((int) code) + ")");
        });
    }
}
