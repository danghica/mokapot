///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.ClassWriter;
//import org.objectweb.asm.FieldVisitor;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import xyz.acygn.millr.messages.NoSuchClassException;
//
///**
// * @author thomasc
// *
// *
// *
// * This class goes through the API supertypes of a given class, and check if
// * they implement methods or fields with arrays. If it is the case, it provides
// * an alternative class replacing the problematic class, and changes the type
// * hierarchy accordingly. The new classes from the type hierarchy are stored in
// * a folder that has to be provided as input. Their new names become
// * nameOfTheOutputFolder.formerName;
// */
//@Deprecated
//public class ArrayTypeHierarchyTransformation extends Transformation {
//
//    public ArrayTypeHierarchyTransformation(Collection<ClassReader> listClassReader) {
//        super(listClassReader);
//    }
//
//    @Override
//    Instance getNewInstance(ClassReader cr) {
//        return new ArrayTypeHierarchyTransformationInstance(cr);
//    }
//
//    public void applyTransformation() {
//        super.applyTransformation();
//        List<ClassWriter> listClassWriter = new ArrayList<>();
//        Set<String> setAPIClassesModified = new HashSet<>();
//        listTransformation.stream().forEach(e -> listClassWriter.addAll(e.getClassWriters()));
//        List<ClassReader> newListClassReader = new ArrayList<>();
//        listClassWriter.stream().forEach(e -> newListClassReader.add(new ClassReader(e.toByteArray())));
//        listTransformation.stream().forEach(e -> setAPIClassesModified.addAll(((ArrayTypeHierarchyTransformationInstance) e).getModifiedAPIClasses()));
//        modifyArrayTypeHierarchyTransformation modifyArrayTypeHierarchy = new modifyArrayTypeHierarchyTransformation((Collection<ClassReader>) newListClassReader, setAPIClassesModified);
//        modifyArrayTypeHierarchy.applyTransformation();
//    }
//
//    @Override
//    public List<ClassWriter> getClassWriter() {
//        List<ClassWriter> listClassWriter = new ArrayList<>();
//        listTransformation.stream().forEach(e -> listClassWriter.addAll(e.getClassWriters()));
//        return listClassWriter;
//    }
//
//    class ArrayTypeHierarchyTransformationInstance extends Instance {
//
//        private final Set<ClassWriter> setCw = new HashSet<>();
//        private final Set<String> setListAPIModified = new HashSet<>();
//
//        /**
//         * @param classReader A ClassReader corresponding to the class to be
//         * transformed;
//         */
//        ArrayTypeHierarchyTransformationInstance(ClassReader classReader) {
//            super(classReader);
//        }
//
//        /**
//         * @return returns the classWriter resulting from the transformation;
//         */
//        @Override
//        public Set<ClassWriter> getClassWriters() {
//            return setCw;
//        }
//
//        @Override
//        public void applyTransformation() {
//            setCw.add(modifyHierarchy(cr, false));
//        }
//
//        private void copySystemClass(String className) throws NoSuchClassException {
//            ClassReader classReader;
//            classReader = Mill.getInstance().getClassReader(className);
//            copySystemClass(classReader);
//        }
//
//        private void copySystemClass(ClassReader cr) {
//            setListAPIModified.add(cr.getClassName());
//            setCw.add(modifyHierarchy(cr, true));
//        }
//
//        private ClassWriter modifyHierarchy(ClassReader classReader, final boolean isAPI) {
//            ClassWriter classWriter = new ClassWriter(0);
//            ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, classWriter) {
//                @Override
//                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//                    if (isAPI == true) {
//                        name = PathConstants.API_ARRAY_FOLDER.getName() + "/" + name;
//                    }
//                    if (hasRecursiveArray(superName) && isApi(superName)) {
//                        copySystemClass(superName);
//                        superName = PathConstants.API_ARRAY_FOLDER.getName() + "/" + superName;
//
//                    }
//                    for (int i = 0; i < interfaces.length; i++) {
//                        if (hasRecursiveArray(interfaces[i]) && isApi(interfaces[i])) {
//                            copySystemClass(interfaces[i]);
//                            interfaces[i] = PathConstants.API_ARRAY_FOLDER.getName() + "/" + superName;
//
//                        }
//                    }
//
//                    super.visit(version, access, name, signature, superName, interfaces);
//
//                }
//            };
//            classReader.accept(cv, 0);
//            return classWriter;
//        }
//
//        private boolean hasRecursiveArray(String clazzName) {
//            if (clazzName == null) {
//                return false;
//            } else {
//                try {
//                    return hasRecursiveArray(Mill.getInstance().getClassReader(clazzName));
//                } catch (IOException ex) {
//                    System.err.println(clazzName);
//                    throw new RuntimeException(ex);
//                }
//            }
//        }
//
//        private boolean hasRecursiveArray(ClassReader cr) {
//            if (hasArray(cr)) {
//                return true;
//            } else {
//                for (String interfaceName : cr.getInterfaces()) {
//                    if (hasRecursiveArray(interfaceName)) {
//                        return true;
//                    }
//                }
//                if (hasRecursiveArray(cr.getSuperName())) {
//                    return true;
//                }
//                return false;
//            }
//        }
//
//        // This method actually returns if the class was loaded by the bootstrap loader.
//        private boolean isApi(String nameClass) {
//            try {
//                return isApi(Class.forName(nameClass.replace("/", ".")));
//            } catch (ClassNotFoundException ex) {
//                System.err.println(nameClass);
//                throw new RuntimeException(ex);
//            }
//        }
//
//        private boolean isApi(Class clazz) {
//            return (clazz.getClassLoader() == null);
//        }
//
//        private boolean hasArray(String classInterfaceName) {
//            try {
//                return hasArray(Mill.getInstance().getClassReader(classInterfaceName));
//            } catch (Exception ex) {
//                System.err.println(classInterfaceName);
//                throw new RuntimeException(ex);
//            }
//        }
//
//        private boolean isAccessOuterPackage(int access) {
//            return ((access & Opcodes.ACC_PROTECTED) != 0 || (access & Opcodes.ACC_PUBLIC) != 0);
//        }
//
//        private boolean hasArray(ClassReader cr) {
//            class HasArrayClassVisitor extends ClassVisitor {
//
//                boolean hasArray = false;
//
//                public HasArrayClassVisitor(int i) {
//                    super(i);
//                }
//
//                @Override
//                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
//                    if (desc.contains("[") && isAccessOuterPackage(access)) {
//                        hasArray = true;
//                        super.visitEnd();
//                    }
//                    return null;
//                }
//
////                @Override
////                public void visitInnerClass(String name, String outerName, String innerName, int access) {
////                    if (hasArray(name) && isAccessOuterPackage(access)) {
////                        hasArray = true;
////                        super.visitEnd();
////                    }
////                }
//                @Override
//                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//                    if ((desc.contains("[")) && isAccessOuterPackage(access)) {
//                        hasArray = true;
//                        super.visitEnd();
//                    }
//                    return null;
//                }
//
//                @Override
//                public void visitOuterClass(String owner, String name, String desc) {
//                    if (hasArray(owner)) {
//                        hasArray = true;
//                        super.visitEnd();
//                    }
//                }
//            }
//            HasArrayClassVisitor hasArrayClassVisitor = new HasArrayClassVisitor(Opcodes.ASM5);
//            cr.accept(hasArrayClassVisitor, 0);
//            return hasArrayClassVisitor.hasArray;
//        }
//
//        final Set<String> getModifiedAPIClasses() {
//            return setListAPIModified;
//        }
//
//    }
//}
