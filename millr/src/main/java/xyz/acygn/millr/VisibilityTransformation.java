/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;


import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.MethodTransformationFailedException;
import xyz.acygn.millr.messages.NoSuchClassException;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;

/**
 * <p>
 * Millr will need to change visibility modifier on classes and methods in order
 * to allow them to be extended/overridden. A simple way would be to change all
 * package-private and private methods/classes to protected. However, as we do
 * not want to degrade the "security" of the program more than needed, if would
 * be preferable to check precisely which methods needs to be changed.
 * <p>
 * If a class C of a package A extends a class D from a package B, then all
 * methods that C overrides that are inherited from D should be change in D from
 * package-private to protected, and the visibility of D should change
 * accordingly. If a private method is called on a instance other than "this",
 * then this one should become protected. This can happens in 2 different cases:
 * 1) If o_1 is an object of Class A, o_1 can call a private method on o_2, if
 * o_2 is itself of class A. 2) Outer/inner classes can refer to private method
 * of one another. In these cases, the private method would have been called
 * using invokespecial (if the private methods were non-static). This will need
 * to be changed to invokevirtual. In cases where the class is non-copiable, a
 * standin would be generate. Therefore, we need to ensure that the class is non
 * final.
 * <p>
 * In the case of api-classes, we are not sure yet how to handle this case.
 * <p>
 * <p>
 * * @author thomasc
 */
public class VisibilityTransformation extends Transformation {

    @Override
    String getNameOfTheTransformation() {
        return " visibility Transformation ";
    }

    VisibilityTransformation(Collection<ClassReader> classReaders) {
        super(classReaders);
        methodToProtected = new HashMap<>();
    }

    final Map<String, List<MethodParameter>> methodToProtected;


    private void addMethodToProtected(MethodParameter mp) {
        synchronized (methodToProtected) {
            if (methodToProtected.containsKey(mp.className)) {
                methodToProtected.get(mp.className).add(mp);
            } else {
                methodToProtected.put(mp.className, new ArrayList<>(Collections.singletonList(mp)));
            }
        }
    }


    public Map<String, List<MethodParameter>> getMethodToChangeToProtected() {
        return methodToProtected;
    }


    @Override
    public Instance getNewInstance(ClassReader cr) {
        return new VisibilityTransformationInstance(cr);
    }

    class VisibilityTransformationInstance extends Instance {

        final ClassNode cv;
        final ClassWriter cw;

        @Override
        ClassNode getClassTransformer() {
            return cv;
        }


        @Override
        ClassWriter getClassWriter() {
            return cw;
        }

        @Override
        ClassVisitor getClassVisitorChecker() {
            return null;
        }

        public VisibilityTransformationInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cv = new VisibilityVisitor(Opcodes.ASM6, cr.getClassName());
        }

        boolean isCopiable = false;

        private boolean isPackagePrivate(int access) {
            if ((access & Opcodes.ACC_PUBLIC) != 0) {
                return false;
            } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
                return false;
            } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
                return false;
            }
            return true;
        }


        class VisibilityVisitor extends ClassNode {

            private String className;


            public VisibilityVisitor(int api, String className) {
                super(api);
                this.className = className;
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                // In cases where the class is non-copiable, a standin would be generate. Therefore, we need to ensure that the class is non final.
                //We create a custom classloader to load the bytecode and forms a class.
                if ((access & Opcodes.ACC_INTERFACE) == 0) {
//                    class tempClassLoader extends ClassLoader {
//
//                        public tempClassLoader(ClassLoader classLoader) {
//                            super(classLoader);
//                        }
//
//                        public Class<?> defineClassPublic(String name, byte[] data, int off, int len) {
//                            return this.defineClass(name, data, off, len);
//                        }
//                    }
                    Class<?> clazz;
                    try {
                        clazz = this.getClass().getClassLoader().loadClass(className.replace("/", "."));
                    } catch (ClassNotFoundException t) {
//                        try {
//
//                            ClassWriter tempClassWriter = InputOutput.fromReaderToWriter(Mill.getInstance().getClassReader(nameClass));
//                            clazz = (new tempClassLoader(VisibilityTransformation.class.getClassLoader())).defineClassPublic(nameClass.replace("/", "."), tempClassWriter.toByteArray(), 0, tempClassWriter.toByteArray().length);
//                        } catch (Throwable e) {
                        throw new NoSuchClassException("impossible to load " + className + " " + t.getMessage());

                        //     }
                    }
                    try {
                        ObjectWireFormat.Technique technique;
                        ObjectWireFormat owf = new ObjectWireFormat(clazz);
                        technique = owf.getTechnique();
                        // technique == long_reference means that the object is not copiable
                        isCopiable = !technique.equals(ObjectWireFormat.Technique.LONG_REFERENCE);
                        if (!isCopiable && (cr.getAccess() & Opcodes.ACC_FINAL) != 0) {
                            super.visit(version, access & ~Opcodes.ACC_FINAL, name, signature, superName, interfaces);
                            return;
                        }
                    } catch (Throwable t) {
                        throw t;
                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);

            }


            @Override
            public void visitEnd() {
                if (isCopiable) {
                    return;
                }

                // If a class C of a package A extends a class D from a package B, then all methods that C  inherit
                //from D should be change in D from package-private to protected.

                ClassDataBase.ClassData cd = ClassDataBase.getClassData(className);
                if (cd.cp.isInterface()) {
                    return;
                }
                // boolean b = false;
                cd.getAllMethods().parallelStream().filter(e ->
                        !Objects.equals(ClassDataBase.getPackageInternal(e.className), (cd.getPackage()))).
                        filter(e -> isPackagePrivate(e.methodAccess)).peek(e -> {
                            if (TypeUtil.isNotProject(e.className)) {
                                //      b = true;
                                Mill.getInstance().addReasonForUnsafe(className,
                                        new Reason.methodPackagePrivateOutsideProject(className, e.methodName, e.methodDesc, e.className));
                            }
                        }
                ).forEach(VisibilityTransformation.this::addMethodToProtected);
                this.methods.parallelStream().forEach(mn -> {
                    try {
                        replaceFieldAccess(mn, className);
                    } catch (AnalyzerException e) {
                        throw new MethodTransformationFailedException(e, className, mn.name, mn.desc);
                    }
                });

            }

            // If a private method is called on a instance other than "this", then this one should become protected.
            // This can happens in 2 different cases: 1) If o_1 is an object of Class A, o_1 can call a
            // private method on o_2, if o_2 is itself of class A. 2
            // Outer/inner classes can refer to private method of one another.
            public void replaceFieldAccess(MethodNode mn, String owner) throws AnalyzerException {
                Analyzer<SourceValue> a = new Analyzer(new SourceInterpreter());
                a.analyze(owner, mn);
                Frame<SourceValue>[] frames = a.getFrames();
                InsnList listInstru = mn.instructions;
                AbstractInsnNode node = listInstru.getFirst();
                int i = 0;
                while (node != null) {
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode methodNode = (MethodInsnNode) node;
                        if (methodNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                            if (!methodNode.name.equals("<init>")) {
                                SourceValue ownerOfMethod = frames[i].getStack(0);
                                if (!valueIsThis(ownerOfMethod)) {
                                    MethodParameter mp = MethodParameter.getMethod(methodNode.owner, methodNode.name, methodNode.desc);
                                    if ((mp.methodAccess & Opcodes.ACC_PRIVATE) != 0) {
                                        if (TypeUtil.isNotProject(methodNode.owner)) {
                                            //This case should never happens.
                                            MessageUtil.error(new Exception("Call to a private method belonging to " + methodNode.owner + "+, this " +
                                                    "one lies outside the project")).report().resume();
                                            System.out.println("name " + methodNode.name);
                                            System.out.println("desc" + methodNode.desc);
                                        } else {
                                            addMethodToProtected(mp);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    i++;
                    node = node.getNext();
                }
            }

            private boolean valueIsThis(SourceValue source) {
                return source.insns.stream().allMatch(this::loadingThisPredicate);
            }


            private boolean loadingThisPredicate(AbstractInsnNode node) {
                if (node instanceof VarInsnNode) {
                    VarInsnNode varNode = (VarInsnNode) node;
                    return varNode.var == 0;
                }
                return false;
            }


//            @Override
//            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
////                if (!name.equals("<init>") && !name.equals("<clinit>")) {
////                    List<MethodParameter> overriden = VisibilityTools.doesOverride(nameClass, name, desc);
////                    overriden.stream().filter(e -> extendedClassesOuterPackage.contains(e.className)).filter(e ->
////                            isPackagePrivate(e.methodAccess)).forEach(e -> {
////                        if (TypeUtil.isNotProject(e.className)) {
////                            MessageUtil.warning("Method   " + name + "whose desc is " + desc + " from " + nameClass +
////                                    "overrides a method that is API and has not the right visibility").emit();
////                        } else {
////                            addMethodToProtected(e);
////                        }
////                    });
////                }
//                return new MethodVisitor(Opcodes.ASM6, super.visitMethod(access, name, desc, signature, exceptions)) {
//
//                    // If a private method is called on a instance other than "this", then this one should become protected.
//                    // This can happens in 2 different cases: 1) If o_1 is an object of Class A, o_1 can call a
//                    // private method on o_2, if o_2 is itself of class A. 2
//                    // Outer/inner classes can refer to private method of one another.
//                    @Override
//                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//                        if (opcode == Opcodes.INVOKESPECIAL && !name.equals("<init>") && !owner.equals(cr.getClassName())) {
//                            MethodParameter mp = MethodParameter.getMethod(owner, name, desc);
//                            if ((mp.methodAccess & Opcodes.ACC_PRIVATE) != 0) {
//                                if (TypeUtil.isNotProject(owner)) {
//                                    MessageUtil.warning("Method   " + name + "whose desc is " + desc + " from " + nameClass +
//                                            " calls a private method whose visibility cannot be changed").emit();
//                                    super.visitMethodInsn(opcode, owner, name, desc);
//                                } else {
//                                    addMethodToProtected(mp);
//                                    //In these cases, the private method would have been called using invokespecial (if the private methods were non-static).
//                                    //This will need to be changed to invokevirtual.
//                                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, desc, itf);
//                                }
//                            } else {
//                                super.visitMethodInsn(opcode, owner, name, desc, itf);
//                            }
//                        } else {
//                            super.visitMethodInsn(opcode, owner, name, desc, itf);
//                        }
//
//                    }
//                };
//            }
        }


        @Override
        void classTransformerToClassWriter() {
            getClassTransformer().accept(getClassWriter());
        }


    }


}
