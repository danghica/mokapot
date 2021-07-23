package xyz.acygn.millr.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import org.objectweb.asm.Type;

/**
 * Information sufficient to locate a method at runtime and generate code that
 * interacts with it. This is designed to work even if the method in question
 * exists in a class that isn't currently loaded or that hasn't been written
 * yet. This is a POJO that can describe a method even if it doesn't exist yet.
 *
 * @author Alex Smith
 */
class MethodReference {

    /**
     * The name of the class that defines the version of the method for use with
     * an <code>invokespecial</code> opcode.
     */
    private final String ofClass;

    /**
     * The name of the class that originally declared the method, used with the
     * <code>invokevirtual</code> and <code>invokeinterface</code> opcodes.
     */
    private final String declaringClass;

    /**
     * Stores whether <code>ofClass</code> is actually an interface. (This can
     * only happen in the case of a static or default method of an interface,
     * because <code>ofClass</code> has to actually define the code of the
     * method.)
     */
    private final boolean ofClassIsInterface;

    /**
     * Stores whether <code>declaringClass</code> is actually an interface.
     */
    private final boolean declaredInInterface;

    /**
     * The name of the method itself.
     */
    private final String name;

    /**
     * The method's descriptor.
     */
    private final String descriptor;

    /**
     * The method's signature.
     */
    private final String signature;

    /**
     * Gets the class to which the referenced method belongs.
     *
     * @return The class's internal name.
     */
    public String getOfClass() {
        return ofClass;
    }

    /**
     * Gets the name of the referenced method.
     *
     * @return The referenced method's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the descriptor of the referenced method.
     *
     * @return The referenced method's descriptor.
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Gets the signature of the referenced method.
     *
     * @return The referenced method's signature.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Creates a new method reference from its individual components.
     *
     * @param ofClass The class to which the method belongs.
     * @param ofClassIsInterface Whether that class is actually an interface.
     * @param name The method's name.
     * @param descriptor The method's descriptor (which specifies its erased
     * argument types and return values in a compressed way).
     * @param signature The method's signature (which specifies what types of
     * arguments it needs, pre-erasure).
     */
    MethodReference(String ofClass, boolean ofClassIsInterface,
            String name, String descriptor, String signature) {
        this.ofClass = ofClass;
        this.declaringClass = ofClass;
        this.ofClassIsInterface = ofClassIsInterface;
        this.declaredInInterface = ofClassIsInterface;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
    }

    /**
     * Creates a new method reference from a Java reflection API reference to a
     * method or constructor.
     *
     * @param exec The method or constructor to convert from the format used by
     * Java reflection to the format used by Java bytecode.
     * @throws IllegalArgumentException If <code>exec</code> is neither a method
     * nor a constructor
     */
    MethodReference(Executable exec) throws IllegalArgumentException {
        this(exec, exec.getDeclaringClass());
    }

    /**
     * Creates a new method reference from a Java reflection API reference to a
     * method, specifying the version that exists within a specific class. This
     * would normally be used to make calls to superclass methods; in such a
     * case, you want the method in the superclass even if it's overriding a
     * method in one of its superclasses.
     *
     * @param exec The method or constructor to convert from the format used by
     * Java reflection to the format used by Java bytecode.
     * @param ofClass The class via which the method would be called, for the
     * purpose of <code>invokeSpecial</code> instructions.
     * @throws IllegalArgumentException If <code>exec</code> is neither a method
     * nor a constructor
     */
    MethodReference(Executable exec, Class<?> ofClass)
            throws IllegalArgumentException {
        this.ofClass = Type.getInternalName(ofClass);
        this.ofClassIsInterface = ofClass.isInterface();
        this.declaringClass = Type.getInternalName(exec.getDeclaringClass());
        this.declaredInInterface = exec.getDeclaringClass().isInterface();
        if (exec instanceof Method) {
            descriptor = Type.getMethodDescriptor((Method) exec);
            name = exec.getName();
        } else if (exec instanceof Constructor) {
            descriptor = Type.getConstructorDescriptor((Constructor) exec);
            name = "<init>";
        } else {
            throw new IllegalArgumentException("Unknown type of executable");
        }
        signature = null;
    }

    /**
     * Generates a Java bytecode instruction to call the referenced method.
     *
     * @param opcode The way in which this method is invoked (virtual, special,
     * or static); specifying <code>Opcodes.INVOKEVIRTUAL</code> will choose
     * whichever of virtual or interface is appropriate.
     * @param visitor The visitor which will be used to visit the generated
     * bytecode instruction.
     */
    public void generateCallInsn(int opcode, MethodVisitor visitor) {
        int realOpcode = opcode;
        if (declaredInInterface && opcode == INVOKEVIRTUAL) {
            realOpcode = INVOKEINTERFACE;
        }
        visitor.visitMethodInsn(realOpcode,
                realOpcode != INVOKEINTERFACE ? ofClass
                        : declaringClass, name, descriptor,
                realOpcode != INVOKEINTERFACE ? ofClassIsInterface
                        : declaredInInterface);
    }
}
