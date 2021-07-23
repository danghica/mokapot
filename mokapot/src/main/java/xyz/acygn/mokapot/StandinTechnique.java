package xyz.acygn.mokapot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.*;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.IndirectStandin;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.ComparablePair;
import xyz.acygn.mokapot.util.ObjectMethodDatabase;
import xyz.acygn.mokapot.util.UniversalInvocationHandler;
import xyz.acygn.mokapot.util.VMInfo;

/**
 * A technique via which a standin is implemented.
 * <p>
 * There's no 100% universal way to implement a standin for a class. Instead,
 * different techniques are used for different classes. Techniques vary in their
 * strengths and weaknesses (e.g. some can be used with more classes but have
 * less functionality, and some with fewer classes but have more).
 *
 * @author Alex Smith
 */
enum StandinTechnique {

    /**
     * The "standin" is actually just the object itself. Used in cases where
     * none of the standin functionality is necessary and some or all of it is
     * unavailable.
     */
    OBJECT_ITSELF(0, 0.6f, Object.class, null, null, CALL_DIRECT,
            CALL_SUPERCLASS, CALL_INTERFACE, STANDIN_FOR_FINAL,
            STANDIN_FOR_JAVA_PACKAGE, LOCAL_STORAGE, NON_JVM_GENERATABLE),
    /**
     * A standin whose referent is a separate object, and whose class is
     * unrelated to that of the object. This does, however, require implementing
     * direct calls to the referent's objects at at least the interface level.
     * <p>
     * Non-JVM-generation of these is useful only in cases where
     * <code>CALL_INTERFACE</code> is vital; otherwise,
     * <code>ReflectiveStandin</code> will work better. (Perhaps this needs a
     * scaling preference based on which of AOT, JIT, and non-JVM is available.)
     * <p>
     * Note: when generating standins for java.*, this sort of standin cannot be
     * AOT/JIT generated, and so its preference is artificially reduced to 0.1
     * (as even a reflective standin would be better).
     */
    INDIRECT_STANDIN(1, 0.5f, IndirectStandin.class,
            new GeneratedStandinFactory.MetaFactory(1),
            RuntimeGeneratedStandinHandler::new, CALL_INTERFACE,
            CALL_INDIRECT, DESCRIBE, LOCAL_STORAGE, REMOTE_STORAGE, MIGRATION,
            NON_JVM_GENERATABLE, STANDIN_FOR_FINAL, STANDIN_FOR_JAVA_PACKAGE,
            WRAP_EXISTING, WRAP_MANAGED, AOT_GENERATABLE, JIT_GENERATABLE,
            SEIZE),
    /**
     * A standin that inherits from the class it stands in for. This only works
     * when the class can actually be extended (i.e. is not <code>final</code>
     * and not in a package in <code>java.*</code>), but is almost fully
     * general; its only real drawback is failing to correctly handle methods
     * whose implementation calls <code>getClass</code> on <code>this</code>.
     */
    INHERITED_FROM_CLASS(2, 0.8f, Standin.class,
            new GeneratedStandinFactory.MetaFactory(2), null, CALL_DIRECT,
            CALL_SUPERCLASS, CALL_INTERFACE, CALL_INDIRECT, DESCRIBE,
            LOCAL_STORAGE, REMOTE_STORAGE, MIGRATION, WRAP_MANAGED,
            AOT_GENERATABLE, JIT_GENERATABLE, STORE_IN_OWN_CLASS),
    /* TODO: It would be nice to use something along the lines of
       RuntimeGeneratedStandinHandler to implement this, but that would require
       some way to do invokespecial via reflection, which I'm currently unaware
       of how to do. */
    /**
     * A standin that inherits from the superclass of the class it stands in
     * for. When using stored-in-self status, the referent will be a separate
     * object of the appropriate class.
     * <p>
     * Note, by "superclass" we mean the closest extensible superclass. This may
     * be the class of the object itself in cases where the object's own class
     * is extensible. (We might use this rather than
     * <code>INHERITED_FROM_CLASS</code> to get at <code>WRAP_EXISTING</code> or
     * because there's no way to generate <code>INHERITED_FROM_CLASS</code>.)
     * This means that the functionalities <code>CALL_DIRECT</code> and
     * <code>STORE_IN_OWN_CLASS</code> are conditional;
     * <code>findBestStandinTechnique</code> has a special case for these.
     */
    INHERITED_FROM_SUPERCLASS(3, 0.3f, Standin.class,
            new GeneratedStandinFactory.MetaFactory(3),
            RuntimeGeneratedStandinHandler::new, CALL_SUPERCLASS,
            CALL_INTERFACE, CALL_INDIRECT, DESCRIBE, LOCAL_STORAGE,
            REMOTE_STORAGE, MIGRATION, STANDIN_FOR_FINAL, WRAP_EXISTING, SEIZE,
            WRAP_MANAGED, AOT_GENERATABLE, JIT_GENERATABLE,
            NON_JVM_GENERATABLE),
    /**
     * A standin implemented entirely using reflection. This is basically a slow
     * fallback in cases where other techniques don't work. Non-JVM generation
     * of these standins is possible purely via the Java API.
     */
    REFLECTIVE_STANDIN(4, 0.2f, ReflectiveStandin.class,
            ReflectiveStandin.Factory::new, null, CALL_INDIRECT, DESCRIBE,
            LOCAL_STORAGE, REMOTE_STORAGE, MIGRATION, NON_JVM_GENERATABLE,
            STANDIN_FOR_FINAL, STANDIN_FOR_JAVA_PACKAGE,
            WRAP_EXISTING, SEIZE, WRAP_MANAGED),
    /**
     * A very lightweight standin used when no functionality is required other
     * than referring to a location manager.
     */
    @SuppressWarnings("Convert2Lambda")
    REMOTE_ONLY_STANDIN(5, 0.9f, RemoteOnlyStandin.class,
            new StandinFactory.Factory() {
        /* Yes, I know this looks like a trivial lambda, but if you try writing
           it as a lambda, Java's type inference can't handle it. Not even if
           you give the types explicitly. */
        @Override
        public <T> StandinFactory<T> metafactory(Class<T> referentClass) {
            return new RemoteOnlyStandin.Factory<>();
        }
    }, null, REMOTE_STORAGE, STANDIN_FOR_FINAL, STANDIN_FOR_JAVA_PACKAGE,
            WRAP_MANAGED, NON_JVM_GENERATABLE);

    /**
     * A number used to represent standin techniques in file names and class
     * names. This number is then used to attempt to locate an AOT-compiled
     * version of a standin on disk.
     */
    public final int fileCode;

    /**
     * A number that controls which standin implementation to use in cases where
     * multiple implementations are available. Higher values will be preferred
     * to lower values. The value must be strictly between 0 and 1.
     */
    private final float preference;

    /**
     * A method that creates factory methods for standin classes that use this
     * standin technique. This exists to make it possible to create standins in
     * an automated way, once we've identified which class they belong to.
     * <p>
     * This can be null in the case of a degenerate standin technique, such as
     * <code>OBJECT_ITSELF</code>, which does not actually produce standins.
     */
    private final StandinFactory.Factory metafactory;

    /**
     * The most precise class that all standins that use this technique
     * implement. In some cases, this class will have a factory method for the
     * standins; in other cases, the standin classes will be generated via some
     * other means and will implement this class.
     * <p>
     * In extreme cases, this can be <code>Object</code> (i.e. the "standin" is
     * not actually a standin class; this is normally used when a particular
     * class is forced, as in the case of trying to make a "standin" for
     * <code>String</code>).
     */
    public final Class<?> standinClass;

    /**
     * A factory that creates proxy handlers that can be used to implement a
     * standin class via reflecting every method. Used as an emergency fallback
     * if no other option is available; this tends to be very slow. Can be
     * <code>null</code>.
     */
    public final Function<Class<?>, UniversalInvocationHandler> proxyHandlerFactory;

    /**
     * The set of functionalities implemented by the standin technique. This is
     * used when trying to find a standin with a specific functionality (or
     * subset of functionalities). This is an immutable set, and cannot be
     * written to.
     */
    public final Set<Functionality> functionalities;

    /**
     * Creates a new enum constant with the given fields, representing a standin
     * technique. This constructor is used only to create the standard enum
     * constants contained in this class, and (being private) cannot be used at
     * runtime to create additional enum constants.
     *
     * @param fileCode The file code of the standin technique.
     * @param preference How favoured this particular standin implementation
     * should be, when multiple work; higher is better. Must be strictly between
     * 0 and 1.
     * @param standinClass The class that all standins using this technique
     * implement.
     * @param metafactory The metafactory used to create standin factories that
     * create standins that use this standin technique.
     * @param proxyHandlerFactory A factory that produces invocation handlers
     * for proxies, allowing the creation of a proxy that implements this
     * technique. Can be <code>null</code> if this is not possible.
     * @param functionalities The functionalities implemented by the standin
     * technique.
     */
    private StandinTechnique(int fileCode, float preference,
            Class<?> standinClass, StandinFactory.Factory metafactory,
            Function<Class<?>, UniversalInvocationHandler> proxyHandlerFactory,
            Functionality... functionalities) {
        this.fileCode = fileCode;
        if (preference <= 0.0f || preference >= 1.0f) {
            throw new IllegalArgumentException(
                    "Standin preference must be positive and less than 1");
        }
        this.preference = preference;
        this.metafactory = metafactory;
        this.standinClass = standinClass;
        this.proxyHandlerFactory = proxyHandlerFactory;
        this.functionalities = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(functionalities)));
    }

    /**
     * Given the name of a class, work out the name of a standin for that class
     * that uses this technique. Normally this will simply involve appending
     * something to the class name, but if the class is in a sealed package, a
     * standin for it will need to be moved to a different package.
     * <p>
     * Note that the class need not actually exist; this method can also be used
     * to determine the name that would be given for a hypothetical standin for
     * a hypothetical, currently nonexistent class.
     *
     * @param className The name of the class, including a package name, in
     * either Java format (period-separated) or JVM binary format
     * (slash-separated).
     * @return The name of a standin for that class that uses this technique, in
     * the same format as <code>className</code> was.
     */
    String mangleClassName(String className) {
        String mangledClassName = className;
        if (VMInfo.isClassNameInSealedPackage(className)) {
            if (className.contains("/")) {
                mangledClassName = "xyz/acygn/mokapot/wrapper/" + className;
            } else {
                mangledClassName = "xyz.acygn.mokapot.wrapper." + className;
            }
        }
        return mangledClassName + "$mokapot_standin" + fileCode;
    }

    /**
     * Finds the best possible standin constructor for a given class, assuming a
     * given list of functionalities are required. If not all the
     * functionalities provided can be satisfied simultaneously, as many as
     * possible from the start of the list are used.
     * <p>
     * The list of functionalities required will automatically be modified by
     * adding any functionalities that are required to create the standin in the
     * first place; for example, if no appropriate standin class has been loaded
     * yet and no appropriate standin class was generated ahead of time, the
     * resulting standin will need to be either <code>JIT_GENERATABLE</code> or
     * <code>NON_JVM_GENERATABLE</code>. It will also remove functionalities
     * that request the ability to make calls to methods the class doesn't
     * actually have.
     * <p>
     * This function is fairly slow; caching the results is recommended if you
     * plan to call it repeatedly with the same class and functionality list.
     *
     * @param <T> The class to find the standin for. This must not be a standin
     * class itself.
     * @param db An object method database for the class that the standin must
     * be found for.
     * @param functionalities The list of desired functionalities. As many as
     * possible from the start of the list will be provided.
     * @return The standin class.
     */
    static <T> StandinFactory<T> findBestStandinFactory(
            ObjectMethodDatabase<T> db, Functionality... functionalities) {
        Class<T> about = db.getAbout();
        if (Standin.class.isAssignableFrom(about)) {
            throw new IllegalArgumentException(
                    "Cannot create a standin for standin class" + db.getAbout());
        }
        Set<Functionality> requiredFunctionalities = new HashSet<>();
        List<Functionality> requestedFunctionalities
                = new ArrayList<>(functionalities.length);
        for (Functionality f : functionalities) {
            if (f.equals(VITAL)) {
                requiredFunctionalities.addAll(requestedFunctionalities);
                requestedFunctionalities.clear();
            } else {
                requestedFunctionalities.add(f);
            }
        }

        /* Add functionalities related to the type of class we can call. */
        if (!ObjectMethodDatabase.isClassExtensible(about)) {
            if (!ObjectMethodDatabase.isClassExtensible(about.getSuperclass())) {
                requiredFunctionalities.add(STANDIN_FOR_JAVA_PACKAGE);
            }
            requiredFunctionalities.add(STANDIN_FOR_FINAL);
        }

        /* Remove functionalities related to calling methods, if those methods
           don't exist. */
        if (!db.hasOwnMethods()) {
            requestedFunctionalities.remove(CALL_DIRECT);
        }
        if (!db.hasNonInterfaceMethods()) {
            requestedFunctionalities.remove(CALL_SUPERCLASS);
        }

        PriorityQueue<ComparablePair<Float, StandinTechnique>> orderedTechniques
                = new PriorityQueue<>();
        for (StandinTechnique technique
                : StandinTechnique.class.getEnumConstants()) {
            /* We need all the required functionalities. */
            if (!technique.functionalities.containsAll(requiredFunctionalities)) {
                continue;
            }

            /* How many requested functionalities do we have, from the start of
               the list? Add that to the preference. */
            float preference = technique.preference;
            if (technique.equals(INDIRECT_STANDIN)
                    && requestedFunctionalities.contains(STANDIN_FOR_JAVA_PACKAGE)) {
                /* A special case, taking into account the difficulty of
                   generating this specific sort of standin (it has to do
                   everything via reflection, which is slow). */
                preference = 0.1f;
            }
            for (Functionality f : requestedFunctionalities) {
                if ((f.equals(CALL_DIRECT) || f.equals(STORE_IN_OWN_CLASS))
                        && technique.equals(INHERITED_FROM_SUPERCLASS)
                        && db.getClosestExtensibleSuperclass().equals(about)) {
                    /* INHERITED_FROM_SUPERCLASS has CALL_DIRECT conditionally.
                       It's not in the functionalities list, but in the case
                       where the class is its own closest extensible superclass,
                       we act as though it were, i.e. we don't do the next
                       check. */
                } else if (!technique.functionalities.contains(f)) {
                    break;
                }
                preference += 1.0f;
            }

            /* Add the technique to our list of possibilities.

               Values are retrieved from prioirty queues in ascending order.
               We want the highest preference first, so we negate the
               preferences as a simple method of reversing the sort order. */
            orderedTechniques.add(new ComparablePair<>(-preference, technique));
        }

        Exception failReason = null;

        while (!orderedTechniques.isEmpty()) {
            /* We're trying the techniques in order, from best to worst. */
            StandinTechnique bestTechnique
                    = orderedTechniques.remove().getSecond();

            if (bestTechnique.metafactory == null) {
                continue;
            }

            try {
                StandinFactory<T> rv = bestTechnique.metafactory.metafactory(about);
                Objects.requireNonNull(rv);
                return rv;
            } catch (StandinFactory.CannotConstructException ex) {
                failReason = ex;
            }
        }

        throw new DistributedError(failReason == null
                ? new LinkageError("no standin type available") : failReason,
                "Cannot create any sort of standin for " + about);
    }

    /**
     * A functionality that a standin technique can provide. In other words,
     * something that can be done reliably with the created standin.
     */
    public static enum Functionality {
        /**
         * A marker specifying that previous functionalities are mandatory. If
         * those functionalities can't be respected, you get an exception rather
         * than an inadequate standin type.
         */
        VITAL,
        /**
         * This standin technique works even if the class itself is
         * <code>final</code>.
         */
        STANDIN_FOR_FINAL,
        /**
         * This standin technique works even if the class's superclass is
         * package-private to <code>java.*</code>.
         */
        STANDIN_FOR_JAVA_PACKAGE,
        /**
         * An object of the standin can be stored in a variable whose class is
         * the object's actual class. Typically needed to make practical use out
         * of <code>CALL_DIRECT</code>, but the two are conceptually different.
         */
        STORE_IN_OWN_CLASS,
        /**
         * Methods of the object itself can be called on the standin. They must
         * produce the correct results both if the object's storage is local and
         * if the object's storage is remote (except in cases where the method
         * itself has some shortcomings that make this impossible, e.g. calling
         * <code>this.getClass</code> or being <code>final</code> or
         * <code>private</code>).
         */
        CALL_DIRECT,
        /**
         * Methods of any of the object's superclasses can be called on the
         * standin.
         */
        CALL_SUPERCLASS,
        /**
         * Methods of the object's interfaces (and <code>Object</code>) can be
         * called on the standin. This follows the same rules as for
         * <code>CALL_DIRECT</code>.
         */
        CALL_INTERFACE,
        /**
         * Methods can be called on the standin's referent (bypassing any
         * forwarding logic) via the use of <code>Standin#invoke</code>.
         *
         * @see Standin#invoke(long, java.lang.Object[],
         * xyz.acygn.mokapot.skeletons.Authorisation)
         */
        CALL_INDIRECT,
        /**
         * The standin supports marshalling and description/undescription of its
         * referent. In other words, its methods can be used to send data across
         * a network.
         */
        DESCRIBE,
        /**
         * The standin allows stored-in-self storage. Most standins allow this,
         * but a few very limited/special-case standins might not.
         */
        LOCAL_STORAGE,
        /**
         * The standin allows storages other than stored-in-self. This makes it
         * suitable for use as a long reference or to associate it with a
         * location manager.
         */
        REMOTE_STORAGE,
        /**
         * The standin's referent can be created before the standin. This means
         * that the standin can be used to add standin functionality to an
         * existing object.
         */
        WRAP_EXISTING,
        /**
         * The relationship between the standin and its referent can be set with
         * both the standin and referent already existing. This functionality is
         * used when creating standins to wrap existing objects without the use
         * of their constructor.
         */
        SEIZE,
        /**
         * The standin can be created using an existing location manager as its
         * storage. This implies that the standin will initially have no
         * referent.
         */
        WRAP_MANAGED,
        /**
         * The standin can start and stop using its referent at runtime. This
         * must be possible without breaking current uses of the standin, and
         * without allowing data to become stale (i.e. it must be possible to
         * copy the data out of the standin's referent into a different
         * location, then point the standin at that location, without anything
         * happening in between). It's OK to assume that no direct references to
         * the standin's referent exist (assuming it's a separate object).
         */
        MIGRATION,
        /**
         * The standin class can be generated ahead-of-time. This implies a use
         * of a separate compiler to generate the standin class. Note that this
         * isn't of much use at runtime unless the standin actually <i>was</i>
         * generated ahead of time, but the presence of this technique means
         * that it's at least worth checking (and may also be of use to an
         * ahead-of-time standin generator to know what to generate).
         */
        AOT_GENERATABLE,
        /**
         * The standin class can be generated at runtime via generating its Java
         * bytecode. This is only of use when running on a platform that uses
         * Java bytecode.
         */
        JIT_GENERATABLE,
        /**
         * The standin can be generated at runtime, even when running on a
         * platform that does not use Java bytecode. Typically, this would imply
         * the use of reflection or proxy libraries, and is often very slow.
         */
        NON_JVM_GENERATABLE;
    }
}
