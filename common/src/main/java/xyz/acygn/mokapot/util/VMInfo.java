package xyz.acygn.mokapot.util;

/**
 * Utility class that deals with unique special cases for Java virtual machines.
 * This also includes centralising the differences between Java and Android.
 *
 * @author Alex Smith
 */
public class VMInfo {

    /**
     * Returns whether this class is actually running on Android, rather than
     * Java.
     *
     * @return Whether the system properties apply that the code is running on a
     * Dalvik virtual machine.
     */
    public static boolean isRunningOnAndroid() {
        return System.getProperty("java.vm.name").equals("Dalvik");
    }

    /**
     * Determines whether a class with the given name could not be injected into
     * the appropriate package. This occurs when the class's name indicates that
     * it belongs to a critical system package, like <code>java.lang</code>.
     * <p>
     * Note that this method works regardless of whether the class in question
     * exists, and thus can safely be used to determine whether it could be
     * useful to generate a class. It also works (returning <code>false</code>)
     * if the class belongs to a nonexistent package; in this situation, the
     * package could first be created as non-sealed, and the class subsequently
     * injected into it.
     *
     * @param className The class's name, in either Java format (e.g.
     * <code>java.lang.String</code>) or JVM format (e.g.
     * <code>java/lang/String</code>).
     * @return <code>true</code> if the package is sealed (i.e. the class cannot
     * be injected); <code>false</code> if the name does not prevent injecting
     * the class into its package.
     */
    public static boolean isClassNameInSealedPackage(String className) {
        return className.startsWith("java.")
                || className.startsWith("java/");
    }

    /**
     * Inaccessible constructor. This is a utility class and not intended to be
     * instantiated.
     */
    private VMInfo() {
    }
}
