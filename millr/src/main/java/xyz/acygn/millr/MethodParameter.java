/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import xyz.acygn.millr.messages.ExceptionWrapper;
import xyz.acygn.millr.messages.NoSuchClassException;

/**
 * @author thomas
 */
public class MethodParameter extends ClassParameter {

    public String methodName;
    public String methodDesc;
    public String methodSignature;
    public boolean isInterface;
    public int methodAccess;
    String[] Exceptions;
    private ClassParameter cp;
    public String methodDescCalled = null;

    public MethodParameter(ClassParameter cp, int methodAccess, String methodName, String methodDesc, String methodSignature,
                           String[] Exceptions) {
        super(cp.classVersion, cp.classAccess, cp.className, cp.classSignature, cp.classSuperName, cp.classInterfaces);
        this.cp = cp;
        this.methodAccess = methodAccess;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodSignature = methodSignature;
        this.isInterface = ((this.classAccess & Opcodes.ACC_INTERFACE) != 0);
        this.Exceptions = Exceptions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(cp.toString());
        sb.append("method Acess" + methodAccess + "\n");
        sb.append("methodName" + methodName + "\n");
        sb.append("methodDesc" + methodDesc + "\n");
        sb.append("methodSignature" + methodSignature + "\n");
        sb.append("Exceptions" + Exceptions == null ? "null" : Arrays.toString(Exceptions));
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MethodParameter)) {
            return false;
        } else {
            final MethodParameter objMethod = (MethodParameter) obj;
            if (super.equals(obj) && (this.classAccess == objMethod.classAccess) && myEquals(objMethod.methodName, this.methodName) && myEquals(objMethod.methodDesc, this.methodDesc)
                    && (Arrays.deepEquals(this.Exceptions, objMethod.Exceptions)) && myEquals(this.methodDescCalled, objMethod.methodDescCalled)) {
                return true;
            }
            return false;
        }
    }

    @Deprecated
    public static MethodParameter getCoreMethodParameter(final String owner, final String methodName, final String methodDesc) {
        ClassParameter cp = new ClassParameter(-1, -1, owner, null, null, null);
        return new MethodParameter(cp, -1, methodName, methodDesc, null, null);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Integer.hashCode(classAccess) + methodName.hashCode() + methodDesc.hashCode() + Arrays.hashCode(Exceptions);
    }

    public static MethodParameter getMethod(String owner, String methodName, String methodDesc) {
        ClassParameter cp = ClassParameter.getClassParameter(owner);
        if (cp.isConcreteClass()) {
            return MethodParameter.getConcreteMethod(owner, methodName, methodDesc);
        } else if (cp.isInterface()) {
            return MethodParameter.getMethodAbstract(owner, methodName, methodDesc);
        }
        //else it must be an abstract class.
        else {
            try {
                return MethodParameter.getConcreteMethod(owner, methodName, methodDesc);
            } catch (RuntimeException ex) {
                return MethodParameter.getMethodAbstract(owner, methodName, methodDesc);
            }
        }
    }


    private static MethodParameter getConcreteMethod(final String owner, final String methodName, final String methodDesc) {
        if (owner.startsWith("[")) {
            ClassParameter cp = ClassParameter.getClassParameter(owner);
            MethodParameter mp;
            if (methodName.equals("clone")) {
                mp = MethodParameter.getMethodOrNull("java/lang/Object", "clone", "()Ljava/lang/Object;", true);
                mp.methodDesc = methodDesc;
            } else {
                mp = MethodParameter.getMethodOrNull("java/lang/Object", methodName, methodDesc, true);
            }
            return new MethodParameter(cp, mp.methodAccess, mp.methodName, mp.methodDesc, mp.methodSignature, mp.Exceptions);
        }
        MethodParameter mp = getMethodOrNull(owner, methodName, methodDesc, true);
        if (mp == null) {
            ClassParameter cp = null;
            try {
                cp = ClassParameter.getClassParameter(owner);
                if (cp==null){
                    throw new Throwable("ClassParameter not found");
                }
            } catch (Throwable t) {
                throw new RuntimeException("can't find method whose owner is :" + owner + " name is : " + methodName + " and desc is : " + methodDesc
                        + " \n because class not found" + t.getMessage());
            }
            throw new RuntimeException("can't find method whose owner is :" + owner + " name is : " + methodName + " and desc is : " + methodDesc + "\n"
                    + " but Class Found \n" + cp.toString());
        }
        return mp;
    }


    //    public static MethodParameter getSimpleMethodParameter(final String owner, final String methodName, final String methodDesc) {
//        ClassParameter cp = new ClassParameter(0, 0, owner, null, null, null);
//        return new MethodParameter(cp, 0, methodName, methodDesc, null, null);
//    }
    static private MethodParameter getMethodAbstract(final String owner, final String methodName, final String methodDesc) throws NoSuchClassException {
        if (owner.startsWith("[")) {
            ClassParameter cp = ClassParameter.getClassParameter(owner);
            MethodParameter mp;
            if (methodName.equals("clone")) {
                mp = MethodParameter.getMethodOrNull("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
                mp.methodDesc = methodDesc;
            } else {
                mp = MethodParameter.getMethodOrNull("java/lang/Object", methodName, methodDesc, false);
            }
            return new MethodParameter(cp, mp.methodAccess, mp.methodName, mp.methodDesc, mp.methodSignature, mp.Exceptions);
        }
        MethodParameter mp = getMethodOrNull(owner, methodName, methodDesc, false);
        if (mp == null) {
            ClassParameter cp;
            try {
                cp = ClassParameter.getClassParameter(owner);
            } catch (Throwable t) {
                throw new RuntimeException("can't find method whose owner is :" + owner + " name is : " + methodName + " and desc is : " + methodDesc
                        + " \n because class not found" + t.getMessage());
            }
            throw new RuntimeException("can't find method whose owner is :" + owner + " name is : " + methodName + " and desc is : " + methodDesc + "\n"
                    + " but Class Found \n" + cp.toString());

        }
        return mp;
    }

    private static MethodParameter getMethodOrNull(final String owner, final String methodName, final String methodDesc,
                                                   boolean isConcrete) throws NoSuchClassException {
        Optional<ClassDataBase.ClassData> cd = ClassDataBase.getClassDataOrNull(owner);
        if (((Optional) cd).isPresent()) {
            Optional<MethodParameter> omp = cd.get().getAllMethods().stream().filter(e ->
                            (VisibilityTools.isSignaturePolymorphic(e))?
                                    ((e.methodName.equals(methodName)) && isDescVarargsCompatible(methodDesc, e.methodDesc)) :
                                    (e.methodName.equals(methodName) && methodDesc.equals(e.methodDesc))).
                    findAny();
            if (omp.isPresent() && ((isConcrete) ? ((omp.get().methodAccess & Opcodes.ACC_ABSTRACT) == 0) : true)) {
                return omp.get();
            }
        }
        ClassReader cr = Mill.getClassReader(owner);
        class ClassVisitorGetMethodInfo extends ClassVisitorGetInfo {

            MethodParameter mp;
            ClassParameter cp;

            public ClassVisitorGetMethodInfo(int api) {
                super(api);
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                cp = new ClassParameter(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodParameter mptemp = new MethodParameter(cp, access, name, desc, signature, exceptions);
                boolean isDescCompatible = VisibilityTools.isSignaturePolymorphic(mptemp)? isDescVarargsCompatible(methodDesc, desc) :
                        desc.equals(methodDesc);
                if (name.equals(methodName) && isDescCompatible && ((isConcrete) ? ((access & Opcodes.ACC_ABSTRACT) == 0) : true)) {
                    mp = mptemp;
                }
                return null;
            }
        }
        ClassVisitorGetMethodInfo cv = new ClassVisitorGetMethodInfo(Opcodes.ASM5);
        cr.accept(cv, 0);
        if (cv.mp != null) {
            return cv.mp;
        }
        for (String inter : cv.cp.classInterfaces) {
            MethodParameter mp;
            if (inter.equals(cv.cp.className)) {
                mp = null;
            } else {
                mp = getMethodOrNull(inter, methodName, methodDesc, isConcrete);
            }
            if (mp != null) {
                return mp;
            }
        }
        if (cv.cp.classSuperName == null) {
            return null;
        } else {
            return getMethodOrNull(cv.cp.classSuperName, methodName, methodDesc, isConcrete);
        }
    }

//    private static MethodParameter getMethodOrNullWithVarargs(final String owner, final String methodName, final String methodDesc) throws NoSuchClassException {
//        ClassReader cr;
//        cr = Mill.getInstance().getClassReader(owner);
//        ClassParameter classParam = ClassParameter.getClassParameter(owner);
//
//        class ClassVisitorGetMethodInfo extends ClassVisitorGetInfo {
//
//            MethodParameter mp;
//            ClassParameter cp;
//
//            public ClassVisitorGetMethodInfo(int api) {
//                super(api);
//            }
//
//            @Override
//            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//                if (superName != null) {
//                    cp = new ClassParameter(version, access, name, signature, superName, interfaces);
//                } else {
//                    cp = new ClassParameter(version, access, name, signature, "java/lang/Object", interfaces);
//                }
//            }
//
//
//            @Override
//            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//                if ((access & Opcodes.ACC_VARARGS) != 0) {
//                    try {
//                        if (name.equals(methodName) && isDescVarargsCompatible(methodDesc, desc)) {
//                            //         System.out.println("methodFound!");
//                            mp = new MethodParameter(classParam, access, name, desc, signature, exceptions);
//                        }
//                    } catch (NoSuchClassException ex) {
//                        throw new ExceptionWrapper(ex);
//                    }
//                }
//                MethodVisitor mv = null;
//                return mv;
//            }
//        }
//        ClassVisitorGetMethodInfo cv = new ClassVisitorGetMethodInfo(Opcodes.ASM5);
//        try {
//            cr.accept(cv, 0);
//        } catch (ExceptionWrapper ex) {
//            throw (NoSuchClassException) ex.getOriginalException();
//        }
//        if (cv.mp != null) {
//            return cv.mp;
//        }
//        for (String inter : cv.cp.classInterfaces) {
//            MethodParameter mp = getMethodOrNull(inter, methodName, methodDesc);
//            if (mp != null) {
//                return mp;
//            }
//        }
//        if (cv.cp.classSuperName == null || cv.cp.className.equals("java/lang/Object")) {
//            return null;
//        } else {
//            return getMethodOrNull(cv.cp.classSuperName, methodName, methodDesc);
//        }
//    }

    private static boolean isDescVarargsCompatible(String descInvocation, String descMethod) throws NoSuchClassException {
//     If the member is a variable arity method with arity n, then for all i (1 ≤ i ≤ n-1), the i'th argument of the method invocation
//     is potentially compatible with the type of the i'th parameter of the method; and, where the nth parameter of the method has type T[], 
        Type[] argsTypeMethod = Type.getArgumentTypes(descMethod);
        if (!TypeUtil.isArray(argsTypeMethod[argsTypeMethod.length - 1])) {
            throw new IllegalArgumentException("should be called on a method whose description is varags compatible: its last arguement type must be an array: " + descMethod);
        }
        Type[] argsTypeMethodInvocation = Type.getArgumentTypes(descInvocation);
        if (argsTypeMethodInvocation.length < argsTypeMethod.length - 1) {
            return false;
        }
        for (int i = 0; i < argsTypeMethod.length - 1; i++) {
            if (!TypeUtil.isSubType(argsTypeMethodInvocation[i], argsTypeMethod[i])) {
                return false;
            }
        }
//      one of the following is true:
//         The arity of the method invocation is equal to n-1.
        if (argsTypeMethodInvocation.length == argsTypeMethod.length - 1) {
            return true;
        }
//     The arity of the method invocation is equal to n, and the nth argument of the method invocation is potentially compatible with either T or T[].
        int n = argsTypeMethod.length;
        if (argsTypeMethod.length == argsTypeMethodInvocation.length) {
            if (TypeUtil.isSubType(argsTypeMethodInvocation[n - 1], argsTypeMethod[n - 1])) {
                return true;
            }
            if (TypeUtil.isSubType(argsTypeMethodInvocation[n - 1], Type.getType(argsTypeMethod[n - 1].getDescriptor().substring(1)))) {
                return true;
            }
            return false;
        }
//      The arity of the method invocation is m, where m > n, and for all i (n ≤ i ≤ m), the i'th argument of the method invocation is potentially compatible with T.
        Type T = Type.getType(argsTypeMethod[n - 1].getDescriptor().substring(1));
        for (int i = argsTypeMethod.length - 1; i < argsTypeMethodInvocation.length; i++) {
            if (!TypeUtil.isSubType(argsTypeMethod[n - 1], T)) {
                return false;
            }
        }
        return true;
    }


    public boolean areOverrideCompatible(MethodParameter mp) {
        return areOverrideCompatible(mp.methodName, mp.methodDesc);
    }

    public boolean areOverrideCompatible(String methodName, String methodDesc) {
        return this.methodName.equals(methodName) && Arrays.equals(Type.getArgumentTypes(methodDesc), Type.getArgumentTypes(this.methodDesc));
    }


    public String getCompatibleCode() {
        return String.valueOf((Arrays.hashCode(Type.getArgumentTypes(methodDesc)) + 13 * methodName.hashCode()));
    }

}
