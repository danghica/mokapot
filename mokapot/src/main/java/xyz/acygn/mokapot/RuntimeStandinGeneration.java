package xyz.acygn.mokapot;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.SEIZE;
import xyz.acygn.mokapot.skeletons.SeizeableStandin;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.UniversalInvocationHandler;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;

/**
 * A class that handles generating standins at runtime via high-level code. In
 * other words, in cases where, for whatever reason, we can't handle bytecode
 * directly.
 *
 * @author Alex Smith
 */
class RuntimeStandinGeneration {

    /**
     * A map of all classes we've generated, together with their initialisers.
     * Each name is supposed to uniquely define a class; thus, if we're given a
     * name but we've already generated a class of that name, we can just return
     * it directly.
     */
    private static final Map<String, ClassAndInitialiser<?>> generatedClasses
            = new HashMap<>();

    /**
     * Generates a standin class via the use of a proxying library. In other
     * words, we're using "stock" generated class bytecode, and all the
     * operations of the standin will be performed via a default method handler.
     * This is slow but doesn't require any bytecode-level operations.
     *
     * @param <T> The class for which the standin will stand in.
     * @param technique The technique via which the standin is implemented.
     * @param forClass <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @param className The name of the class to generate.
     * @return A pair of the standin class (either generated on the spot or
     * reused from a previous call to this method), and an initialiser used to
     * initialise the objects after their magical construction
     * @throws IllegalArgumentException if no runtime standin generation
     * strategy is available for the given <code>technique</code>
     * @throws StandinFactory.CannotConstructException if a runtime standin
     * generation strategy should have been available for the given
     * <code>technique</code>, but all attempts to use it failed
     */
    static synchronized <T> ClassAndInitialiser<T> generateStandinClass(
            StandinTechnique technique, Class<T> forClass, String className)
            throws IllegalArgumentException,
            StandinFactory.CannotConstructException {
        Objects.requireNonNull(technique);
        if (technique.proxyHandlerFactory == null) {
            throw new IllegalArgumentException("technique " + technique
                    + " cannot be used for runtime standin generation");
        }

        if (generatedClasses.containsKey(className)) {
            @SuppressWarnings("unchecked")
            ClassAndInitialiser<T> rv
                    = (ClassAndInitialiser) generatedClasses.get(className);
            return rv;
        }

        ObjectWireFormat<T> owf
                = knowledgeForClass(forClass).getWireFormat();
        Class<?>[] interfaces = Stream.concat(Stream.of(technique.functionalities.contains(SEIZE)
                ? SeizeableStandin.class : Standin.class),
                owf.getAccessibleInterfaces().stream()).sorted(
                (c1, c2) -> c1.getName().compareTo(c2.getName()))
                .toArray(Class<?>[]::new);
        Class<?> superclass = technique.standinClass;
        if (superclass.isInterface()) {
            /* If the standin technique is entirely interface-based, that's
               presumably to allow us to extend a superclass that's parallel to
               the standin itself. */
            superclass = owf.getClosestExtensibleSuperclass();
        }

        /* Ugh: for some reason, the Javassist naming strategy is a global. Try
           to work around this using dynamic scope; we're inside a synchronized
           block, which will work around the race conditions that normally
           result in using dynamic scope in a multithreaded program. This
           assumes that nothing else is trying to use Javassist at the same
           time, but there's not much else we can do. */
        try {
            final Class<?>[] finalInterfaces = interfaces;
            final Class<?> finalSuperclass = superclass;
            return AccessController.doPrivileged(
                    (PrivilegedExceptionAction<ClassAndInitialiser<T>>) ()
                    -> generateJavassistProxy(
                            className, finalSuperclass, finalInterfaces,
                            technique.proxyHandlerFactory, forClass));
        } catch (PrivilegedActionException ex) {
            throw new StandinFactory.CannotConstructException(ex.getCause());
        }
    }

    /**
     * Handles the work of actually generating a Javassist proxy.
     *
     * @param <T> The class being generated.
     * @param className The name of the class to generate.
     * @param superclass The superclass of the class being generated.
     * @param interfaces The interfaces that the class should implement.
     * @param handlerFactory The factory with which to create the proxy's
     * invocation handler.
     * @param forClass <code>T.class</code>.
     * @return A class, together with an initialiser for it.
     * @throws xyz.acygn.mokapot.StandinFactory.CannotConstructException If the
     * class could not be created
     */
    private static <T> ClassAndInitialiser<T> generateJavassistProxy(
            String className, Class<?> superclass, Class<?>[] interfaces,
            Function<Class<?>, UniversalInvocationHandler> handlerFactory,
            Class<T> forClass)
            throws Exception {
        ProxyFactory.UniqueName oldNamingStrategy
                = ProxyFactory.nameGenerator;
        try {
            ProxyFactory.nameGenerator = (superClassName) -> className;
            ProxyFactory proxyBuilder = new ProxyFactory();

            proxyBuilder.setUseCache(false); // we do caching ourself
            proxyBuilder.setUseWriteReplace(false);
            proxyBuilder.setSuperclass(superclass);
            proxyBuilder.setInterfaces(interfaces);
            /* The cast here is necessary with some versions of
               Javassist, and redundant with others; the method doesn't
               have the same type in every version. So unfortunately,
               some people will have to live with the warning here. */
            @SuppressWarnings("unchecked")
            Class<? extends Standin<T>> rClass = (Class) proxyBuilder.createClass();
            ClassAndInitialiser<T> rv
                    = new ClassAndInitialiser<>(rClass, (s) -> {
                        UniversalInvocationHandler handler
                                = handlerFactory.apply(forClass);
                        ((Proxy) s).setHandler(handler);
                    });
            generatedClasses.put(className, rv);
            return rv;
        } finally {
            ProxyFactory.nameGenerator = oldNamingStrategy;
        }
    }

    /**
     * Inaccessible constructor. This is a utility class not meant to be
     * instantiated.
     */
    private RuntimeStandinGeneration() {
    }

    /**
     * A standin class, together with an initialiser for it. This is basically
     * just a type alias to make the code easier to read and to help out Java's
     * type inference algorithm.
     *
     * @param <T>
     */
    static class ClassAndInitialiser<T> extends
            Pair<Class<? extends Standin<T>>, Consumer<Standin<T>>> {

        /**
         * Creates a new pair of a standin class and an initialiser for it.
         *
         * @param c The standin class.
         * @param initialiser The class's initialiser.
         */
        ClassAndInitialiser(Class<? extends Standin<T>> c,
                Consumer<Standin<T>> initialiser) {
            super(c, initialiser);
        }
    }
}
