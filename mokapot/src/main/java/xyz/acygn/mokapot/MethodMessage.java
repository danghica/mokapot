package xyz.acygn.mokapot;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isStatic;
import java.time.Duration;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.ClassKnowledge.methodCode;
import static xyz.acygn.mokapot.LengthIndependent.getActualClassInternal;
import xyz.acygn.mokapot.wireformat.MethodCodes;

/**
 * A message which tells the recipient system to run a specified method. This
 * can be used both for static methods, and for object methods. The parameters
 * should not have been explicitly marshalled; they'll be marshalled along with
 * the message as a whole.
 * <p>
 * Classes are identified via <code>Class</code> objects (which will presumably
 * be serialised in the standard Java way when the MethodMessage is marshalled).
 * It is advised to ensure that the sender and recipient are using identical
 * classpaths, in order to avoid a misunderstanding between the two systems
 * about what a class is called.
 * <p>
 * Note that in order to allow changes to the details of classes to be made,
 * methods are identified not via <code>Method</code> objects, but via their
 * name, parameters and declaring class.
 *
 * @author Alex Smith
 */
class MethodMessage extends SynchronousMessage<Object> {

    /**
     * In the case of a static method, the class that declares that method. This
     * is null when calling an instance method.
     */
    private final Class<?> declaringClass;

    /**
     * The method's code. This is a number that, given an object's actual class
     * (or the declaring class of a static method), uniquely identifies a method
     * that can be called on that object.
     *
     * @see MethodCodes
     */
    private final long methodCode;

    /**
     * The arguments to the method, including <code>this</code> if the method is
     * an instance method. With instance methods, the object to invoke on is
     * treated as the first argument.
     */
    private final Object[] arguments;

    /**
     * Constructs a MethodMessage that invokes an instance method on the remote
     * system via use of a method code. The method will be run synchronously on
     * the current thread.
     *
     * @param methodCode The code of the method to invoke.
     * @param arguments The object to invoke on, followed by the remaining
     * (non-<code>this</code>) arguments to the method.
     */
    MethodMessage(long methodCode, Object... arguments) {
        this.methodCode = methodCode;
        this.arguments = arguments;
        this.declaringClass = null;
    }

    /**
     * Constructs a MethodMessage that invokes a method on the remote system.
     * The method will be run synchronously on the current thread. (This method
     * is typically called by the communicator itself, immediately before
     * sending the message.)
     *
     * @param method The method to invoke.
     * @param arguments If this is an instance method, he object to invoke on,
     * followed by the remaining (non-<code>this</code>) arguments to the
     * method; for static method, this is simply a list of arguments.
     */
    MethodMessage(Method method, Object... arguments) {
        if (isStatic(method.getModifiers())) {
            this.declaringClass = method.getDeclaringClass();
            this.methodCode = methodCode(method, declaringClass);
        } else {
            this.declaringClass = null;
            this.methodCode = methodCode(method, getActualClassInternal(arguments[0]));
        }
        this.arguments = arguments;
    }

    /**
     * Invokes the method, and sends the results back to the original system via
     * the given communicator.
     *
     * @return The return value of the method.
     * @throws Throwable If an exception occurs running the method, this throws
     * that exception.
     */
    @Override
    public Object calculateReply() throws Throwable {
        // TODO: If the object isn't here, tell the caller it's migrated?
        // TODO: If the location manager is tight, migrate?

        MethodHandle m = getMethod();
        return m.invokeWithArguments(arguments);
    }

    /**
     * Gets the <code>MethodHandle</code> object corresponding to the method
     * that this message is to invoke.
     *
     * @return A MethodHandle object.
     * @throws NoSuchMethodException If an incorrect method code is stored in
     * this <code>MethodMessage</code> object (for example, because it was
     * generated on a system with a different classpath and the version of the
     * class there had more methods)
     */
    private MethodHandle getMethod() throws NoSuchMethodException {
        Class<?> codeClass = declaringClass;
        if (codeClass == null) {
            codeClass = getActualClassInternal(arguments[0]);
        }

        return knowledgeForClass(codeClass).getMethodByCode(methodCode);
    }

    /**
     * Produces a human-readable string describing this method message.
     *
     * @return A human-readable version of the method message.
     */
    @Override
    public String toString() {
        String methodToString = null;

        Class<?> codeClass = declaringClass;
        if (codeClass == null) {
            codeClass = getActualClassInternal(arguments[0]);
        }
        ClassKnowledge<?> codeClassKnowledge
                = knowledgeForClass(codeClass);

        for (Method m : codeClassKnowledge.getMethods()) {
            if (methodCode == methodCode(m, codeClass)) {
                methodToString = m.getName();
            }
        }
        if (methodToString == null) {
            methodToString = "{unknown method " + methodCode
                    + " of " + codeClass + "}";
        }

        methodToString += "(";
        String sep = "";
        boolean skip = declaringClass == null;
        for (Object argument : arguments) {
            if (!skip) {
                methodToString += sep + DistributedMessage.safeStringify(argument);
                sep = ", ";
            }
            skip = false;
        }
        methodToString += ")";
        return (declaringClass != null ? declaringClass
                : DistributedMessage.safeStringify(arguments[0]))
                + "." + methodToString;

    }

    @Override
    public Duration periodic() {
        return null;
    }
}
