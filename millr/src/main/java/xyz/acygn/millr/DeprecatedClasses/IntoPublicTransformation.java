///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr.DeprecatedClasses;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.stream.Collectors;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
//import org.objectweb.asm.FieldVisitor;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.ModuleVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.signature.SignatureReader;
//import org.objectweb.asm.signature.SignatureVisitor;
//import org.objectweb.asm.signature.SignatureWriter;
//import xyz.acygn.millr.Transformation;
//
///**
// *
// * @author thomasc
// */
//public class IntoPublicTransformation {
//
//    protected List<Transformation.Instance> listTransformation;
//    Collection<ClassReader> listClassReader;
//
//
//    public IntoPublicTransformation(Collection<ClassReader> listClassReader) {
//        this.listClassReader = listClassReader;
//        listTransformation = listClassReader.stream().map(e->getNewInstance(e)).collect(Collectors.toList());
//    }
//
//
//    public void checkProgramCompatibility(){
//        listTransformation.stream().forEach(Transformation.Instance::checkCompatibility);
//    }
//
//    public void applyTransformation() {
//    }
//
//    public Collection<ClassWriter> getClassWriter() {
//        return listTransformation.stream().map(e->e.getClassWriters()).flatMap(l -> l.stream()).collect(Collectors.toList());
//    }
//
//    Transformation.Instance getNewInstance(ClassReader cr){
//        return new intoPublicTransformationInstance(cr);
//    }
//
//    public class intoPublicTransformationInstance extends Transformation.Instance{
//
//        public intoPublicTransformationInstance(ClassReader cr) {
//            super(cr);
//            cw = new ClassWriter(0);
//            cv = new intoPublicClassVisitor(Opcodes.ASM5, cw);
//        }
//
//
//        private class signatureTransformer extends SignatureWriter{
//
//            public signatureTransformer() {
//                super();
//            }
//            @Override
//            public void visitClassType(String name){
//                super.visitClassType("publicjvm/" + name);
//            }
//        }
//
//        final String newSignature(String signature){
//            SignatureReader sr = new SignatureReader(signature);
//            SignatureVisitor sv = new signatureTransformer();
//            sr.accept(sv);
//            return sv.toString();
//        }
//
//        private class intoPublicClassVisitor extends ClassVisitor{
//
//            public intoPublicClassVisitor(int api) {
//                super(api);
//            }
//
//             public intoPublicClassVisitor(int api, ClassVisitor cv) {
//                super(api, cv);
//            }
//
//
//
//            @Override
//            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
//        //        System.out.println("className" + name);
//                name = "publicjvm/" + name;
//                String newSuperName = "publicjvm/" + superName;
//                interfaces =  (interfaces==null) ? new String[0]: interfaces;
//                String[] newInterfaces = new String[interfaces.length];
//                for (int i = 0; i< interfaces.length; i++){
//                    newInterfaces[i] = "publicjvm/" + interfaces[i];
//                }
//                access = (access & ~Opcodes.ACC_PRIVATE);
//                access = access & ~Opcodes.ACC_PROTECTED;
//                access += Opcodes.ACC_PUBLIC;
//                if (name.equals("publicjvm/java/util/Collections$UnmodifiableRandomAccessList")){
//                    System.out.println("supername" + newSuperName);
//                    System.out.println("interfaces" + Arrays.toString(newInterfaces));
//                    System.out.println("signature" + signature);
//                }
//                super.visit(version, Opcodes.ACC_PUBLIC, name, (signature==null)?  null: newSignature(signature), newSuperName, newInterfaces);
//            }
//
//            @Override
//            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value){
//                access = (access & Opcodes.ACC_STATIC) + (Opcodes.ACC_ABSTRACT & access) + Opcodes.ACC_PUBLIC;
//                return super.visitField(access, name, desc, signature, value);
//            }
//
//            @Override
//            public void visitInnerClass(String name, String outerName, String innerName, int access){
//         //       System.out.println("inner class name " + name);
//                super.visitInnerClass("publicjvm/"+ name, outerName, innerName, Opcodes.ACC_PUBLIC);
//            }
//
//            @Override
//            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions){
//                 class customMethodVisitor extends MethodVisitor{
//
//                    public customMethodVisitor(int api) {
//                        super(api);
//                    }
//
//                    public customMethodVisitor(int api, MethodVisitor mv) {
//                        super(api, mv);
//                    }
//
//                    @Override
//                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf){
//                      //  System.out.println("MethodOwner" + owner);
//                        super.visitMethodInsn(opcode, "publicjvm/"+owner, name, desc, true);
//                    }
//
//                    @Override
//                    public void visitFieldInsn(int opcode, String owner, String name, String desc){
//                      //  System.out.println("FieldOwner" + owner);
//                        super.visitFieldInsn(opcode, "publicjvm"+owner, name, desc);
//                    }
//
//                 }
//                                 access = (access & ~Opcodes.ACC_PRIVATE);
//                access = access & ~Opcodes.ACC_PROTECTED;
//                access += Opcodes.ACC_PUBLIC;
//                 return	new customMethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions));
//            }
//
//            @Override
//            public ModuleVisitor visitModule(String name, int access, String version){
//                access = (access & ~Opcodes.ACC_PRIVATE);
//                access = access & ~Opcodes.ACC_PROTECTED;
//                access += Opcodes.ACC_PUBLIC;
//                return visitModule(name, access, version);
//            }
//
//
//        }
//
//
//
//    }
//}