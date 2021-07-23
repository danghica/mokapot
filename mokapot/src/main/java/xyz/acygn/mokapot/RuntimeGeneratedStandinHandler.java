package xyz.acygn.mokapot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.methodCode;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Authorisation;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.util.StringifyUtils;
import xyz.acygn.mokapot.util.UniversalInvocationHandler;

/**
 * Generic implementation of a composition-based standin that inherits from the
 * class being stood in for. The standin itself is a proxy class generated at
 * runtime; however, all its methods are implemented via delegating them to the
 * standin handler, and likewise, all its state is stored in or via the standin
 * handler.
 * <p>
 * This is used for "emergency" generation of standins that are usable as
 * inheritance-based standins, on platforms which don't run Java bytecode (and
 * thus don't allow the use of <code>StandinGenerator</code>. Instead, a
 * proxying library is used to pass invocations to this class, which in turn
 * invokes them on an external referent.
 * <p>
 * This is also used in situations where visibility requirements mean that
 * operations on an object will necessarily require the use of reflection (in
 * which case a direct implementation in terms of bytecode would fail to
 * verify).
 * <p>
 * This technique is inefficient, due to the need to use three separate objects
 * (one of which has a large number of unused fields), and due to the need to
 * use reflection for every operation, but has the advantage that it can easily
 * be adapted for use with any existing proxying library.
 *
 * @author Alex Smith
 * @param <T> The type of the standin's referent.
 */
class RuntimeGeneratedStandinHandler<T> extends ReflectiveStandin<T>
        implements UniversalInvocationHandler {

    /**
     * Constructs a new standin handler with the given referent.
     *
     * @param referent The object which will store the standin's data. This must
     * not be the standin itself, as that would cause an infinite loop.
     * @param auth Ignored; used to prevent a potential ambiguity between two
     * constructors with similar arguments.
     */
    RuntimeGeneratedStandinHandler(T referent, Authorisation auth) {
        super(referent);
    }

    /**
     * Constructs a new standin handler with no referent. For this to work, the
     * standin cannot have stored-in-self status (as that would imply that the
     * data were stored in the referent, which does not exist), and as such the
     * storage method cannot be the default and thus must be specified
     * explicitly.
     * <p>
     * Note that this does not prevent the standin being converted into
     * stored-in-self status at some later time (via first undropping the
     * referent).
     *
     * @param actualReferentClass The actual class of objects that the standin
     * stands in for.
     * @param storage The place in which the standin's data is stored.
     */
    RuntimeGeneratedStandinHandler(Class<T> actualReferentClass,
            ForwardingStandinStorage<T> storage) {
        super(actualReferentClass, storage);
    }

    /**
     * Constructs a new standin handler with no referent and unset storage. This
     * is used when pretending we just created an object via use of its
     * constructor. The caller will need to continue via using
     * <code>undropReferent</code> and <code>setStorage</code> in that order.
     *
     * @param actualReferentClass The actual class of objects that the standin
     * stands in for.
     */
    RuntimeGeneratedStandinHandler(Class<T> actualReferentClass) {
        super(actualReferentClass, (ForwardingStandinStorage<T>) null);
    }

    /**
     * Handles a method being called on the standin. This version of the method
     * uses the API used by the core Java Proxy library (and its
     * <code>InvocationHandler</code> interface).
     * <p>
     * There are two possibilities. One is that the method is a method of
     * <code>Standin</code> (i.e. it has a namespacing parameter as its last
     * parameter, either <code>Authorisation</code> or
     * <code>ProxyOrWrapper.Namepsacer</code>); in that case, the method is
     * forwarded to the superclass implementation (i.e. using the methods in
     * <code>ReflectiveStandin</code>). The other is that the method is a method
     * of the class we're standin in for, in which case it's called indirectly
     * via <code>ReflectiveStandin#invoke</code>.
     *
     * @param generatedStandin The standin on which the method was called.
     * Unused (because such a standin is defined by its handler, i.e.
     * <code>this</code>, which we already know).
     * @param method The method that was called.
     * @param methodParams The parameters that were used in the method call.
     * @return The return value of the method.
     * @throws Throwable If the method throws an exception, this method throws
     * the same exception
     */
    @Override
    public Object invoke(Object generatedStandin, Method method,
            Object[] methodParams) throws Throwable {
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> lastParamType = (paramTypes.length == 0 ? null
                : paramTypes[paramTypes.length - 1]);

        if (ProxyOrWrapper.Namespacer.class.equals(lastParamType)
                || Authorisation.class.equals(lastParamType)) {
            /* It's a standin method. */
            try {
                return method.invoke(this, methodParams);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            } catch (ClassCastException | IllegalAccessException | IllegalArgumentException ex) {
                throw new DistributedError(ex, "invoke of " + method);
            }
        } else {
            /* It's a method of the original class. */
            long code = methodCode(
                    method, getReferentClass(null));

            StandinStorage<T> s = getStorage(UNRESTRICTED);

            if (s instanceof ForwardingStandinStorage) {
                return ((ForwardingStandinStorage) s)
                        .forwardMethodCall(code, methodParams);
            }

            return invoke(code, methodParams, UNRESTRICTED);
        }
    }

    /**
     * Returns a string representation of this standin handler.
     *
     * @return A string describing the referent (if any) and its actual type.
     */
    @Override
    public String toString() {
        return "standin handler for referent "
                + getReferentClass(null).getName() + "{"
                + StringifyUtils.toString(getReferent(UNRESTRICTED)) + "}";
    }
}
