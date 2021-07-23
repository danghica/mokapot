package xyz.acygn.mokapot;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import xyz.acygn.millr.generation.StandinGenerator;
import static xyz.acygn.mokapot.Authorisations.OBJECT_CREATOR;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.LOOKUP;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.DESCRIBE;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.JIT_GENERATABLE;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.NON_JVM_GENERATABLE;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.WRAP_EXISTING;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.WRAP_MANAGED;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.SeizeableStandin;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.TrivialStandinStorage;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Lazy;
import xyz.acygn.mokapot.util.Pair;
import static xyz.acygn.mokapot.util.VMInfo.isClassNameInSealedPackage;
import static xyz.acygn.mokapot.util.VMInfo.isRunningOnAndroid;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * A factory that creates standins belonging to a generated class. When the
 * factory object itself is created, it attempts to load the standin class if it
 * already exists, or generate it if it doesn't. It can subsequently create
 * objects of the standin in question.
 * <p>
 * Note that this class is not <code>StandinGenerator</code>, i.e. it simply
 * works out how to generate the standin class (if doing so is even necessary)
 * rather than actually doing the generation (this will be delegated to other
 * classes if necessary).
 *
 * @author Alex Smith
 * @param <T> The class that the generated standins stand in for.
 */
class GeneratedStandinFactory<T> implements StandinFactory<T> {

    /**
     * Class loader to use in the case that the class loader we want is not
     * accessible. For example, if it's the bootstrap class loader and thus is
     * represented as <code>null</code>.
     */
    private final static ClassLoader FALLBACK_CLASS_LOADER
            = GeneratedStandinFactory.class.getClassLoader();

    /**
     * Method handle for injecting classes into the class loaders. Obviously,
     * this should be kept well clear of untrusted code.
     */
    private static final MethodHandle CLASS_INJECTOR;

    static {
        try {
            CLASS_INJECTOR = doPrivileged(
                    (PrivilegedExceptionAction<MethodHandle>) () -> {
                        Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                                "defineClass", String.class, byte[].class,
                                int.class, int.class);
                        defineClassMethod.setAccessible(true);
                        return LOOKUP.unreflect(defineClassMethod);
                    });
        } catch (PrivilegedActionException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Initialisers for runtime-generated standins. In some cases, we'll be
     * using the same generated class multiple times, so we need to store these
     * for at least as long as the class is loaded (i.e. for the lifetime of the
     * program).
     */
    private static final Map<Pair<Class<?>, StandinTechnique>, Consumer<Standin<?>>> initializers
            = new HashMap<>();

    /**
     * The standin technique that the resulting standin class uses.
     */
    private final StandinTechnique technique;

    /**
     * The class for which the standins are standing in.
     */
    private final Class<T> forClass;

    /**
     * The standin class itself.
     */
    private final Class<? extends Standin<T>> standinClass;

    /**
     * A function that runs on standins that have just been created without
     * using their constructor. It sets all of their state other than their
     * storage and their referent.
     * <p>
     * This could be <code>null</code>, in which case no action is required.
     */
    private final Consumer<Standin<T>> initializer;

    /**
     * Loads or generates a standin class, and creates a factory object for
     * instantiating that class.
     *
     * @param technique The technique via which the standin should be
     * implemented.
     * @param forClass The class that the new standin class will stand in for.
     * @throws CannotConstructException If the standin class can neither be
     * generated nor loaded
     */
    @SuppressWarnings("unchecked")
    GeneratedStandinFactory(StandinTechnique technique, Class<T> forClass)
            throws CannotConstructException {
        this.technique = technique;
        this.forClass = forClass;

        /* Load the standin class. First, we need to determine what it's
           called. */
        String standinClassName = technique.mangleClassName(forClass.getName());

        /* First attempt; is it already loaded and/or can we load it from
           disk? */
        ClassLoader forClassLoader = forClass.getClassLoader();
        if (forClassLoader == null) {
            forClassLoader = FALLBACK_CLASS_LOADER;
        }

        Class<? extends Standin<T>> candidate = null;
        Throwable failReason = null;

        try {
            /* We're assuming here that nobody else has put a malicious class
               with the name in question on the class loader's classpath. Also
               that the class loader in question will fail to find the class if
               it isn't already loaded and isn't on disk. These both seem like
               reasonable assumptions; the former is about the environment, the
               latter might be broken by a particularly insane class loader.

               The warning suppress is needed because Java's type system
               understandably can't make assumptions about a class that we
               freshly loaded from disk. */
            @SuppressWarnings("unchecked")
            Class<? extends Standin<T>> loadedStandinClass
                    = (Class) forClassLoader.loadClass(standinClassName);
            candidate = loadedStandinClass;
        } catch (ClassNotFoundException ex) {
            try (DeterministicAutocloseable ac
                    = Lazy.TIME_BASE.get().pause()) {
                /* Second attempt: can we generate and load it on the spot? */
                if (safeToJITGenerate(forClass, technique)
                        && technique.functionalities.contains(JIT_GENERATABLE)) {
                    StandinGenerator<T> generator
                            = new StandinGenerator<>(
                                    ClassKnowledge.knowledgeForClass(forClass)
                                            .getWireFormat());
                    // TODO: This is just temporary until the generator
                    // understands standin techniques.
                    byte[] bytecode;
                    switch (technique) {
                        case INHERITED_FROM_CLASS:
                            bytecode = generator.generateAsBytecode(true).getFirst();
                            break;
                        case INDIRECT_STANDIN:
                            bytecode = generator.generateAsBytecode(false).getFirst();
                            break;
                        default:
                            throw new UnsupportedOperationException("TODO");
                    }
                    candidate = (Class<? extends Standin<T>>) CLASS_INJECTOR.
                            invoke(forClassLoader, standinClassName, bytecode,
                                    0, bytecode.length);
                }
            } catch (Throwable ex1) {
                /* Something went wrong; fall through to the third attempt.
                   We can't catch more specifically than this because we're
                   calling the class injector via a method handle. */
                failReason = ex1;
            }
            /* Third attempt: can we generate the class via a library such as
               Javassist or Dexmaker? */
            try (DeterministicAutocloseable ac
                    = Lazy.TIME_BASE.get().pause()) {
                if (candidate == null
                        && technique.functionalities.contains(NON_JVM_GENERATABLE)) {
                    Pair<Class<? extends Standin<T>>, Consumer<Standin<T>>> candidatePair
                            = RuntimeStandinGeneration.generateStandinClass(
                                    technique, forClass, standinClassName);
                    candidate = candidatePair.getFirst();
                    initializers.put(new Pair<>(forClass, technique),
                            (Consumer) candidatePair.getSecond());
                } else if (candidate == null) {
                    throw new CannotConstructException(failReason);
                }
            }
        }

        standinClass = candidate;
        /* HashMap isn't dependently-typed, so we need a manual type system
           override to make this work. */
        initializer = (Consumer) initializers.get(new Pair<>(forClass, technique));
    }

    @Override
    public Standin<T> newFromDescription(ReadableDescription description)
            throws IOException, UnsupportedOperationException {
        if (!technique.functionalities.contains(DESCRIBE)) {
            throw new UnsupportedOperationException("Standin " + standinClass
                    + " does not support undescription");
        }

        /* We have a general rule that standins can be created without using
           their constructor if you first undrop them, then set their
           storage. */
        Standin<T> rv = newStandinClassInstance();
        if (initializer != null) {
            initializer.accept(rv);
        }
        rv.undropResources(UNRESTRICTED);
        rv.replaceWithReproduction(forClass, description, UNRESTRICTED);
        rv.setStorage(new TrivialStandinStorage<>(), UNRESTRICTED);
        return rv;
    }

    @Override
    public Standin<T> wrapObject(T t) throws UnsupportedOperationException {
        if (!technique.functionalities.contains(WRAP_EXISTING)) {
            throw new UnsupportedOperationException("Standin " + standinClass
                    + " does not support wrapping existing objects");
        }

        Standin<T> rv = newStandinClassInstance();
        if (initializer != null) {
            initializer.accept(rv);
        }
        if (rv instanceof SeizeableStandin) {
            ((SeizeableStandin<T>) rv).seizeReferent(t, UNRESTRICTED);
        } else {
            throw new UnsupportedOperationException("Standin " + standinClass
                    + " does not support seizeing existing objects");
        }
        rv.setStorage(new TrivialStandinStorage<>(), UNRESTRICTED);
        return rv;
    }

    @Override
    public Standin<T> standinFromLocationManager(LocationManager<T> lm)
            throws UnsupportedOperationException {
        if (!technique.functionalities.contains(WRAP_MANAGED)) {
            throw new UnsupportedOperationException("Standin " + standinClass
                    + " does not support wrapping location managers");
        }

        Standin<T> rv = newStandinClassInstance();
        if (initializer != null) {
            initializer.accept(rv);
        }
        /* Note: we can't normally do this with a dropped referent, but a
           special case in IndirectStandin detects and allows it, and a direct
           standin can't have a dropped referent by definition. */
        rv.setStorage(new ForwardingStandinStorage<>(lm), UNRESTRICTED);
        return rv;
    }

    /**
     * Creates a new instance of the standin class. The class's constructor will
     * not be run.
     *
     * @return A new standin class instance.
     */
    private Standin<T> newStandinClassInstance() {
        Standin<T> rv = doPrivileged(
                (PrivilegedAction<Standin<T>>) () -> OBJECT_CREATOR.newInstance(standinClass));
        return rv;
    }

    /**
     * Determines whether it's safe to generate a standin by loading bytecode in
     * a just-in-time fashion. It's unsafe either if we're running on a system
     * that does not use Java bytecode, or if the generated bytecode would
     * violate Java's visibility rules.
     *
     * @param forClass The class for which the standin is being generated.
     * @param technique The technique that would be used.
     * @return <code>true</code> if generating the standin in this way is safe.
     */
    private boolean safeToJITGenerate(
            Class<T> forClass, StandinTechnique technique) {
        if (isRunningOnAndroid()) {
            return false;
        }
        return !isClassNameInSealedPackage(forClass.getName())
                || !technique.equals(StandinTechnique.INDIRECT_STANDIN);
    }

    /**
     * Metafactory class that lazily generates GeneratedStandinFactories. The
     * laziness is important to avoid an infinite regress during the loading of
     * <code>StandinTechnique</code> itself. For similar reasons, this class
     * does not refer to <code>StandinTechnique</code> during its construction,
     * only at runtime.
     */
    static class MetaFactory implements StandinFactory.Factory {

        /**
         * The standin technique used by the GeneratedStandinFactory.
         * Initialised lazily.
         */
        private StandinTechnique lazyTechnique = null;

        /**
         * The filecode of the technique to use. Used to identify it before the
         * techniques themselves are loaded.
         */
        private final int techniqueFileCode;

        /**
         * Creates a standin metafactory that creates standin factories that
         * create standins that use a given technique.
         *
         * @param techniqueFileCode The file code of the technique to use.
         */
        MetaFactory(int techniqueFileCode) {
            this.techniqueFileCode = techniqueFileCode;
        }

        @Override
        public synchronized <T> StandinFactory<T>
                metafactory(Class<T> referentClass)
                throws CannotConstructException {
            if (lazyTechnique == null) {
                for (StandinTechnique technique : StandinTechnique.values()) {
                    if (technique.fileCode == techniqueFileCode) {
                        lazyTechnique = technique;
                        break;
                    }
                }
                if (lazyTechnique == null) {
                    throw new RuntimeException(
                            "Nonexistent standin technique filecode "
                            + techniqueFileCode);
                }
            }

            return new GeneratedStandinFactory<>(lazyTechnique, referentClass);
        }
    }
}
