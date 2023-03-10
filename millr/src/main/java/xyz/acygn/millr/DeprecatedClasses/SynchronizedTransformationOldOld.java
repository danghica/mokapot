///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr.DeprecatedClasses;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import java.util.stream.Collectors;
//
//import javafx.util.Pair;
//import org.objectweb.asm.AnnotationVisitor;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
//import org.objectweb.asm.Handle;
//import org.objectweb.asm.Label;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.Type;
//import org.objectweb.asm.TypePath;
//import org.objectweb.asm.commons.AnalyzerAdapter;
//import xyz.acygn.millr.*;
//import xyz.acygn.millr.messages.MessageUtil;
//
///**
// * @author thomasc
// * @deprecated in favor of {@link SynchronizedTransformation}
// */
//@Deprecated
//public class SynchronizedTransformationOldOld extends Transformation {
//
//    List<ClassReader> listClassReader;
//    List<ClassReader> listToBeTransformed;
//    List<ClassReader> listTransformed;
//
//    public SynchronizedTransformationOldOld(List<ClassReader> listClassReader) {
//        super(listClassReader);
//        this.listClassReader = listClassReader;
//        listToBeTransformed = (List<ClassReader>) ((ArrayList) listClassReader).clone();
//        listTransformed = new ArrayList<ClassReader>(listToBeTransformed.size());
//    }
//
//    public void applyTransformation() {
//        while (!listToBeTransformed.isEmpty()) {
//            try {
//                getNewInstance(listToBeTransformed.get(0)).applyTransformation();
//            }
//            catch (Exception e) {
//                MessageUtil.error(e).report().resume();
//            }
//        }
//    }
//
//    private ClassWriter fromReaderToWriter(ClassReader cr) {
//        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//        cr.accept(cw, ClassReader.EXPAND_FRAMES);
//        return cw;
//    }
//
//    ClassReader getClassReader(String name) throws ClassNotFoundException {
//        Optional<ClassReader> classreader = listToBeTransformed.stream().filter(e -> (e.getClassName().equals(name))).findAny();
//        if (classreader.isPresent()) {
//            return classreader.get();
//        }
//        else {
//            classreader = listTransformed.stream().filter(e -> (e.getClassName().equals(name))).findAny();
//        }
//        if (classreader.isPresent()) {
//            return classreader.get();
//        }
//        else {
//            throw new ClassNotFoundException(name);
//        }
//    }
//
//    void updateClassReader(ClassReader cr) {
//        System.out.println(listToBeTransformed == null);
//        listToBeTransformed.removeIf(e -> e.getClassName().equals(cr.getClassName()));
//        listTransformed.removeIf(e -> (e.getClassName().equals(cr.getClassName())));
//        listToBeTransformed.add(cr);
//    }
//
//    public List<ClassWriter> getClassWriter() {
//        return listTransformed.stream().map(this::fromReaderToWriter).collect(Collectors.toList());
//    }
//
//    @Override
//    Instance getNewInstance(ClassReader cr) {
//        return new TransformationInstance(cr);
//    }
//
//    class TransformationInstance extends Instance {
//
//        public TransformationInstance(ClassReader cr) {
//            super(cr);
//            cv = new ClassVisitorRunnableTransformation(Opcodes.ASM5, cr.getClassName(), getVersion(cr), cw);
//        }
//
//        void applyTransformation() {
//            System.out.println("BEFORE APPLYING TRANSFO:");
//            System.out.println(listToBeTransformed.size());
//            System.out.println(listTransformed.size());
//            cr.accept(cv, ClassReader.EXPAND_FRAMES);
//            ClassReader additionalMethod = ((ClassVisitorRunnableTransformation) cv).getAdditionalMethods();
//            listTransformed.add(merge(cw, additionalMethod));
//            listToBeTransformed.removeIf(e -> (e.getClassName().equals(cr.getClassName())));
//            System.out.println("After applying Transfo:");
//            System.out.println(listToBeTransformed.size());
//            System.out.println(listTransformed.size());
//        }
//
//        ClassReader merge(ClassWriter cw, ClassReader cr) {
//            ClassReader classReader = new ClassReader(cw.toByteArray());
//            class reference {
//
//                Object object;
//            }
//            ClassWriter returnClassWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//            class fancyEndClassVisitor extends ClassVisitor {
//
//                public fancyEndClassVisitor(int api, ClassVisitor cv) {
//                    super(api, cv);
//                }
//
//                @Override
//                public void visitEnd() {
//
//                }
//
//                public void fancyVisitEnd() {
//                    super.visitEnd();
//                }
//            }
//            fancyEndClassVisitor fancyEnd = new fancyEndClassVisitor(Opcodes.ASM5, returnClassWriter);
//            ClassVisitor OnlyMethod = new ClassVisitor(Opcodes.ASM5) {
//                @Override
//                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//                    return fancyEnd.visitMethod(access, name, desc, signature, exceptions);
//                }
//            };
//            classReader.accept(fancyEnd, ClassReader.EXPAND_FRAMES);
//            cr.accept(OnlyMethod, ClassReader.EXPAND_FRAMES);
//            fancyEnd.fancyVisitEnd();
//            return new ClassReader(returnClassWriter.toByteArray());
//        }
//
//    }
//
//    class ClassVisitorRunnableTransformation extends ClassVisitor {
//
//        String nameClass;
//        int version;
//        MethodAdder methodAdder;
//
//        public ClassVisitorRunnableTransformation(int api, String nameClass, int version, ClassVisitor classVisitor) {
//            super(Opcodes.ASM5, classVisitor);
//            this.nameClass = nameClass;
//            this.version = version;
//            this.methodAdder = new MethodAdder(nameClass);
//        }
//
//        @Override
//        public MethodVisitor visitMethod(int access, String name, String description, String signature, String[] Exceptions) {
//            return new MethodVisitorRunnableTransformation(Opcodes.ASM5, nameClass, access, name, description, methodAdder, super.visitMethod(access, name, description, signature, Exceptions));
//        }
//
//        ClassReader getAdditionalMethods() {
//            return methodAdder.getAdditionalMethods();
//        }
//    }
//
//    class MethodVisitorRunnableTransformation extends AnalyzerAdapter {
//
//        List<Object[]> listTryCatchBlock;
//        boolean isStatic;
//        String name;
//        String desc;
//
//        //A methodAdder object that will collect the methods to add to the ClassReader itself (in the case of synchronized(this)), and add them once the transformation is done
//        MethodAdder methodAdder;
//
//        // Some instruction on the handling of exceptions caused by the monitorenter- monitorexit will needs to be removed.
//        boolean removeInstruction = false;
//
//        // The instructions to be removed are located through analaysis of the labels.
//        List<Label> MonitorTryCatch = new ArrayList<>();
//
//        boolean justEnterMonitor = false;
//
//        //The methodvisitor that will create the method that acts as the code in between the methodenter- methodexit /*
//        createMethodCode createMethodVisitor = null;
//
//        // If the instruction we go through is between some synchronized( ){ } brackets, then we redirect the instruction to the methodvisitor createMethodVisitor.
//        boolean redirectInstruction = false;
//
//        public MethodVisitorRunnableTransformation(int api, String owner, int access, String name, String desc, MethodAdder methodAdder, MethodVisitor mv) {
//            this(api, owner, access, name, desc, methodAdder, mv, new ArrayList<Object[]>());
//        }
//
//        public MethodVisitorRunnableTransformation(int api, String owner, int access, String name, String desc, MethodAdder methodAdder, MethodVisitor mv, List<Object[]> TryCatchBlock) {
//            super(api, owner, access, name, desc, mv);
//            this.name = name;
//            this.desc = desc;
//            isStatic = ((access & Opcodes.ACC_STATIC) != 0);
//            this.listTryCatchBlock = TryCatchBlock;
//            this.methodAdder = methodAdder;
//        }
//
//        @Override
//        public void visitInsn(int opcode) {
//            if (opcode == Opcodes.MONITOREXIT) {
//                System.out.println("monitorExit");
//                if (createMethodVisitor == null) {
//                    super.visitInsn(Opcodes.MONITOREXIT);
//                } //Otherwise, if we were redirecting the instruction to a methodvisitor, and this one did not retransmit them to a second one, then we must
//                // stop, get the result of the method, and update the stack and the locals.
//                else if (createMethodVisitor.redirectInstruction == false) {
//                    Pair<List<Type>, List<Type>> pair = createMethodVisitor.finishMethod();
//                    List<Type> stackType = pair.getKey();
//                    List<Type> localsType = pair.getValue();
//                    int location = isStatic ? 0 : 1;
//                    // We update every local variables;
//                    for (int index = 0; index < localsType.size(); index++) {
//                        if (localsType.get(index).getInternalName().equals(PathConstants.SEPARATION_CLASS)) {
//
//                        }
//                        else {
//                            super.visitInsn(Opcodes.DUP);
//                            super.visitIntInsn(Opcodes.BIPUSH, index);
//                            super.visitInsn(Opcodes.AALOAD);
//                            Type T = localsType.get(index);
//                            super.visitTypeInsn(Opcodes.CHECKCAST, T.getInternalName());
//                            if (T.getInternalName().startsWith(PathConstants.PRIMITIVE_WRAPPER)) {
//                                String description = null;
//                                if (T.getInternalName() == null ? PathConstants.BOOLEAN_WRAPPER == null : T.getInternalName().equals(PathConstants.BOOLEAN_WRAPPER)) {
//                                    description = "z";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.BYTE_WRAPPER == null : T.getInternalName().equals(PathConstants.BYTE_WRAPPER)) {
//                                    description = "B";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.CHAR_WRAPPER == null : T.getInternalName().equals(PathConstants.CHAR_WRAPPER)) {
//                                    description = "C";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.DOUBLE_WRAPPER == null : T.getInternalName().equals(PathConstants.DOUBLE_WRAPPER)) {
//                                    description = "D";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.FLOAT_WRAPPER == null : T.getInternalName().equals(PathConstants.FLOAT_WRAPPER)) {
//                                    description = "F";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.INT_WRAPPER == null : T.getInternalName().equals(PathConstants.INT_WRAPPER)) {
//                                    description = "I";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.LONG_WRAPPER == null : T.getInternalName().equals(PathConstants.LONG_WRAPPER)) {
//                                    description = "L";
//                                }
//                                if (T.getInternalName() == null ? PathConstants.SHORT_WRAPPER == null : T.getInternalName().equals(PathConstants.SHORT_WRAPPER)) {
//                                    description = "S";
//                                }
//                                if (description == null) {
//                                    throw new RuntimeException("desc = null");
//                                }
//                                super.visitFieldInsn(Opcodes.GETFIELD, T.getInternalName(), "internal", description);
//                                super.visitIntInsn(Type.getType(description).getOpcode(Opcodes.ISTORE), location);
//                                location += Type.getType(description).getSize();
//                            }
//                            else {
//                                super.visitIntInsn(localsType.get(index).getOpcode(Opcodes.ISTORE), location);
//                                location += localsType.get(index).getSize();
//                            }
//                        }
//                    }
//
//                    // We load the new Stack;
//                    for (int index = localsType.size(); index < stackType.size() + stackType.size(); index++) {
//                        super.visitInsn(Opcodes.DUP);
//                        super.visitIntInsn(Opcodes.BIPUSH, index);
//                        super.visitInsn(Opcodes.AALOAD);
//                        Type T = stackType.get(index - localsType.size());
//                        super.visitTypeInsn(Opcodes.CHECKCAST, T.getInternalName());
//                        if (T.getInternalName().startsWith(PathConstants.PRIMITIVE_WRAPPER)) {
//                            String descrption = null;
//                            if (T.getInternalName() == null ? PathConstants.BOOLEAN_WRAPPER == null : T.getInternalName().equals(PathConstants.BOOLEAN_WRAPPER)) {
//                                descrption = "z";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.BYTE_WRAPPER == null : T.getInternalName().equals(PathConstants.BYTE_WRAPPER)) {
//                                descrption = "B";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.CHAR_WRAPPER == null : T.getInternalName().equals(PathConstants.CHAR_WRAPPER)) {
//                                descrption = "C";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.DOUBLE_WRAPPER == null : T.getInternalName().equals(PathConstants.DOUBLE_WRAPPER)) {
//                                descrption = "D";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.FLOAT_WRAPPER == null : T.getInternalName().equals(PathConstants.FLOAT_WRAPPER)) {
//                                descrption = "F";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.INT_WRAPPER == null : T.getInternalName().equals(PathConstants.INT_WRAPPER)) {
//                                descrption = "I";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.LONG_WRAPPER == null : T.getInternalName().equals(PathConstants.LONG_WRAPPER)) {
//                                descrption = "L";
//                            }
//                            if (T.getInternalName() == null ? PathConstants.SHORT_WRAPPER == null : T.getInternalName().equals(PathConstants.SHORT_WRAPPER)) {
//                                descrption = "S";
//                            }
//                            if (descrption == null) {
//                                throw new RuntimeException("desc = null");
//                            }
//                            super.visitFieldInsn(Opcodes.GETFIELD, T.getInternalName(), "internal", descrption);
//                        }
//                        super.visitInsn(Opcodes.SWAP);
//                    }
//                    super.visitInsn(Opcodes.POP);
//                    createMethodVisitor = null;
//                    redirectInstruction = false;
//                } // Otherwise, if the createMethodVisitor methodvisitor was redirecting the instruction, then we let it handle that.
//                else {
//                    createMethodVisitor.visitInsn(opcode);
//                }
//
//            } // Monitoexit was a special case. In the general case, if we were
//            // redirecting instructions, then we just redirect this visitor to the createMethodVisitor.
//            else if (redirectInstruction == true) {
//                createMethodVisitor.visitInsn(opcode);
//            } // If we hit montorenter, and we are not redirecting instructions, then we must create a new createMethodVisitor, passes every locals and the state of the stack
//            // to the method, call it, and redirect future instructions to it.
//            // Finally, we put justEnterMonitor to true.
//            else if (opcode == Opcodes.MONITORENTER) {
//                System.out.println("monitorEnter");
//                String objectRef = (String) stack.get(stack.size() - 1);
//                // First, we measure the size of the stack.
//                List<Type> stackType = fromListToTypes(stack);
//                List<Type> localType = fromListToTypes(locals);
//                int location = 0;
//                for (Type t : localType) {
//                    super.visitIntInsn(t.getOpcode(Opcodes.ILOAD), location);
//                    location += t.getSize();
//                }
//                StringBuilder descMethod = new StringBuilder("(");
//                //The first item in the stack shall be the the object on which monitorenter is called
//                stackType.stream().skip(1).forEachOrdered(e -> descMethod.append(e.getDescriptor()));
//                localType.stream().forEachOrdered(e -> descMethod.append(e.getDescriptor()));
//                descMethod.append(")[Ljava/lang/Object;");
//                String randomMethodName = InputOutput.getInstance().generateNewMethodName();
//                createMethodVisitor = methodAdder.createNewMethod(objectRef, randomMethodName, descMethod.toString(), listTryCatchBlock, isStatic, locals, stack);
//                redirectInstruction = true;
//                justEnterMonitor = true;
//                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, objectRef, randomMethodName, descMethod.toString(), false);
//            }
//            else //General case: if we are not redirecting, and this is not a monitorenter-montorexit, then the instruction simply flows through.
//            {
//                super.visitInsn(opcode);
//            }
//        }
//
//        @Override
//        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitFrame(type, nLocal, local, nStack, stack);
//            }
//            else {
//                super.visitFrame(type, nLocal, local, nStack, stack);
//            }
//        }
//
//        @Override
//        public void visitIincInsn(int var, int increment
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitIincInsn(var, increment);
//            }
//            else {
//                super.visitIincInsn(var, increment);
//            }
//        }
//
//        @Override
//        public void visitFieldInsn(int opcode, String owner, String name, String desc
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitFieldInsn(opcode, owner, name, desc);
//            }
//            else {
//                super.visitFieldInsn(opcode, owner, name, desc);
//            }
//        }
//
//        @Override
//        public void visitIntInsn(int opcode, int operand
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitIntInsn(opcode, operand);
//            }
//            else {
//                super.visitIntInsn(opcode, operand);
//            }
//        }
//
//        @Override
//        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
//            }
//            else {
//                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
//            }
//        }
//
//        @Override
//        public void visitJumpInsn(int opcode, Label label
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitJumpInsn(opcode, label);
//            }
//            else {
//                super.visitJumpInsn(opcode, label);
//            }
//        }
//
//        /*
//                With the Oracle compiler, the Synchronized(){} is implemented as follows:
//                monitorenter
//                visitLabel L1
//                Code...
//                monitorexit
//                visitLabel L2
//
//
//                Label L3
//                Code in case of error during L1 L2
//                monitorexit
//                Label L4
//
//                Exception Table : L1 L2 L3
//                                  L3 L4 L3
//
//                We remove the code between L3 - L4, as the problem of the syncrhonization is now implemented through the call of the method
//
//                The goal of the implementation is to make sure the lock on the object is released even if the code fails.
//
//
//                We localized this code by finding L1, which is the first label visited right after monitorenter.
//                We store L1 L2 L3 into MonitorTryCatch
//                We find the labels L2 L3 by looking at the exception table, encoded in our case in the listTryCatchBlock.
//         */
//        @Override
//        public void visitLabel(Label label) {
//            System.out.println("visitLabel");
//            System.out.println("MonitorTryCatch.isEmpty() ?" + MonitorTryCatch.isEmpty());
//            System.out.println(justEnterMonitor);
//            if (justEnterMonitor && createMethodVisitor.redirectInstruction == false) {
//                System.out.println("in the right place");
//                Optional<Object[]> MonitorTryCatchOb = listTryCatchBlock.stream().filter(e -> e[0].equals(label)).findAny();
//                if (!MonitorTryCatchOb.isPresent()) {
//                    throw new RuntimeException("first label of monitorenter without associated tryCatchBlock");
//                }
//                else {
//                    MonitorTryCatch = new ArrayList<>();
//                    for (Object ob : MonitorTryCatchOb.get()) {
//                        MonitorTryCatch.add((Label) ob);
//                    }
//                    System.out.println("adding labels");
//                    listTryCatchBlock.remove(MonitorTryCatchOb.get());
//                }
//                justEnterMonitor = false;
//            }
//            else if (redirectInstruction) {
//                createMethodVisitor.visitLabel(label);
//            }
//            else if (!MonitorTryCatch.isEmpty()) {
//                if (label.equals(MonitorTryCatch.get(2))) {
//                    removeInstruction = true;
//                    MonitorTryCatch.clear();
//                }
//                else {
//                    super.visitLabel(label);
//                }
//            }
//            else {
//                List<Object[]> listVisitTyCatch = listTryCatchBlock.stream().filter(e -> e[0].equals(label)).collect(Collectors.toList());
//                for (Object[] ob : listVisitTyCatch) {
//                    visitTryCatchBlock((Label) ob[0], (Label) ob[1], (Label) ob[2], (String) ob[3]);
//                }
//                super.visitLabel(label);
//            }
//        }
//
//        @Override
//        public void visitLdcInsn(Object cst
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitLdcInsn(cst);
//            }
//            else {
//                super.visitLdcInsn(cst);
//            }
//        }
//
//        @Override
//        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels
//        ) {
//
//            if (redirectInstruction) {
//                createMethodVisitor.visitLookupSwitchInsn(dflt, keys, labels);
//            }
//            else {
//                super.visitLookupSwitchInsn(dflt, keys, labels);
//            }
//        }
//
//        @Override
//        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitMethodInsn(opcode, owner, name, desc, itf);
//            }
//            else {
//                super.visitMethodInsn(opcode, owner, name, desc, itf);
//            }
//
//        }
//
//        @Override
//        public void visitMultiANewArrayInsn(String desc, int dims
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitMultiANewArrayInsn(desc, dims);
//            }
//            else {
//                super.visitMultiANewArrayInsn(desc, dims);
//            }
//        }
//
//        @Override
//        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitTableSwitchInsn(min, max, dflt, labels);
//            }
//            else {
//                super.visitTableSwitchInsn(min, max, dflt, labels);
//            }
//        }
//
//        @Override
//        public void visitTypeInsn(int opcode, String type
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitTypeInsn(opcode, type);
//            }
//            else {
//                if (opcode == Opcodes.NEW) {
//                    stack.add(new Pair(type, Opcodes.UNINITIALIZED_THIS));
//                }
//                super.visitTypeInsn(opcode, type);
//            }
//        }
//
//        @Override
//        public void visitVarInsn(int opcode, int var
//        ) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitVarInsn(opcode, var);
//            }
//            else if (opcode == Opcodes.ASTORE) {
//                if (stack.get(stack.size()).equals(Opcodes.NULL)) {
//                    if (locals.get(var) instanceof Pair) {
//                        locals.set(var, ((Pair) locals.get(var)).getKey());
//                    }
//                    else {
//                        super.visitInsn(Opcodes.POP);
//                    }
//                }
//                else {
//                    super.visitVarInsn(opcode, var);
//                }
//            }
//            else {
//                super.visitVarInsn(opcode, var);
//            }
//        }
//
//        private void printStackLocal() {
//            System.out.println("the stack");
//            System.out.println(Arrays.deepToString(stack.toArray()));
//            System.out.println("the locals");
//            System.out.println(Arrays.deepToString(locals.toArray()));
//        }
//
//        @Override
//        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//            if (redirectInstruction) {
//                return createMethodVisitor.visitInsnAnnotation(typeRef, typePath, desc, visible);
//            }
//            else {
//                return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
//            }
//        }
//
//        @Override
//        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitTryCatchBlock(start, end, handler, type);
//            } // We delay the visit of the TryCatchBlock until we hit the first label of it.
//            else {
//                listTryCatchBlock.add(new Object[]{start, end, handler, type});
//            }
//        }
//
//        @Override
//        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
//            if (redirectInstruction) {
//                return createMethodVisitor.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
//            }
//            else {
//                return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
//            }
//        }
//
//        @Override
//        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitLocalVariable(name, desc, signature, start, end, index);
//            }
//            else {
//                super.visitLocalVariable(name, desc, signature, start, end, index);
//            }
//        }
//
//        @Override
//        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
//            if (redirectInstruction) {
//                return visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
//            }
//            else {
//                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
//            }
//        }
//
//        @Override
//        public void visitLineNumber(int line, Label start) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitLineNumber(line, start);
//            }
//            else {
//                super.visitLineNumber(line, start);
//            }
//        }
//
//        @Override
//        public void visitMaxs(int maxStack, int maxLocals) {
//            if (redirectInstruction) {
//                createMethodVisitor.visitMaxs(maxStack, maxLocals);
//            }
//            super.visitMaxs(maxStack, maxLocals);
//        }
//
//        @Override
//        public void visitEnd() {
//            System.out.println("We reached the end");
//            if (redirectInstruction) {
//                throw new RuntimeException("visitEnd reached before a monitorexit");
//            }
//            super.visitEnd();
//        }
//    }
//
//    class createMethodCode extends MethodVisitorRunnableTransformation {
//
//        boolean isStatic;
//        List<Object> initialStack;
//        List<Object> initialLocal;
//        //      private final List<Object> additionalLocals;
//
//        public createMethodCode(int api, String owner, int access, String name, String description, MethodVisitor mv, List<Object[]> listTryCatch, MethodAdder inheritedMethodAdder, List<Object> stackType, List<Object> localType) {
//            super(api, owner, access, name, description, inheritedMethodAdder, mv, listTryCatch);
//            isStatic = ((access & Opcodes.ACC_STATIC) != 0);
//            initialStack = stackType;
//            initialLocal = localType;
//        }
//
//        public createMethodCode(int api, String owner, int access, String name, String description, MethodVisitor mv, List<Object[]> listTryCatch, MethodAdder inheritedMethodAdder) {
//            super(api, owner, access, name, description, inheritedMethodAdder, mv, listTryCatch);
//            isStatic = ((access & Opcodes.ACC_STATIC) != 0);
//        }
//
//        void updateTheCode() {
//        }
//
//        public Pair<List<Type>, List<Type>> finishMethod() {
//            super.visitInsn(Opcodes.POP);
//            List<Type> stackTypes = fromListToTypes(stack);
//            List<Type> localsTypes = fromListToTypes(locals);
//            if (!isStatic) {
//                localsTypes.remove(0);
//            }
//            Pair<List<Type>, List<Type>> returnPair = new Pair(stackTypes, localsTypes);
//            // compute the size of the locals;
//            //First, we store the stack in order to empty it.
//            int locationlocals = isStatic ? 0 : 1;
//            locationlocals = localsTypes.stream().map((t) -> t.getSize()).reduce(locationlocals, Integer::sum);
//            for (Type t : stackTypes) {
//                super.visitVarInsn(t.getOpcode(Opcodes.ISTORE), locationlocals);
//                locationlocals += t.getSize();
//            }
//
//            super.visitIntInsn(Opcodes.BIPUSH, localsTypes.size() + stackTypes.size());
//            super.visitTypeInsn(Opcodes.ANEWARRAY, "Ljava/lang/Object;");
//            localsTypes.addAll(stackTypes);
//            locationlocals = isStatic ? 0 : 1;
//            for (int index = 0; index < localsTypes.size(); index++) {
//                Type T = localsTypes.get(index);
//                super.visitInsn(Opcodes.DUP);
//                super.visitIntInsn(Opcodes.BIPUSH, index);
//                if (TypeUtil.isPrimitive(T)) {
//                    super.visitTypeInsn(Opcodes.NEW, PathConstants.PRIMITIVE_WRAPPER + "$" + T.getClassName() + "wrapper");
//                    super.visitInsn(Opcodes.DUP);
//                    super.visitIntInsn(localsTypes.get(index).getOpcode(Opcodes.ILOAD), locationlocals);
//                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, PathConstants.PRIMITIVE_WRAPPER + "$" + T.getClassName() + "wrapper", "<init>", "(" + T.getDescriptor() + ")V", false);
//                }
//                else {
//                    super.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationlocals);
//                }
//                super.visitInsn(Opcodes.AASTORE);
//                locationlocals += T.getSize();
//            }
//            super.visitInsn(Opcodes.ARETURN);
//            super.visitEnd();
//            updateTheCode();
//            return returnPair;
//
//        }
//
//        private int localShift;
//
//        @Override
//        public void visitCode() {
//            int i = 0;
//            int indexlocal = isStatic ? 0 : 1;
//            List<Type> initialStackTypes = fromListToTypes(stack);
//            while (i < initialStackTypes.size()) {
//                super.visitVarInsn(initialStackTypes.get(i).getOpcode(Opcodes.ILOAD), indexlocal);
//                indexlocal += initialStackTypes.get(i).getSize();
//                i += 1;
//            }
//            localShift = indexlocal;
//        }
//
//        @Override
//        public void visitVarInsn(int opcode, int var) {
//            if (var != 0) {
//                super.visitVarInsn(opcode, var + localShift);
//            }
//            else {
//                super.visitVarInsn(opcode, var);
//            }
//        }
//
//        @Override
//        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
//            // The stack should be the same, but the locals are quite different.
//            nLocal += initialStack.size();
//            List<Object> updatedlocal = (List) ((ArrayList) initialStack).clone();
//            updatedlocal.addAll(Arrays.asList(local));
//            local = updatedlocal.toArray();
//            super.visitFrame(type, nLocal, local, nStack, stack);
//        }
//    }
//
//    class MethodAdder {
//
//        private ClassReader classReader;
//        private String nameClass;
//        private final MethodAdder selfRef;
//
//        MethodAdder(String nameClass) {
//            this.nameClass = nameClass;
//            ClassWriter classwriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//            classwriter.visit(51, Opcodes.ACC_PUBLIC, "additionalMethods", "()V", null, null);
//            this.classReader = new ClassReader(classwriter.toByteArray());
//            selfRef = this;
//        }
//
//        void updateReader(ClassWriter cw) {
//            classReader = new ClassReader(cw.toByteArray());
//        }
//
//        String getMethodDescWithoutFirstArg(String desc) {
//            StringBuilder descriptionMethod = new StringBuilder("(");
//            Type[] argTypes = Type.getArgumentTypes(desc);
//            for (int i = 1; i < argTypes.length; i++) {
//                descriptionMethod.append(argTypes[i].getDescriptor());
//            }
//            descriptionMethod.append(")[Ljava/lang/Object;");
//            return descriptionMethod.toString();
//        }
//
//        createMethodCode createNewMethod(String owner, String methodName, String description, List<Object[]> listTryCatch, boolean isStatic, List<Object> localType, List<Object> stackType) {
//            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//            class ObjectWrapper {
//
//                Object internal;
//            }
//            final ObjectWrapper reference = new ObjectWrapper();
//            ClassVisitor classvisitor = new ClassVisitor(Opcodes.ASM5, cw) {
//                @Override
//                public void visitEnd() {
//                    int access = isStatic ? Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC : Opcodes.ACC_PUBLIC;
//                    MethodVisitor mv = this.visitMethod(access, methodName, description, null, null);
//                    System.out.println(description);
//                    System.out.println("ISSTATIC " + isStatic);
//                    String newDesc = isStatic ? description : getMethodDescWithoutFirstArg(description);
//                    reference.internal = new createMethodCode(Opcodes.ASM5, owner, access, methodName, newDesc, mv, listTryCatch, selfRef, localType, stackType) {
//                        @Override
//                        void updateTheCode() {
//                            updateReader(cw);
//                        }
//                    };
//
//                }
//            };
//            classReader.accept(classvisitor, ClassReader.EXPAND_FRAMES);
//            try {
//                ClassReader cr = getClassReader(owner);
//                ClassWriter classwriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//                ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, classwriter) {
//                    @Override
//                    public void visitEnd() {
//                        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC & Opcodes.ACC_SYNCHRONIZED, methodName, description, null, null);
//                        mv.visitCode();
//                        Type[] listInputType = Type.getArgumentTypes(description);
//                        int locationlocals = 1;
//                        if (!isStatic) {
//                            // First, we have to locate the reference to the object that calls the methods. It should be the first of the locals loaded in the stack.
//                            int locationMethodRef = 1;
//                            List<Type> listStackType = fromListToTypes(stackType);
//                            locationlocals += 1 + listStackType.stream().mapToInt(e -> e.getSize()).sum();
//                            Type[] inputTypes = Type.getArgumentTypes(description);
//                            // We now load the object on which the method will be called;
//                            mv.visitVarInsn(Opcodes.ALOAD, locationMethodRef);
//                            // We now load all the arguements, without the objectReference of course.
//                            locationlocals = 1;
//                            for (Type T : fromListToTypes(stackType)) {
//                                mv.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationlocals);
//                                locationlocals += T.getSize();
//                            }
//                            for (Type T : fromListToTypes(localType).subList(1, localType.size())) {
//                                mv.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationlocals);
//                                locationlocals += T.getSize();
//                            }
//                        }
//                        else {
//                            for (int index = 0; index < listInputType.length; index++) {
//                                Type T = listInputType[index];
//                                mv.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationlocals);
//                                locationlocals += T.getSize();
//                            }
//                        }
//                        if (isStatic) {
//                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, nameClass, methodName, description, false);
//                        }
//                        else {
//                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, nameClass, methodName, getMethodDescWithoutFirstArg(description), false);
//                        }
//                        mv.visitInsn(Opcodes.ARETURN);
//                        mv.visitEnd();
//                        super.visitEnd();
//                    }
//
//                };
//                cr.accept(cv, ClassReader.EXPAND_FRAMES);
//                updateClassReader(new ClassReader(classwriter.toByteArray()));
//
//            }
//            catch (ClassNotFoundException ex) {
//                throw new RuntimeException(ex);
//            }
//
//            return (createMethodCode) reference.internal;
//        }
//
//        ClassReader getAdditionalMethods() {
//            return classReader;
//        }
//
//    }
//
//    private int getVersion(ClassReader cr) {
//        class intWrapper {
//
//            int internal;
//        }
//        final intWrapper pointer = new intWrapper();
//        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5) {
//            @Override
//            public void visit(int version,
//                              int access,
//                              String name,
//                              String signature,
//                              String superName,
//                              String[] interfaces) {
//                pointer.internal = version;
//            }
//        };
//        cr.accept(classVisitor, ClassReader.SKIP_CODE);
//        return pointer.internal;
//    }
//
//    private List<Type> fromListToTypes(List<Object> listObject) {
//        List<Type> listType = new ArrayList<>();
//        for (int index = 0; index < listObject.size(); index++) {
//            Object o = listObject.get(index);
//            if (o == Opcodes.UNINITIALIZED_THIS || o instanceof Label) {
//                throw new RuntimeException("transformation impossible due to unitializedthis present");
//            }
//            if (o instanceof Integer) {
//                try {
//                    // Primitive types are represented by Opcodes.TOP, Opcodes.INTEGER, Opcodes.FLOAT,
//                    // Opcodes.LONG, Opcodes.DOUBLE,Opcodes.NULL or Opcodes.UNINITIALIZED_THIS
//                    // long and double are represented by two elements, the second one being TOP
//                    int value = (Integer) ((Integer) o).intValue();
//                    if (value == Opcodes.TOP) {
//                        throw new RuntimeException("fromListToType encountered a TOP value");
//                    }
//                    if (value == Opcodes.INTEGER) {
//                        listType.add(Type.INT_TYPE);
//                    }
//                    if (value == Opcodes.FLOAT) {
//                        listType.add(Type.FLOAT_TYPE);
//                    }
//                    if (value == Opcodes.LONG) {
//                        listType.add(Type.LONG_TYPE);
//                        index++;
//                    }
//                    if (value == Opcodes.DOUBLE) {
//                        listType.add(Type.DOUBLE_TYPE);
//                        index++;
//                    }
//                    if (value == Opcodes.NULL) {
//                        System.err.print("null reference in stack. We assume it of type Objects. Might break things");
//
//
//                    }
//
//                }
//                catch (Exception ex) {
//                    Logger.getLogger(SynchronizedTransformationOld.class
//                            .getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            else if (o instanceof String) {
//                listType.add(Type.getObjectType((String) o));
//            }
//            else {
//                throw new RuntimeException("object" + o.toString() + " in the locals are neither unitilized, nor integer, nor String, nor label");
//            }
//        }
//        return listType;
//    }
//
//}
