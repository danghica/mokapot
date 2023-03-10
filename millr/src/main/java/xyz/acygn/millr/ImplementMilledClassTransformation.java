/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import xyz.acygn.millr.Mill;
import xyz.acygn.millr.PathConstants;
import xyz.acygn.millr.SubProject;
import xyz.acygn.millr.Transformation;

/**
 * This transformation makes every milled class implements the
 * interface MILLR_INTERFACE. This ones allows us to distinguish between classes
 * that have been milled and unmilled classes. Furthermore, each milledClass now
 * implements a public method getOriginalClass that returns the original class.
 * <p>
 * This Transformation cannot be called alone, it has to work in conjunction with Mill.
 *
 * @author thomasc
 */
public class ImplementMilledClassTransformation extends Transformation {

    /**
     * Implements a milled class transformation on the specified sub-project.
     *
     * @param sp sub-project on which transformation is applied
     * @throws Exception
     */
    ImplementMilledClassTransformation(SubProject sp) throws Exception {
        super(sp);
/*        super(sp.CollectionClassReader);
        applyTransformation();
        sp.CollectionClassReader = getClassWriter().stream().map(e -> new ClassReader(e.toByteArray())).collect(Collectors.toSet());
        for (String newClassName : sp.getCollectionClassName()) {
            String previousClassName = getUnmillrName(newClassName);
            Path newFilePath = getMillrPath(sp.fromNameClassToPath.get(previousClassName));
            sp.fromNameClassToPath.remove(previousClassName);
            sp.fromNameClassToPath.put(newClassName, newFilePath);
        }
        sp.updateCollectionClassName();
        if (sp.isJar) {
            sp.outputJar = getMillrPath(sp.outputJar.toPath()).toFile();
        }
        sp.updateCollectionClassName();
        sp.checkConsistencyFromNameToPaths();*/
    }

    @Override
    String getNameOfTheTransformation() {
        return "add millr interface marker Transformation";
    }

    public ImplementMilledClassTransformation(List<ClassReader> listClassReaders) {
        super(listClassReaders);
    }


    /**
     * Returns a new transformation instance for a millred transformation.
     *
     * @param cr The ClassReader we want to Transform.
     * @return a new MillredTransformationInstance
     */
    Instance getNewInstance(ClassReader cr) {
        return new MillredTransformationInstance(cr);
    }



    /**
     * Given the file of a class, returns the corresponding file with _millr_
     * attached to the name of the class.
     *
     * @param file A file of a class
     * @return The file corresponding to the same class but milled.
     */
    private static File getMillrFile(File file) {
        return getMillrPath(file.toPath()).toFile();
    }

    /**
     * Each class will be milled to a class with a new name: this method returns
     * the new name.
     *
     * @param className The internal name of the class before being milled.
     * @return The internal name of the class after being milled.
     */
    private static String getMillrName(String className) {
        int indexLastSlash = 0;
        while (className.indexOf("/", indexLastSlash + 1) != -1) {
            indexLastSlash = className.indexOf("/", indexLastSlash + 1);
        }
        String returnString = (indexLastSlash != 0) ? className.subSequence(0, indexLastSlash + 1) + "_millr_" + className.substring(indexLastSlash + 1) : "_millr_" + className;
        return returnString;
    }


    private static Path getMillrPath(Path path) {
        String windows = ".*[wW][iI][nN][dD][oO][wW][sS].*";
        String pathString = path.toString();

        // if on windows, reformat pathString
        if (System.getProperty("os.name").matches(windows))
            pathString = pathString.replace("\\", "/");

        // obtain new path
        if (pathString.endsWith(".class")) {
            String nameClass = pathString.substring(0, pathString.length() - 6);
            return new File(getMillrName(nameClass) + ".class").toPath();
        }
        else if (pathString.endsWith(".jar")) {
            String nameJar = pathString.substring(0, pathString.length() - 4);
            return new File(getMillrName(nameJar) + ".jar").toPath();
        }
        else {
            throw new UnsupportedOperationException("getMillrPath only works for paths that millr deals with, that is, path of *.class or *.jar files" + "\n Path feed as input = " + path.toString());
        }
    }

    /**
     * Each class will be milled to a class with a new name: this method returns
     * the new name.
     *
     * @param className The internal name of the class before being milled.
     * @return The internal name of the class after being milled.
     */
    static String getMaybeMillrName(String className) {
        if (!Mill.getInstance().getAllClassReaders().stream().anyMatch(e -> e.getClassName().equals(className))) {
            return className;
        }
        return getMillrName(className);
    }

    /**
     * Given the internal name of a class being milled, returns the name of the
     * original class. It is the inverse transformation of the method
     * getMillrName. If feed with a name of a class that has not being milled,
     * it will output an error and returns the name of the input class.
     *
     * @param millrName The internal name of a class being milled.
     * @return The name of the original class.
     */
    private static String getUnmillrName(String millrName) {
        if (!millrName.contains("_millr_")) {
            // System.err.println("Tried to get the original name of a class that has not been milld " + millrName);
            return millrName;
        }
        int indexLastSlash = 0;
        while (millrName.indexOf("/", indexLastSlash + 1) != -1) {
            indexLastSlash = millrName.indexOf("/", indexLastSlash + 1);
        }
        int length = "_millr_".length();
        if (indexLastSlash != 0 && !millrName.subSequence(indexLastSlash + 1, indexLastSlash + length + 1).equals("_millr_")) {
            throw new RuntimeException("expeted: _millr_ + found: " + millrName.subSequence(indexLastSlash + 1, indexLastSlash + length + 1) + " for ClassName " + millrName);
        }
        if (indexLastSlash != 0) {
            return millrName.subSequence(0, indexLastSlash + 1) + millrName.substring(indexLastSlash + 1 + length);
        }
        else {
            return millrName.substring(length);
        }
    }

    /**
     * Given a signature or a description, returns a similar signature /
     * description but with the names of the classes updated to take into
     * account that they now have been milled.
     *
     * @param signature A signature / description of a method, class, Field...
     *                  maybe null
     * @return The new signature with class names updated to refer to the milled
     * classes.
     */
    private static String getMillrSignature(String signature) {
        if (signature == null) {
            return null;
        }
        class SignatureMillr extends SignatureWriter {

            public SignatureMillr(int api) {
                super();
            }

            @Override
            public void visitClassType(java.lang.String name) {
                super.visitClassType(getMaybeMillrName(name));
            }

        }
        SignatureReader sr = new SignatureReader(signature);
        SignatureWriter sw = new SignatureMillr(Opcodes.ASM6);
        sr.accept(sw);
        return sw.toString();
    }

    /**
     * Given an array of signatures or descriptions, returns a list of similar
     * signatures / descriptions, but with the names of the classes updated to
     * take into account that they have now been milled.
     *
     * @param signatures an array of signatures / descriptions of methods, classes, fields ...
     * @return an array of modified signatures to refer to the milled entities
     */
    private static String[] getMillrSignature(String[] signatures) {
        if (signatures == null) {
            return null;
        }
        List<String> list = Arrays.asList(signatures).stream().map(e -> getMillrSignature(e)).collect(Collectors.toList());
        return list.toArray(new String[list.size()]);
    }

    /**
     * Given a string representing the internal name of an unmilled class, returns
     * a string representing the internal name of the milled version of the class.
     *
     * @param type internal name of original class
     * @return internal name of milled class
     */
    private static String getMillrInternalName(String type) {
        if (type == null) {
            return null;
        }
        return Type.getType(getMillrSignature(Type.getObjectType(type).getDescriptor())).getInternalName();
    }

    /**
     * Given an array of strings representing the internal names of unmilled classes,
     * returns an array of strings representing the internal names of the milled versions
     * of said classes.
     *
     * @param types internal names of original classes
     * @return internal names of milled classes
     */
    private static String[] getMillrInternalName(String[] types) {
        if (types == null) {
            return null;
        }
        List<String> list = Arrays.asList(types).stream().map(e -> getMillrInternalName(e)).collect(Collectors.toList());
        return list.toArray(new String[list.size()]);
    }


    class MillredTransformationInstance extends Instance {

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

        MillredTransformationInstance(ClassReader cr) {
            super(cr);
            cw =  new ClassWriter(0);
            cv = new MillrClassVisitor(Opcodes.ASM5, cw);

        }

        @Override
        void classTransformerToClassWriter() {

        }

        /**
         * This classVisitor will add MILLR_INTERFACE to the interfaces extended
         * by the class it visits, and will implement a new method
         * getOriginalClass().
         */
        private final class MillrClassVisitor extends ClassVisitor {

            public MillrClassVisitor(int api, ClassVisitor cv) {
                super(api, cv);
            }
            
            String nameOfTheClass;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (interfaces!= null && Arrays.asList(interfaces).contains("java/lang/annotation/Annotation")){
                     super.visit(52, access, name, signature, superName, interfaces);
                     return;
                }
                if (name.equals("xyz/acygn/millr/util/MillredClass")){
                    super.visit(version, access, name, signature, superName, interfaces);
                    return;
                }
               //Set<String> interfacesSet = Arrays.asList(interfaces).stream().map(e -> getMillrInternalName(e)).collect(Collectors.toSet());
                Set<String> interfacesSet = Arrays.asList(interfaces).stream().collect(Collectors.toSet());
                interfacesSet.add(PathConstants.MILLR_INTERFACE);
                nameOfTheClass = name;
               super.visit(52, access, name, signature, superName, interfacesSet.toArray(new String[interfacesSet.size()]));
                // super.visit(52, access, getMillrInternalName(name), getMillrSignature(signature), getMillrInternalName(superName), interfacesSet.toArray(new String[interfacesSet.size()]));
                if ((access & Opcodes.ACC_INTERFACE) == 0) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "getOriginalUnmilledClass", "()Ljava/lang/Class;", null, null);
                    //   try {
                    mv.visitLdcInsn(Type.getObjectType(name));
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(2, 2);
                    mv.visitEnd();
                }
//                } catch (ClassNotFoundException ex) {
//                    throw new RuntimeException(ex);
//                }
            }

        /*


            */
        /**
             * Visit an inner class of the current Class-File, we update it with
             * new names.
             *
             * @param name      the internal name of an inner class
             * @param outerName the internal name of the class to which the inner
             *                  class belongs
             * @param innerName the (simple) name of the inner class inside its
             *                  enclosing class. May be null for anonymous inner classes.
             * @param access    access - the access flags of the inner class as
             *                  originally declared in the enclosing class.
             *//*
            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                super.visitInnerClass(getMillrInternalName(name), getMillrInternalName(outerName), getMillrInternalName(innerName), access);
            }

            *//**
             * visitOuterClass but replacing the name of the owner, the name of
             * the outerClass, and the description with updated ones.
             *//*
            @Override
            public void visitOuterClass(String owner, String name, String desc) {
                super.visitOuterClass(getMillrInternalName(owner), getMillrInternalName(name), getMillrSignature(desc));
            }

            *//**
             * The name of the file where the source is should be udpated.
             *//*
            public void visitSource(String source, String debug) {
                // todo?
            }*/
/*
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] Exceptions) {
                class ImplementationMillrMethodVisitor extends MethodVisitor {

                    public ImplementationMillrMethodVisitor(int api, MethodVisitor mv) {
                        super(api, mv);
                    }

                    *//**
                     * Visiting a fieldInstruction, but replacing the name of
                     * the owner of the field, as well as its description.
                     *
                     * @param opcode
                     * @param owner
                     * @param name
                     * @param desc
                     *//*
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        super.visitFieldInsn(opcode, getMillrInternalName(owner), name, getMillrSignature(desc));

                    }

                    *//**
                     * When visiting a frame, the objects in locals and the
                     * stack should be updated with new names.
                     *//*
                    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                        super.visitFrame(type, nLocal, modifyStackLocal(local), nStack, modifyStackLocal(stack));
                    }

                    *//**
                     * Recursively change the types in a list of Objects
                     * encoding stacks-locals in the ASM AnalyzerAdapter
                     * framework.
                     *
                     * @param array Array representing either the stack-locals
                     * @return The corresponding array with types updated.
                     *//*
                    private Object[] modifyStackLocal(Object[] array) {
                        Object[] newArray = new Object[array.length];
                        for (int i = 0; i < array.length; i++) {
                            Object o = array[i];
                            if (o instanceof String) {
                                try {
                                    newArray[i] = getMillrInternalName((String) o);
                                }
                                catch (Throwable t) {
                                    newArray[i] = array[i];
                                }
                            }
                            else {
                                newArray[i] = array[i];
                            }
                        }
                        return newArray;
                    }

                    *//**
                     * We update the visitInvokeDynamic instruction to update
                     * the names. Those ones particularly have to be updated for
                     * the handles and the bootstrapmethod arguments.
                     *//*
                    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                        super.visitInvokeDynamicInsn(name, getMillrSignature(desc), getNewHandle(bsm), getNewArgs(bsmArgs));
                    }

                    *//**
                     * Modify the methodHandle to have appropriate name and
                     * types.
                     *
                     * @param bsm A handle
                     * @return A handle with updated types.
                     *//*
                    private Handle getNewHandle(Handle bsm) {
                        return new Handle(bsm.getTag(), getMillrInternalName(bsm.getOwner()), bsm.getName(), getMillrSignature(bsm.getDesc()), bsm.isInterface());
                    }

                    *//**
                     * Given BoostrapAguments for a method handle, return new
                     * ones with appropriate types.
                     *
                     * @param bsmArgs arguments for a boostrap method
                     * @return new arguments with appropriate types.
                     *//*
                    private Object[] getNewArgs(Object... bsmArgs) {
                        List<Object> newArg = new ArrayList<Object>();
                        for (Object o : bsmArgs) {
                            if (o instanceof Handle) {
                                newArg.add(getNewHandle((Handle) o));
                            }
                            else if (o instanceof Type) {
                                newArg.add(Type.getType(getMillrSignature(((Type) o).getDescriptor())));
                            }
                            else {
                                newArg.add(o);
                            }
                        }
                        return newArg.toArray();
                    }

                    public void visitLdcInsn(Object cst) {
                        *//*
                        value - the constant to be loaded on the stack. This parameter must be a non null Integer, a Float, a Long, 
                        a Double, a String, a Type of OBJECT or ARRAY sort for .class constants, for classes whose version is 49.0, a Type of 
                        METHOD sort or a Handle for MethodType and MethodHandle constants, for classes whose version is 51.0.
                         *//*
                        if (cst instanceof Type) {
                            super.visitLdcInsn(Type.getType(getMillrSignature(((Type) cst).getDescriptor())));
                        }
                        else {
                            super.visitLdcInsn(cst);
                        }
                    }

                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        super.visitLocalVariable(name, getMillrSignature(desc), getMillrSignature(signature), start, end, index);
                    }

                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (name.equals(owner.split("/")[owner.split("/").length -1])){
                            name = getMillrName(name);
                        }
                        super.visitMethodInsn(opcode, getMillrInternalName(owner), name, getMillrSignature(desc), itf);
                    }

                    public void visitMultiANewArrayInsn(java.lang.String desc, int dims) {
                        super.visitMultiANewArrayInsn(getMillrSignature(desc), dims);
                    }

                    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                        super.visitTryCatchBlock(start, end, handler, getMillrInternalName(type));
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, java.lang.String desc, boolean visible) {
                        throw new UnsupportedOperationException("visitTypeAnnotation not yet implemented");
                    }

                    @Override
                    public void visitTypeInsn(int opcode, java.lang.String type) {
                        super.visitTypeInsn(opcode, getMillrInternalName(type));
                    }
                    

                }
                if (name.equals(nameOfTheClass.split("/")[nameOfTheClass.split("/").length - 1])){
                    name = getMillrName(name);
                }
                return new ImplementationMillrMethodVisitor(Opcodes.ASM6, super.visitMethod(access, name, getMillrSignature(desc), getMillrSignature(signature), getMillrInternalName(Exceptions)));
            }

            @Override
            public FieldVisitor visitField(int access, java.lang.String name, java.lang.String desc, java.lang.String signature, java.lang.Object value) {
                class milledFieldVisitor extends FieldVisitor {

                    milledFieldVisitor(int api, FieldVisitor fv) {
                        super(api, fv);
                    }

                    public AnnotationVisitor visitAnnotation(java.lang.String desc, boolean visible) {
                        return new milledAnnotationVisitor(api, super.visitAnnotation(getMillrSignature(desc), visible));
                    }

                    *//**
                     * todo NEEDS TO BE TESTED. In particular, the typePath stuff, I
                     * am not sure about.
                     *//*
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, java.lang.String desc, boolean visible) {
                        return new milledAnnotationVisitor(api, super.visitTypeAnnotation(typeRef, typePath, getMillrSignature(desc), visible));
                    }

                    @Override
                    public void visitAttribute(Attribute attr) {
                        throw new UnsupportedOperationException("Visit Attribute of Fields not yet implemented");

                    }

                }

                // Needs to be tested with values fields non empty.
                return new milledFieldVisitor(api, super.visitField(access, name, getMillrSignature(desc), getMillrSignature(signature), value));
            }*/


        }

        class milledAnnotationVisitor extends AnnotationVisitor {

            milledAnnotationVisitor(int api, AnnotationVisitor av) {
                super(api, av);
            }

            @Override
            public AnnotationVisitor visitAnnotation(java.lang.String name, java.lang.String desc) {
                return super.visitAnnotation(name, getMillrSignature(desc));
            }

            @Override
            public void visitEnum(java.lang.String name, java.lang.String desc, java.lang.String value) {
                super.visitEnum(name, getMillrSignature(desc), value);
            }
        }



    }
}
