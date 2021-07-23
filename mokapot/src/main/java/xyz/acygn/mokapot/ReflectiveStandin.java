package xyz.acygn.mokapot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.DistributedMessage.standinStringify;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Authorisation;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.IndirectStandin;
import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Slow but general-purpose standin implementation. Can be used as a standin for
 * any object in cases where using a more specific standin class is impossible
 * or undesirable. It uses the default methods in Standin, which rely on Java's
 * reflection API in order to access information about the object in question.
 *
 * @author Alex Smith
 * @param <T> The actual class of the object we're standing in for.
 */
class ReflectiveStandin<T> extends IndirectStandin<T> {

    /**
     * The class knowledge for the actual class of the wrapped referent.
     */
    private final Class<T> actualReferentClass;

    /**
     * Creates a new composition-based standin wrapping the given referent. It
     * will delegate method calls to the given referent, and the class it's
     * "standing in for" will be the actual class of the given referent. (Note
     * that for the Java generics to type correctly, <code>T</code> must
     * actually be the referent's actual class; Java's type system only enforces
     * that it's the <i>declared</i> class, which is not a strong enough
     * assertion, so the rest has to be checked by hand.)
     *
     * @param referent The referent to wrap.
     */
    @SuppressWarnings("unchecked")
    ReflectiveStandin(T referent) {
        super(referent, UNRESTRICTED);
        actualReferentClass = (Class<T>) knowledgeForActualClass(referent).getAbout();
    }

    /**
     * Creates a new composition-based standin with a dropped referent. A custom
     * standin storage must be provided that explains where the standin's data
     * is stored (as it obviously isn't in the referent); likewise, this cannot
     * imply stored-in-self status.
     *
     * @param actualReferentClass The actual class of referents that the standin
     * will refer to.
     * @param storage Information on where the standin's data is stored.
     */
    ReflectiveStandin(Class<T> actualReferentClass,
            ForwardingStandinStorage<T> storage) {
        super(storage, UNRESTRICTED);
        this.actualReferentClass = actualReferentClass;
    }

    /**
     * Creates a new composition-based standin whose referent is created from a
     * description.
     *
     * @param referentClass The class of the new object to create.
     * @param description The description from which to create the standin.
     * @throws IOException
     */
    ReflectiveStandin(Class<T> referentClass, ReadableDescription description)
            throws IOException {
        super(referentClass, description, UNRESTRICTED);
        this.actualReferentClass = referentClass;
    }

    @Override
    public Class<T> getReferentClass(ProxyOrWrapper.Namespacer dummy) {
        return actualReferentClass;
    }

    /* Note: this method critically relies on the referent <i>not</i> being a
       standin, thus it's unsuitable for pulling up into Standin as a default
       (because a Standin can in general have getReferent() == this). */
    @Override
    public Object invoke(long methodCode, Object[] methodArguments,
            Authorisation auth) throws Throwable {
        auth.verify();
        ClassKnowledge<T> referentKnowledge
                = knowledgeForClass(getReferentClass(null));

        try {
            return referentKnowledge.getMethodByCode(methodCode)
                    .bindTo(getReferent(auth))
                    .invokeWithArguments(methodArguments);
        } catch (NoSuchMethodException | IllegalAccessException
                | IllegalArgumentException ex) {
            throw new DistributedError(ex, "invoking " + methodCode + " on "
                    + standinStringify(this));
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    /**
     * A factory for creating ReflectiveStandins for a given class.
     *
     * @param <T> The class for which the standins are created.
     */
    public static class Factory<T> implements StandinFactory<T> {

        /**
         * The class for which the standins are created. This is stored
         * explicitly to get around Java's type erasure rules.
         */
        final private Class<T> referentClass;

        /**
         * Creates a new standin factory to create reflective standins for a
         * given class.
         *
         * @param referentClass The class for which standins are created.
         */
        public Factory(Class<T> referentClass) {
            Objects.requireNonNull(referentClass);
            this.referentClass = referentClass;
        }

        @Override
        public Standin<T> newFromDescription(ReadableDescription description)
                throws IOException {
            return new ReflectiveStandin<>(referentClass, description);
        }

        @Override
        public Standin<T> wrapObject(T t) {
            return new ReflectiveStandin<>(t);
        }

        @Override
        public Standin<T> standinFromLocationManager(LocationManager<T> lm) {
            return new ReflectiveStandin<>(referentClass,
                    new ForwardingStandinStorage<>(lm));
        }
    }
}
