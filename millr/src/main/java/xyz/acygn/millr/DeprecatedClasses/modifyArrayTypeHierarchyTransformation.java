///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr;
//
//import java.util.Collection;
//import java.util.Set;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.Label;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.Type;
//
///**
// *
// * @author thomasc
// * Deprecated class.
// * Was used to modify API hierarchy in case one class from the project extended/implemented another from the API, but milling the project-class would
// * break types.
// */
// class modifyArrayTypeHierarchyTransformation extends Transformation {
//
//    public modifyArrayTypeHierarchyTransformation(SubProject  sp) throws Exception {
//        super(sp);
//    }
//
//    Set<String> setClassAPIModified;
//
//    public modifyArrayTypeHierarchyTransformation(Collection<ClassReader> listClassReader, Set<String> setClassAPIModified) {
//        super(listClassReader);
//        this.setClassAPIModified = setClassAPIModified;
//    }
//
//    @Override
//    Instance getNewInstance(ClassReader cr) {
//        return new modifyArrayTypeHierarchyTransformationInstance(cr);
//    }
//
//    private class modifyArrayTypeHierarchyTransformationInstance extends Instance {
//
//        public modifyArrayTypeHierarchyTransformationInstance(ClassReader cr) {
//            super(cr);
//            cv = new ModifyArrayTypeHierarchyVisitor(Opcodes.ASM5, cw);
//        }
//    }
//
//        class ModifyArrayTypeHierarchyVisitor extends ClassVisitor {
//
//            public ModifyArrayTypeHierarchyVisitor(int api, ClassVisitor cv) {
//                super(api, cv);
//            }
//
//            @Override
//            public MethodVisitor visitMethod(int access,
//                    String name,
//                    String desc,
//                    String signature,
//                    String[] exceptions) {
//                return new ArrayMethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions));
//            }
//
//        }
//
//        class ArrayMethodVisitor extends MethodVisitor {
//
//            public ArrayMethodVisitor(int api, MethodVisitor mv) {
//                super(api, mv);
//            }
//
//            private String changeDesc(String desc) {
//                if (desc == null){
//                    return null;
//                }
//                for (String oldDesc : setClassAPIModified) {
//                    desc = desc.replace(oldDesc, PathConstants.API_ARRAY_FOLDER.getName() + "/" + oldDesc);
//                }
//                return desc;
//            }
//
//            @Override
//            public void visitTypeInsn(int opcode,
//                          String type){
//                super.visitTypeInsn(opcode, changeDesc(type));
//            }
//
//            @Override
//            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
//                super.visitFieldInsn(opcode, changeDesc(owner), name, changeDesc(desc));
//            }
//
//            @Override
//            public void visitMethodInsn(int opcode,
//                    String owner,
//                    String name,
//                    String desc,
//                    boolean itf) {
//                super.visitMethodInsn(opcode, changeDesc(owner), name, changeDesc(desc), itf);
//            }
//
//            @Override
//            public void visitMultiANewArrayInsn(String desc, int dims) {
//                super.visitMultiANewArrayInsn(changeDesc(desc), dims);
//            }
//
//            @Override
//            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//                super.visitTryCatchBlock(start, end, handler, changeDesc(type));
//            }
//
//            @Override
//            public void visitLocalVariable(String name, String desc, String signature,
//                    Label start,
//                    Label end,
//                    int index) {
//                super.visitLocalVariable(name, changeDesc(desc), changeDesc(signature), start, end, index);
//            }
//
//            @Override
//            public void visitLdcInsn(Object cst) {
//                if (cst instanceof Type) {
//                    int sort = ((Type) cst).getSort();
//                    if (sort == Type.OBJECT) {
//                        cst = Type.getType(changeDesc(((Type) cst).getDescriptor()));
//                    }
//                }
//                super.visitLdcInsn(cst);
//            }
//        }
//
//    }
