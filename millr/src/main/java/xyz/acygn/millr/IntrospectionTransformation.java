
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import org.objectweb.asm.*;

/**
 *
 * @author thomas
 * This transformation replace every instanceof or the ifacpme (that compares for reference equality) bytecode instruction to calls to methods. It furthermore also replaces the getClass instruction
 * in order for it to return the class of the referent object, not the one of the milled-class /standin  stanting for it.
 */
public class IntrospectionTransformation extends Transformation {

    public IntrospectionTransformation(SubProject sp) throws Exception {
        super(sp);
    }

    @Override
    String getNameOfTheTransformation() {
        return " introspection transformation ";
    }

    public Instance getNewInstance(ClassReader cr) {
        return new IntrospectionInstance(cr);
    }

    class IntrospectionInstance extends Instance {

        final ClassVisitor cv;
        final ClassWriter cw;

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

        public IntrospectionInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            if (cr.getClassName().equals("xyz/acygn/mokapot/util/TypeSafe")){
                cv = new ClassVisitor(Opcodes.ASM6, cw){};
            }
            else {
                cv = new DirectObjectClassVisitor(Opcodes.ASM6, cw);
            }
        }

        @Override
        void classTransformerToClassWriter() {

        }

    }

    class DirectObjectClassVisitor extends ClassVisitor {

        public DirectObjectClassVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor superMethod = super.visitMethod(access, name, desc, signature, exceptions);
            return new DirectObjectMethodVisitor(api, superMethod);
        }

        private class DirectObjectMethodVisitor extends MethodVisitor {

            public DirectObjectMethodVisitor(int api, MethodVisitor mv) {
                super(api, mv);
            }

//            @Override
//            public void visitTypeInsn(int opcode, String type) {
//                //note that the Array Transformation should be done before so that we do not have instances of arrays
//                if (opcode == Opcodes.INSTANCEOF) {
//                    super.visitLdcInsn(Type.getObjectType(type));
//                    super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.MOKAPOT_DIRECT_ACCESS_NAME, PathConstants.IS_INSTANCE_METHOD, "(Ljava/lang/Object;Ljava/lang/Class;)Z", false);
//                }
//                else {
//                    super.visitTypeInsn(opcode, type);
//                }
//            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                if (opcode == Opcodes.IF_ACMPNE) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.MOKAPOT_DIRECT_ACCESS_NAME, PathConstants.IS_EQUAL_METHOD, "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                    super.visitJumpInsn(Opcodes.IFEQ, label);
                } else {
                    super.visitJumpInsn(opcode, label);
                }
            }
//
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (name.equals("getClass") && desc.equals("()Ljava/lang/Class;")) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.MOKAPOT_DIRECT_ACCESS_NAME, PathConstants.GET_CLASS_METHOD, "(Ljava/lang/Object;)Ljava/lang/Class;", itf);
                    //               }
//                else if (owner.equals("java/lang/Class") && name.equals("cast")) {
//                    super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.MOKAPOT_DIRECT_ACCESS_NAME, PathConstants.GET_CAST_METHOD, "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", itf);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        }
    }

}
