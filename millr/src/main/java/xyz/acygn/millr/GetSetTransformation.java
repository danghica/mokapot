/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import xyz.acygn.millr.messages.*;

/**
 * @author thomas This transformation implements a set of getter/setter methods
 * for every fields of the object, and replace, in the methods, every access to
 * fields other than the one belonging to "this" and the ones "not part of the
 * project" (so those whose getter/setter functions have not been created), by
 * calls to the getter-setter methods.
 */
class GetSetTransformation extends Transformation {

    private Collection<getSetTransformationInstance> listTransformation;

    public GetSetTransformation(SubProject sp) throws Exception {
        super(sp);
    }

    @Override
    public Instance getNewInstance(ClassReader cr) {
        return new getSetTransformationInstance(cr);
    }

    @Override
    String getNameOfTheTransformation() {
        return "getters and setters Transformation ";
    }

    class getSetTransformationInstance extends Instance {

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

        public getSetTransformationInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cv = new GetSetClassVisitor(Opcodes.ASM6, cr.getClassName());
        }


        @Override
        void classTransformerToClassWriter() {
            getClassTransformer().accept(getClassWriter());
        }


        class GetSetClassVisitor extends ClassNode {

            private String className;


            public GetSetClassVisitor(int api, String className) {
                super(api);
                this.className = className;

            }

            @Override
            public void visitEnd() {
                if(cr.getClassName().equals("org/gjt/sp/jedit/gui/SplashScreen")){
                    System.out.println("stop");
                }
                //ClassDataBase has been defined at the start of the transformation. Therefore, the information
                //it contains about fields are now outdated.
                //Fields have been replaced by the array Transformation to fields with new names and new description.
                for (FieldParameter field : ClassDataBase.getClassData(className).getAllField()) {
                    FieldParameter newField;
                    if (VisibilityTools.isValueField(field.cp.className, field.name, field.desc)){
                        newField = field;
                    }
                    else if (TypeUtil.isNotProject(field.cp.className)){
                        newField= field;
                    }
                    else {
                     newField = new FieldParameter(
                                field.cp,
                                field.access,
                                ObjectMillrNamingUtil.getNewNameString(field),
                                SignatureArrayTypeModifier.getNewSignature(field.desc),
                                SignatureArrayTypeModifier.getNewSignature(field.signature)
                        ); }

                    createGetterSetter(newField);
                }
                for (MethodNode mn : this.methods) {
                    if (!ObjectMillrNamingUtil.isComingFromMillr(MethodParameter.getCoreMethodParameter(className, mn.name, mn.desc))) {
                        try {
                            replaceFieldAccess(mn, className);
                        } catch (Throwable t) {
                            throw new MethodTransformationFailedException(t, className, mn.name, mn.desc);
                        }
                    }
                }

            }

            void createGetterSetter(FieldParameter fp) {
                int access = fp.access;
                String desc = fp.desc;
                String className = fp.cp.className;
                String name = fp.name;
                if ((access & Opcodes.ACC_STATIC) != 0) {
                    if ((access & Opcodes.ACC_FINAL) == 0) {
                        // We cannot create a setter for a static final field.
                        MethodVisitor mvSet = this.visitMethod(access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_ENUM,
                                ObjectMillrNamingUtil.getNameSetterField(fp), "(" + desc + ")V", null, null);
                        mvSet.visitVarInsn(Type.getType(desc).getOpcode(Opcodes.ILOAD), 0);
                        mvSet.visitFieldInsn(Opcodes.PUTSTATIC, className, name, desc);
                        mvSet.visitInsn(Opcodes.RETURN);
                        mvSet.visitMaxs(0, 0);
                        mvSet.visitEnd();
                    }
                    MethodVisitor mvGet = this.visitMethod(access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_ENUM,
                            ObjectMillrNamingUtil.getNameGetterField(fp), "()" + desc, null, null);
                    mvGet.visitFieldInsn(Opcodes.GETSTATIC, className, name, desc);
                    mvGet.visitInsn(Type.getType(desc).getOpcode(Opcodes.IRETURN));
                    mvGet.visitMaxs(0, 0);
                    mvGet.visitEnd();
                } else {
                    MethodVisitor mvSet = this.visitMethod(access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_ENUM,
                            ObjectMillrNamingUtil.getNameSetterField(fp), "(" + desc + ")V", null, null);
                    mvSet.visitVarInsn(Opcodes.ALOAD, 0);
                    mvSet.visitVarInsn(Type.getType(desc).getOpcode(Opcodes.ILOAD), 1);
                    mvSet.visitFieldInsn(Opcodes.PUTFIELD, className, name, desc);
                    mvSet.visitInsn(Opcodes.RETURN);
                    mvSet.visitMaxs(0, 0);
                    mvSet.visitEnd();


                    MethodVisitor mvGet = this.visitMethod(access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_ENUM,
                            ObjectMillrNamingUtil.getNameGetterField(fp), "()" + desc, null, null);
                    mvGet.visitVarInsn(Opcodes.ALOAD, 0);
                    mvGet.visitFieldInsn(Opcodes.GETFIELD, className, name, desc);
                    mvGet.visitInsn(Type.getType(desc).getOpcode(Opcodes.IRETURN));
                    mvGet.visitMaxs(0, 0);
                    mvGet.visitEnd();
                }
            }

            public void replaceFieldAccess(MethodNode mn, String owner) throws AnalyzerException {
                if (cr.getClassName().equals("org/gjt/sp/jedit/gui/SplashScreen")){
                    System.out.println("debug");
                }
                Analyzer<SourceValue> a = new Analyzer(new SourceInterpreter());
                //  Analyzer<BasicValue> b = new Analyzer(new BasicInterpreter());
                if ((mn.access & Opcodes.ACC_STATIC) != 0) {
                    return;
                }
                if (isGetterAndSetterFromMillr(mn)) {
                    return;
                }
                a.analyze(owner, mn);
                //      b.analyze(owner, mn);

                Frame<SourceValue>[] frames = a.getFrames();
                //    Frame<BasicValue>[] framesValues = b.getFrames();
                InsnList listInstru = mn.instructions;
                AbstractInsnNode node = listInstru.getFirst();
                int i = 0;
                while (node != null) {
                    if (node instanceof FieldInsnNode) {
                        FieldInsnNode fieldNode = (FieldInsnNode) node;
                        int opcode = fieldNode.getOpcode();
                        if (opcode == Opcodes.GETFIELD) {
                            SourceValue ownerOfField = frames[i].getStack(frames[i].getStackSize() - 1);
                            //     BasicValue finerOwner = framesValues[i].getStack(framesValues[i].getStackSize() - 1);

                            if (!valueIsThis(ownerOfField)) {
                                //     System.out.println("finerOwner  " + finerOwner.getType().getInternalName());
                                //     System.out.println("Field owner " + fieldNode.owner);
                                FieldParameter fp = FieldParameter.getFieldParameter(fieldNode.owner, fieldNode.name, fieldNode.desc);
                                if (TypeUtil.isNotProject(fieldNode.owner)) {
                                    Mill.getInstance().addReasonForUnsafe(className, new Reason.methodAccessApiField(
                                            MethodParameter.getCoreMethodParameter(className, mn.name, mn.desc),
                                            fp
                                    ));
                                } else {
                                    MethodInsnNode newNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldNode.owner,
                                            ObjectMillrNamingUtil.getNameGetterField(fp),
                                            "()" + fieldNode.desc, false);
                                    listInstru.insert(node, newNode);
                                    listInstru.remove(node);
                                    node = newNode;
                                }
                            }
                        } else if (opcode == Opcodes.PUTFIELD) {
                            SourceValue ownerOfField = frames[i].getStack(frames[i].getStackSize() - (1 + Type.getType(fieldNode.desc).getSize()));
                            if (!valueIsThis(ownerOfField)) {
                                //      BasicValue finerOwner = framesValues[i].getStack(framesValues[i].getStackSize() - 2);
                                FieldParameter fp = FieldParameter.getFieldParameter(fieldNode.owner, fieldNode.name, fieldNode.desc);
                                if (TypeUtil.isNotProject(fieldNode.owner)) {
                                    Mill.getInstance().addReasonForUnsafe(className, new Reason.methodAccessApiField(
                                            MethodParameter.getCoreMethodParameter(className, mn.name, mn.desc),
                                            fp));
                                } else {
                                    MethodInsnNode newNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldNode.owner,
                                            ObjectMillrNamingUtil.getNameSetterField(fp),
                                            "(" + fieldNode.desc + ")V", false);
                                    listInstru.insert(node, newNode);
                                    listInstru.remove(node);
                                    node = newNode;
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

            @Deprecated
            private class GetSetMethodVisitor extends MethodVisitor {

                private String className;

                public GetSetMethodVisitor(int api) {
                    super(api);
                }

                public GetSetMethodVisitor(int api, MethodVisitor mv, String className) {
                    super(api, mv);
                    this.className = className;
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    if (opcode == Opcodes.GETFIELD && (!owner.equals(className)) && !TypeUtil.isNotProject(owner)) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "_millr_" + name + "get", "()" + desc, false);
                    } else if (opcode == Opcodes.PUTFIELD && (!owner.equals(className)) && !TypeUtil.isNotProject(owner)) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "_millr_" + name + "set", "(" + desc + ")V", false);
                    } else if (opcode == Opcodes.GETSTATIC && (!owner.equals(className)) && !TypeUtil.isNotProject(owner)) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "_millr_" + name + "get", "()" + desc, false);
                    } else if (opcode == Opcodes.PUTSTATIC && (!owner.equals(className)) && !TypeUtil.isNotProject(owner)) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "_millr_" + name + "set", "(" + desc + ")V", false);
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                }

            }

        }

        private boolean isGetterAndSetterFromMillr(MethodNode mn) {
            return (mn.name.startsWith("_millr_") && (mn.name.endsWith("get") || mn.name.endsWith("set")));
        }


    }

}
