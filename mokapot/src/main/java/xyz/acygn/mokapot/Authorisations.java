package xyz.acygn.mokapot;

import java.lang.reflect.Field;
import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import xyz.acygn.mokapot.skeletons.Authorisation;

/**
 * A class holding constant pre-initialised <code>Authorisation</code> objects.
 * This is used by the core of the distributed system when it needs to give
 * standin code an appropriate capability so that it can carry out an operation
 * (e.g. performing operations that may violate the invariants &ndash; which it
 * doesn't know &ndash; or assigning to <code>final</code> fields).
 * <p>
 * This class also contains the actual objects that implement the capabilities
 * (and thus violate the normal laws of Java), in the case where they're
 *
 * @author Alex Smith
 */
class Authorisations {

    /**
     * Default object creator. Used for authorisations that allow the
     * instantiation of objects without calling their constructor. Also used to
     * create Authorisation objects themselves (as they can't be created the
     * usual way).
     */
    static final Objenesis OBJECT_CREATOR;

    /**
     * Unrestricted authorisation. Can be used by any class in this package to
     * perform an arbitrary operation that would otherwise be impermissible.
     * <p>
     * TODO: We don't need this much power in most cases, and it'd be kind-of
     * bad if this leaked, so we at least need some sort of anti-leak protection
     * before this is usable as a security mechanism. (Right now, it's more
     * being used as a placeholder for a security mechanism, as all the nodes
     * trust each other anyway.)
     */
    static final Authorisation UNRESTRICTED;

    static {
        try {
            OBJECT_CREATOR = doPrivileged(
                    (PrivilegedAction<Objenesis>) ObjenesisStd::new);
            UNRESTRICTED = doPrivileged(
                    (PrivilegedAction<Authorisation>) () -> OBJECT_CREATOR.newInstance(Authorisation.class));
            Iterable<Field> authFields
                    = knowledgeForActualClass(UNRESTRICTED)
                            .getInstanceFieldList();
            for (Field f : authFields) {
                switch (f.getName()) {
                    case "classInstantiator":
                        f.set(UNRESTRICTED, new Authorisation.ClassInstantiator() {
                            @Override
                            public <T> T instantiate(Class<T> ofClass) {
                                return doPrivileged(
                                        (PrivilegedAction<T>) () -> OBJECT_CREATOR.newInstance(ofClass));
                            }
                        });
                        break;
                    case "objectCloner":
                        f.set(UNRESTRICTED, (Authorisation.ObjectCloner) ClassKnowledge::cloneObject);
                        break;
                    default:
                        throw new NoSuchFieldException(
                                "Don't know how to gain capability " + f);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Inaccessible constructor. This class exists only as a namespace to hold
     * constants and is not meant to be instantiated.
     */
    private Authorisations() {
    }
}
