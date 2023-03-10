package xyz.acygn.millr;

import java.io.IOException;
import static java.lang.System.err;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.AnalyzerAdapter;
import xyz.acygn.millr.messages.TransformationFailedException;

/**
 *
 *
 * @author thomasc
 */
public class SynchronizedTransformation extends Transformation implements Opcodes {

    public SynchronizedTransformation(SubProject sp) throws Exception {
        super(sp);
    }

    @Override
    String getNameOfTheTransformation() {
        return "synchronized Transformation ";
    }

    static final String COUNTDOWNLATCHClASS = "java.util.concurrent.CountDownLatch";
    static final String COUNTDOWNLATCHDesc = "Ljava/util/concurrent/CountDownLatch;";
    static final String doneLatch = "_millr_doneLatch";
    static final String startLatch = "_millr_startLatch";
 

    @Override
    Instance getNewInstance(ClassReader cr) {
        return new TransformationInstance(cr);
    }

    class TransformationInstance extends Instance {

        final ClassVisitor cv;
        final ClassWriter cw;


        @Override
        void classTransformerToClassWriter() {

        }

        @Override
        ClassVisitor getClassTransformer() {
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

        public TransformationInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0){
                cv = cw;
            }
            else {
                cv = new ClassVisitorRunnableTransformation(Opcodes.ASM6, cr.getClassName(), cw);
            }
        }

    }

    class ClassVisitorRunnableTransformation extends ClassVisitor {

        final String nameClass;

        public ClassVisitorRunnableTransformation(int api, String nameClass, ClassVisitor classVisitor) {
            super(api, classVisitor);
            this.nameClass = nameClass;
        }



        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            interfaces = (interfaces == null) ? new String[0] : interfaces;
            String[] newInterface = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, newInterface, 0, interfaces.length);
            newInterface[newInterface.length  -1] = "xyz/acygn/millr/util/hasSyncMethods";
            super.visit(version, access, name, signature, superName, newInterface);
        }

        @Override
        public void visitEnd() {
            FieldVisitor fv = super.visitField(Opcodes.ACC_PRIVATE, doneLatch, "Ljava/util/concurrent/CountDownLatch;", null, null);
            fv.visitEnd();

            MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "startLock", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/InterruptedException");
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, "java/util/concurrent/CountDownLatch");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/CountDownLatch", "<init>", "(I)V", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitTypeInsn(NEW, "xyz/acygn/millr/util/LatchThread");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "xyz/acygn/millr/util/LatchThread", "<init>", "(Lxyz/acygn/millr/util/hasSyncMethods;Ljava/util/concurrent/CountDownLatch;)V", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "run", "()V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CountDownLatch", "await", "()V", false);
            mv.visitLabel(l1);
            Label l3 = new Label();
            mv.visitJumpInsn(GOTO, l3);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/InterruptedException"});
            mv.visitVarInsn(ASTORE, 1);
            mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Interupted Exception thrown by startLatch: this case should have never happened");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/InterruptedException", "getMessage", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();

            mv = super.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getLock", "(Ljava/util/concurrent/CountDownLatch;)V", null, null);
            mv.visitCode();
            Label l10 = new Label();
            Label l11 = new Label();
            Label l12 = new Label();
            mv.visitTryCatchBlock(l10, l11, l12, "java/lang/InterruptedException");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CountDownLatch", "countDown", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, "java/util/concurrent/CountDownLatch");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/CountDownLatch", "<init>", "(I)V", false);
            mv.visitFieldInsn(PUTFIELD, nameClass, doneLatch, "Ljava/util/concurrent/CountDownLatch;");
            mv.visitLabel(l10);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, nameClass, doneLatch, "Ljava/util/concurrent/CountDownLatch;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CountDownLatch", "await", "()V", false);
            mv.visitLabel(l11);
            Label l13 = new Label();
            mv.visitJumpInsn(GOTO, l13);
            mv.visitLabel(l12);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/InterruptedException"});
            mv.visitVarInsn(ASTORE, 2);
            mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Interupted Exception thrown by doneLatch: this case should have never happened");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/InterruptedException", "getMessage", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l13);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();

            mv = super.visitMethod(ACC_PUBLIC, "releaseLock", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, nameClass, doneLatch, "Ljava/util/concurrent/CountDownLatch;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/CountDownLatch", "countDown", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions){
            return new SynchronizedMethodVisitor(nameClass, access, name, desc, super.visitMethod(access, name, desc, signature, exceptions));
        }

    }

    class SynchronizedMethodVisitor extends AnalyzerAdapter {

        public SynchronizedMethodVisitor(String nameClass, int access, String name, String desc, MethodVisitor mv) {
            super(Opcodes.ASM6, nameClass, access, name, desc, mv);
        }

        @Override
        public void visitInsn(int opcode) {

            if (opcode == Opcodes.MONITORENTER) {
                String desc = (String) stack.get(stack.size() - 1);
                if (TypeUtil.isSystem(desc)) {
                    super.visitInsn(opcode);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, (String) stack.get(stack.size() - 1), "startLock", "()V", false);
                }
            } else if (opcode == Opcodes.MONITOREXIT) {
                String desc = (String) stack.get(stack.size() - 1);
                if (TypeUtil.isSystem(desc)) {
                    super.visitInsn(opcode);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, (String) stack.get(stack.size() - 1), "releaseLock", "()V", false);
                }
            } else {
                super.visitInsn(opcode);
            }
        }

    }

}
