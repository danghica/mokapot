package xyz.acygn.mokapot.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.lang.reflect.Modifier.isTransient;

import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.Arrays;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableSortedSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static xyz.acygn.mokapot.util.TypeSafe.getInterfacesAndSuperclass;
import static xyz.acygn.mokapot.util.VMInfo.isClassNameInSealedPackage;

/**
 * A listing of all the methods (and fields, and classes) that exist on objects
 * which have a given actual class. This is useful both for static analysis, and
 * at runtime.
 * <p>
 * Note that this checks for fields and methods on <i>objects</i>, i.e.
 * <code>static</code> methods of a class are not included. Additionally,
 * <code>transient</code> fields are not included; this seems to give the best
 * results in practice, as such fields cannot contribute towards the state of
 * the object if used in the intended way.
 * <p>
 * The database also contains other miscellaneous information about the class.
 *
 * @author Alex Smith
 * @param <T> The actual class of objects described by the database.
 */
public class ObjectMethodDatabase<T> {

    /**
     * Produces a "method signature" for a method. This contains the method's
     * name and argument types, but typically not its declaring class or return
     * type. As an exception, if the method is static, the signature includes
     * its declaring class too (because unlike in the case of an instance
     * method, static methods do not shadow each other). Two methods with the
     * same signature are considered to be identical by this class (as if both
     * exist on the same object, they will do the same thing when called
     * virtually).
     * <p>
     * Note that this is not the same concept as a Java method signature or
     * method descriptor, which does not include the method's name.
     *
     * @param m The method to calculate the signature of.
     * @return The method's signature.
     */
    public static String methodSignature(Method m) {
        StringBuilder s = new StringBuilder(m.getName());
        if ((m.getModifiers() & STATIC) != 0) {
            s.append(':');
            s.append(m.getDeclaringClass().getName());
        }
        for (Class<?> paramType : m.getParameterTypes()) {
            s.append(',');
            s.append(paramType.getName());
        }
        return s.toString();
    }

    /**
     * The actual class of the objects that this ObjectMethodDatabase describes.
     * This is so that it can be given explicitly to methods that would
     * otherwise fail due to type erasure.
     */
    private final Class<T> about;

    /**
     * The set of classes and interfaces that the described objects are
     * assignable to. This includes <code>about</code> itself, all its
     * superclasses, and all its interfaces (including superinterfaces, even
     * indirectly). This is a commonly needed piece of information when doing
     * analysis on classes, and needs to be calculated anyway, so it's cached
     * here.
     *
     * You can think of this as the set of all possible
     * <code>Class&lt;? super T&gt;</code> objects.
     */
    private final Set<Class<? super T>> superclasses;

    /**
     * The set of all non-<code>transient</code> fields possessed by objects
     * described by this database (including fields not directly accessible from
     * this package). Note that "field possessed by an object" does not include
     * static fields, even if they belong to the right class.
     * <p>
     * The fields are sorted into alphabetical order, in order to give a
     * canonical order in which to represent the fields in a serialised data
     * structure.
     */
    private final SortedSet<Field> fields;

    /**
     * The set of all methods possessed by the class this knowledge is about
     * (including methods not directly accessible by this package), with any
     * security checks required to invoke them already completed. As with the
     * <code>fields</code> field, this makes static analysis faster, reflection
     * faster, and allows security restrictions to be tightened after its
     * construction.
     */
    private final Set<Method> methods;

    /**
     * Whether this class has non-private methods that aren't declared in a
     * superclass or interface. In other words, whether there's anything that
     * you could call on an object of this class if its declared type is its
     * actual type, but couldn't call with any other declared type.
     */
    private final boolean hasOwnMethods;

    /**
     * Whether this class has non-private methods that aren't declared in an
     * interface. This is a stronger version of <code>hasOwnMethods</code>. Note
     * that <code>Object</code> is considered an interface for this purpose (as
     * all objects, even those stored in a variable whose declared type is an
     * interface, have the methods of <code>Object</code>).
     */
    private final boolean hasNonInterfaceMethods;

    /**
     * The set of all interfaces directly or indirectly implemented by the class
     * this knowledge is about.
     */
    private final Set<Class<?>> interfaces;

    /**
     * The closest extensible class to the class this knowledge is about.
     */
    private final Class<? super T> closestExtensibleSuperclass;

    /**
     * Create a database of methods, fields and superclasses/interfaces for
     * objects with the given actual class.
     *
     * @param about The actual class that the resulting database should be
     * about.
     */
    public ObjectMethodDatabase(Class<T> about) {
        this.about = about;

        /* Find the set of superclasses (including implemented interfaces).
           Also the sets of methods and fields belonging to the class, including
           indirectly. */
        superclasses = new HashSet<>();
        fields = new TreeSet<>((f1, f2) -> f1.getName().compareTo(f2.getName()));
        HashSet<Method> allMethods = new HashSet<>();
        Deque<Class<? super T>> uncheckedClasses = new ArrayDeque<>();
        uncheckedClasses.add(about);
        while (!uncheckedClasses.isEmpty()) {
            Class<? super T> classToCheck = uncheckedClasses.removeLast();
            if (superclasses.contains(classToCheck)
                    || classToCheck.isPrimitive()) {
                continue;
            }
            try {
                fields.addAll(Arrays.asList(classToCheck.getDeclaredFields()));
            }
            catch(Throwable t){
                System.err.println((Arrays.toString(((URLClassLoader) this.getClass().getClassLoader()).getURLs())));
                throw t;
            }
            allMethods.addAll(Arrays.asList(classToCheck.getDeclaredMethods()));

            if (!classToCheck.equals(Object.class)) {
                uncheckedClasses.addAll(getInterfacesAndSuperclass(classToCheck));
            }
            superclasses.add(classToCheck);
        }

        fields.removeIf((field) -> isStatic(field.getModifiers()));
        fields.removeIf((field) -> isTransient(field.getModifiers()));

        /* Remove shadowed methods from the set of methods. */
        methods = new HashSet<>();
        Map<String, List<Method>> shadowGroups
                = allMethods.parallelStream().collect(
                        Collectors.groupingByConcurrent(
                                ObjectMethodDatabase::methodSignature));
        shadowGroups.forEach((signature, shadowGroup) -> {
            final boolean[] falseTrue = {false, true};
            /* First try to find a concrete or default implementing method. If
               there isn't one, ignore default methods (given that concrete
               methods beat them when there'd otherwise be a tie) and try
               again. */
            FOUND_SHADOWING_METHOD:
            for (boolean concreteOnly : falseTrue) {
                NEXT_SHADOWING_METHOD:
                for (Method shadowingMethod : shadowGroup) {
                    if (isAbstract(shadowingMethod.getModifiers())
                            || (concreteOnly && shadowingMethod.isDefault())) {
                        continue;
                    }

                    /* We're looking for the method that shadows all other
                       methods, i.e. its declaring class extends or implements
                       the declaring classes of the rest of the group. */
                    for (Method shadowedMethod : shadowGroup) {
                        if (isAbstract(shadowedMethod.getModifiers())
                                || (concreteOnly && shadowedMethod.isDefault())) {
                            continue;
                        }
                        if (!shadowedMethod.getDeclaringClass().isAssignableFrom(
                                shadowingMethod.getDeclaringClass())) {
                            continue NEXT_SHADOWING_METHOD;
                        }
                    }
                    /* We found it. Add it to the set of methods. */
                    methods.add(shadowingMethod);
                    break FOUND_SHADOWING_METHOD;
                }
            }
            /* It's possible we don't find such a method; this can occur when
               the class we're analysing is abstract and the same method exists
               as an interface method via two separate inheritance hierarchies
               (e.g. via AbstractCollection.size and List.size when analysing
               AbstractList). */
        });

        /* Search the shadow groups for methods that are "new" (i.e. not
           declared in a superclass or interface). If we find them, we need to
           update hasOwnMethods and hasNonInterfaceMethods accordingly. */
        AtomicBoolean ownMethods = new AtomicBoolean(false);
        AtomicBoolean nonInterfaceMethods = new AtomicBoolean(false);
        shadowGroups.forEach((signature, shadowGroup) -> {
            boolean superclassDeclarationFound = false;
            boolean interfaceDeclarationFound = false;
            boolean nonPrivateMethodFound = false;
            for (Method candidateDeclaration : shadowGroup) {
                if (!isPrivate(candidateDeclaration.getModifiers())) {
                    nonPrivateMethodFound = true;
                }
                Class<?> declaringClass
                        = candidateDeclaration.getDeclaringClass();
                if (declaringClass.isInterface()
                        || Object.class.equals(declaringClass)) {
                    interfaceDeclarationFound = true;
                }
                if (!declaringClass.equals(about)) {
                    superclassDeclarationFound = true;
                }
            }
            if (nonPrivateMethodFound) {
                if (!interfaceDeclarationFound) {
                    nonInterfaceMethods.set(true);
                    if (!superclassDeclarationFound) {
                        ownMethods.set(true);
                    }
                }
            }
        });
        hasOwnMethods = ownMethods.get();
        hasNonInterfaceMethods = nonInterfaceMethods.get();

        interfaces = superclasses.stream().filter(Class::isInterface)
                .collect(Collectors.toSet());
        Class<? super T> currentSuperclass = about;
        while (!isClassExtensible(currentSuperclass)) {
            currentSuperclass = currentSuperclass.getSuperclass();
        }
        closestExtensibleSuperclass = currentSuperclass;
    }

    /**
     * Perform access checks on all the fields and methods stored in this
     * database. If the checks succeed, then the field and method objects
     * returned from <code>getInstanceFieldList</code> and
     * <code>getMethods</code> will be usable from any package regardless of
     * visibility and finality modifiers.
     *
     * @throws SecurityException If an access check fails
     */
    public void setAccessible() throws SecurityException {
        /* Perform security checks on all the fields and methods now, to save
        time later, and allow the possibility of dropping permissions. */
        AccessibleObject.setAccessible(
                Stream.concat(fields.stream(), methods.stream()).toArray(
                        (n) -> new AccessibleObject[n]), true);
    }

    /**
     * Returns the class that is described in this database (that is, the actual
     * class of objects that the database describes).
     *
     * @return <code>T.class</code>
     */
    public Class<T> getAbout() {
        return about;
    }

    /**
     * Produces a list of fields that objects described by this class possess.
     * For any given class, its fields will always be returned in the same
     * order, even on different computers or on different runs of the program.
     * <p>
     * Bear in mind that this will not contain static fields; those belong to
     * the class, not the object.
     *
     * @return An unmodifiable ordered list of fields.
     */
    public final Iterable<Field> getInstanceFieldList() {
        return unmodifiableSortedSet(fields);
    }

    /**
     * Produces a set of methods that objects described by this class possess.
     * This will not be in any particular order.
     *
     * @return An unmodifiable set of methods.
     */
    public Set<Method> getMethods() {
        return unmodifiableSet(methods);
    }

    /**
     * Produces a set of interfaces that objects described by this class
     * implement. This is not ordered, and includes interfaces that are
     * implemented by the class's superclasses, indirectly via other interfaces,
     * etc..
     *
     * @return An unmodifiable set of interfaces.
     */
    public Set<Class<?>> getInterfaces() {
        return unmodifiableSet(interfaces);
    }

    /**
     * Returns an extensible non-interface class that objects described by this
     * class belong to, as specific as possible. In other words, a class that
     * other classes can be declared as extending.
     * <p>
     * When creating wrapper classes to replace the objects described by this
     * class, making them extend this class, and implement the interfaces
     * returned by <code>getInterfaces</code>, will make them able to be stored
     * in as many of the variables that could hold the original object as
     * possible.
     *
     * @return The most specific extensible superclass of the objects this
     * database is describing.
     */
    public Class<? super T> getClosestExtensibleSuperclass() {
        return closestExtensibleSuperclass;
    }

    /**
     * Returns whether the class described by the database has non-private
     * methods that are not declared by a superclass or interface. In other
     * words, it has some method that can be called (without reflection) only if
     * the variable storing the object has the same declared type as the
     * object's actual type.
     *
     * @return <code>true</code> if the class has callable methods that are not
     * simply implementations of a parent's/interface's specification.
     */
    public boolean hasOwnMethods() {
        return hasOwnMethods;
    }

    /**
     * Returns whether the class described by this database has non-private
     * methods that are not specified by an interface. The methods of
     * <code>Object</code> count as interface methods for this purpose (as
     * they're implicitly specified by every interface).
     *
     * @return <code>true</code> if the class has callable methods that are not
     * simply implementations of a specification in an interface, nor methods of
     * <code>Object</code>.
     */
    public boolean hasNonInterfaceMethods() {
        return hasNonInterfaceMethods;
    }

    /**
     * Determines whether a given class can be extended. A class cannot be
     * extended if it's <code>final</code>, anonymous, synthetic, or
     * package-private to a sealed package. (Note that a class cannot be
     * <code>private</code>, even if Java's syntax might seem like it; marking
     * an inner class as private actually marks all its constructors as private
     * instead. As such, the class <i>can</i> be extended, just not constructed
     * in the normal way.)
     *
     * @param c The class to check.
     * @return Whether the class can be extended.
     */
    public static boolean isClassExtensible(Class<?> c) {
        int mod = c.getModifiers();

        if (isFinal(mod) || c.isSynthetic() || c.isAnonymousClass()) {
            return false;
        }
        if (!isPublic(mod)) {
            return !isClassNameInSealedPackage(c.getName());
        }
        return true;
    }
}
