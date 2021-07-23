package xyz.acygn.mokapot.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javassist.util.proxy.MethodHandler;

/**
 * An interface for classes that can serve as an invocation handler for proxies,
 * no matter how the proxies are created. Currently, this requires the class to
 * work for both regular Java proxies and for Javassist proxies, but other sorts
 * of proxy may be added in the future.
 * <p>
 * The only method that needs to be implemented is the <code>invoke</code>
 * method of the Java standard library's <code>InovcationHandler</code>; other
 * required methods will be implemented via default methods in this class.
 *
 * @author Alex Smith
 */
public interface UniversalInvocationHandler
        extends InvocationHandler, MethodHandler {

    /**
     * Handles a method being called on the standin. This version of the method
     * uses the API used by the Javassist library (and its
     * <code>MethodHandler</code> interface), and simply delegates to the "main"
     * version of the method (that's missing the <code>nextMethod</code>
     * parameter).
     *
     * @param generatedStandin The standin on which the method was called.
     * Unused (because such a standin is defined by its handler, i.e.
     * <code>this</code>, which we already know).
     * @param method The method that was called.
     * @param nextMethod The method that would have been called in the absence
     * of a handler. Unused.
     * @param methodParams The parameters that were used in the method call.
     * @return The return value of the method.
     * @throws Throwable If the method throws an exception, this method throws
     * the same exception
     */
    @Override
    public default Object invoke(Object generatedStandin, Method method,
            Method nextMethod, Object[] methodParams) throws Throwable {
        return invoke(generatedStandin, method, methodParams);
    }
}
