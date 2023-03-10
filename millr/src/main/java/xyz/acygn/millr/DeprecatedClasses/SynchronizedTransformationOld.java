//package xyz.acygn.millr.DeprecatedClasses;
//
//import java.io.IOException;
//import static java.lang.System.err;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import java.util.stream.Collectors;
//import javafx.util.Pair;
//import org.objectweb.asm.AnnotationVisitor;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
//import org.objectweb.asm.FieldVisitor;
//import org.objectweb.asm.Handle;
//import org.objectweb.asm.Label;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.Type;
//import org.objectweb.asm.TypePath;
//import org.objectweb.asm.commons.AnalyzerAdapter;
//import xyz.acygn.millr.SynchronizedTransformation;
//import xyz.acygn.millr.Transformation;
//import xyz.acygn.millr.TypeUtil;
//
//
///**
// * @deprecated in favor of {@link SynchronizedTransformation}
// * @author thomasc
// */
//@Deprecated
//public class SynchronizedTransformationOld extends Transformation {
//
//    public SynchronizedTransformationOld(List<ClassReader> listClassReader) {
//        super(listClassReader);
//    }
//
////    private ClassWriter fromReaderToWriter(ClassReader cr) {
////        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
////        cr.accept(cw, ClassReader.EXPAND_FRAMES);
////        return cw;
////    }
//    @Override
//    Instance getNewInstance(ClassReader cr) {
//        return new TransformationInstance(cr);
//    }
//
//    class TransformationInstance extends Instance {
//
////        boolean hasSynchronized() {
////            class booleanRef {
////
////                boolean b;
////
////                booleanRef(boolean b) {
////                    this.b = b;
////                }
////            }
////            final booleanRef bool = new booleanRef(false);
////            ClassVisitor classVisitorSynchronized = new ClassVisitor(Opcodes.ASM5) {
////                @Override
////                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
////                    if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
////                        bool.b = true;
////                    }
////                    return super.visitMethod(access, name, desc, signature, exceptions);
////                }
////            };
////            cr.accept(classVisitorSynchronized, ClassReader.SKIP_CODE);
////            return bool.b;
////        }
//        @Override
//        void applyTransformation() {
//            if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
//                cr.accept(cw, 0);
//            } else {
//                cr.accept(cv, ClassReader.EXPAND_FRAMES);
//            }
//        }
//
//        public TransformationInstance(ClassReader cr) {
//            super(cr);
//            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//            cv = new ClassVisitorRunnableTransformation(Opcodes.ASM5, cr.getClassName(), cw);
//
//        }
//
//    }
//
//    class ClassVisitorRunnableTransformation extends ClassVisitor {
//
//        String nameClass;
//
//        public ClassVisitorRunnableTransformation(int api, String nameClass, ClassVisitor classVisitor) {
//            super(Opcodes.ASM5, classVisitor);
//            this.nameClass = nameClass;
//        }
//
//        @Override
//        public MethodVisitor visitMethod(int access, String name, String description, String signature, String[] Exceptions) {
//            System.out.println(name + nameClass);
//            if (name.equals("<init>")) {
//                // We are in the case where the method is a constructor
//                return new SynchronizedMethodVisitor(nameClass, access, name, description, super.visitMethod(access, name, description, signature, Exceptions)) {
//                    @Override
//                    public void visitInsn(int opcode) {
//                        if (opcode == Opcodes.RETURN) {
//                            super.visitVarInsn(Opcodes.ALOAD, 0);
//                            super.visitTypeInsn(Opcodes.NEW, "java/util/concurrent/locks/ReentrantLock");
//                            super.visitInsn(Opcodes.DUP);
//                            super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/concurrent/locks/ReentrantLock", "<init>", "()V", false);
//                            super.visitFieldInsn(Opcodes.PUTFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//                            super.visitInsn(opcode);
//                        } else {
//                            super.visitInsn(opcode);
//                        }
//                    }
//
//                };
//            } else if (((access & Opcodes.ACC_SYNCHRONIZED) != 0) && ((access & Opcodes.ACC_STATIC) == 0)) {
//                MethodVisitor methodVisitor = super.visitMethod(access & ~Opcodes.ACC_SYNCHRONIZED, name, description, signature, Exceptions);
//
//
//                Label l0 = new Label();
//                Label l1 = new Label();
//                Label l2 = new Label();
//                methodVisitor.visitTryCatchBlock(l0, l1, l2, null);
//                methodVisitor.visitLabel(l0);
//                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
//                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V", false);
//                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
//                Type[] inputTypes = Type.getArgumentTypes(description);
//                int locationLocals = 1;
//                for (Type T : inputTypes) {
//                    methodVisitor.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationLocals);
//                    locationLocals += T.getSize();
//                }
//                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, nameClass, name + "millredSynchronized", description, false);
//                methodVisitor.visitLabel(l1);
//                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
//                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V", false);
//                Label l3 = new Label();
//                methodVisitor.visitJumpInsn(Opcodes.GOTO, l3);
//                methodVisitor.visitLabel(l2);
//                //    methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
//                methodVisitor.visitVarInsn(Opcodes.ASTORE, locationLocals);
//                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
//                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V", false);
//                methodVisitor.visitVarInsn(Opcodes.ALOAD, locationLocals);
//                methodVisitor.visitInsn(Opcodes.ATHROW);
//                methodVisitor.visitLabel(l3);
//                //        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
//                methodVisitor.visitInsn(Type.getReturnType(description).getOpcode(Opcodes.IRETURN));
//                methodVisitor.visitMaxs(0, 0);
//                //   methodVisitor.visitMaxs(locationLocals + 5, 8);
//                methodVisitor.visitEnd();
//                return new SynchronizedMethodVisitor(nameClass, access, name + "millredSynchronized", description, super.visitMethod(access & ~Opcodes.ACC_SYNCHRONIZED, name + "millredSynchronized", description, signature, Exceptions));
//            } else {
//                return new SynchronizedMethodVisitor(nameClass, access, name, description, super.visitMethod(access, name, description, signature, Exceptions));
//            }
//
//        }
//
//        @Override
//
//        public void visitEnd() {
//            FieldVisitor fv = super.visitField(Opcodes.ACC_PRIVATE, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;", null, null);
//            fv.visitEnd();
//
//            MethodVisitor methodVisitor = super.visitMethod(Opcodes.ACC_PUBLIC, "getLock", "()V", null, null);
//                            methodVisitor.visitCode();
////                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
////                methodVisitor.visitInsn(Opcodes.MONITORENTER);
////                methodVisitor.visitInsn(Opcodes.RETURN);
////                methodVisitor.visitMaxs(0, 0);
////                methodVisitor.visitEnd();
//            methodVisitor.visitCode();
//            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
//            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V", false);
//            methodVisitor.visitInsn(Opcodes.RETURN);
//            //        methodVisitor.visitMaxs(1, 1);
//            methodVisitor.visitMaxs(0, 0);
//            methodVisitor.visitEnd();
//
//            MethodVisitor methodVisitorTwo = super.visitMethod(Opcodes.ACC_PUBLIC, "releaseLock", "()V", null, null);
//            methodVisitorTwo.visitCode();
////             methodVisitorTwo.visitVarInsn(Opcodes.ALOAD, 0);
////            methodVisitorTwo.visitInsn(Opcodes.MONITORENTER);
////             methodVisitorTwo.visitVarInsn(Opcodes.ALOAD, 0);
////             methodVisitorTwo.visitInsn(Opcodes.MONITOREXIT);
////             methodVisitorTwo.visitInsn(Opcodes.RETURN);
//    //         methodVisitorTwo.visitEnd();
//
//            methodVisitorTwo.visitVarInsn(Opcodes.ALOAD, 0);
//            methodVisitorTwo.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//            methodVisitorTwo.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V", false);
//            methodVisitorTwo.visitVarInsn(Opcodes.ALOAD, 0);
//            methodVisitorTwo.visitFieldInsn(Opcodes.GETFIELD, nameClass, "intrinsicLock", "Ljava/util/concurrent/locks/ReentrantLock;");
//            methodVisitorTwo.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V", false);
//            methodVisitorTwo.visitInsn(Opcodes.RETURN);
//            methodVisitorTwo.visitMaxs(0, 0);
//            methodVisitorTwo.visitEnd();
//
//        }
//
//    }
//
//    class SynchronizedMethodVisitor extends AnalyzerAdapter {
//
//        public SynchronizedMethodVisitor(String nameClass, int access, String name, String desc, MethodVisitor mv) {
//            super(Opcodes.ASM5, nameClass, access, name, desc, mv);
//        }
//
//        @Override
//        public void visitInsn(int opcode) {
//
//            if (opcode == Opcodes.MONITORENTER) {
//                String desc = (String) stack.get(stack.size() - 1);
//                if (TypeUtil.isSystem(desc)) {
//                    super.visitInsn(opcode);
//                } else {
//                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, (String) stack.get(stack.size() - 1), "getLock", "()V", false);
//                }
//            } else if (opcode == Opcodes.MONITOREXIT) {
//                String desc = (String) stack.get(stack.size() - 1);
//                if (TypeUtil.isSystem(desc)) {
//                    super.visitInsn(opcode);
//                } else {
//                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, (String) stack.get(stack.size() - 1), "releaseLock", "()V", false);
//                }
//            } else {
//                super.visitInsn(opcode);
//            }
//        }
//
//    }
//
//}
