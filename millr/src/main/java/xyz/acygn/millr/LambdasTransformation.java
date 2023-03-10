/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
//import  org.apache.tools.ant.AntClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.NoSuchClassException;
import xyz.acygn.millr.messages.TransformationFailedException;
import xyz.acygn.mokapot.util.Pair;

/**
 * @author thomasc
 */
public class LambdasTransformation extends Transformation {

    Set<ClassWriter> setCw;

    public LambdasTransformation(SubProject sp) {
        //Lambda Transformation is a global Transformation, this constructor is a dummy one.
        super(sp);
    }

    @Override
    public void carryTransformation(){
        //Apply transformation does all the work.
        applyTransformation();
        // We collect the Classes resulting from the transformation.
        sp.CollectionClassReader.clear();
        sp.CollectionClassReader.addAll( getClassWriter().stream().map(e -> new ClassReader(e.toByteArray())).collect(Collectors.toSet()));
        sp.checkAndRemoveNulls();
        /**
         * We update the collection of names, we now have a inconsistency : there are more classes that in the map
         * of Subproject that maps classes to paths.
         */
        sp.updateCollectionClassName();
        for (String className : sp.getCollectionClassName()) {
            if (!sp.fromNameClassToPath.containsKey(className)) {
                if (isClassGeneratedbyRetroLambda(className)) {
                    Pair<Integer, String> infoLambda = getClassLambdaIsComingFrom(className);
                    String ClassComingFrom = infoLambda.getSecond();
                    if (!sp.fromNameClassToPath.containsKey(ClassComingFrom)) {
                        MessageUtil.error(new NoSuchClassException("This case should not be happening: "
                                + "impossible to find " + ClassComingFrom + "in the project"))
                                .report()
                                .resume();
                    }
                    else {
                        String originalPath = sp.fromNameClassToPath.get(ClassComingFrom).toString();
                        String pseudoNameOfClass = originalPath.substring(0, originalPath.length() - ".class".length());
                        String newPseudoNameOfClass = getRetroLambdaName(pseudoNameOfClass, infoLambda.getFirst());
                        Path newPath = new File(newPseudoNameOfClass + ".class").toPath();
                        sp.fromNameClassToPath.put(className, newPath);
                    }
                } else if (className.endsWith("$")) {
                    String ClassComingFrom = className.substring(0, className.length() - 1);
                    if (!sp.fromNameClassToPath.containsKey(ClassComingFrom)) {
                        MessageUtil.error(new NoSuchClassException("This case should not be happening: "
                                + "impossible to find " + ClassComingFrom + "in the project")).report().resume();

                    }
                    else {
                        String originalPath = sp.fromNameClassToPath.get(ClassComingFrom).toString();
                        String pseudoNameOfClass = originalPath.substring(0, originalPath.length() - ".class".length());
                        String newPseudoNameOfClass = pseudoNameOfClass + "$.class";
                        Path newPath = new File(newPseudoNameOfClass).toPath();
                        sp.fromNameClassToPath.put(className, newPath);
                    }
                } else {
                    MessageUtil.warning("The Lambda Transformation created this className " + className + "but we do not know the original class it is coming from");
                }
            }
        }
        sp.checkConsistencyFromNameToPaths();
    }

    @Override
    String getNameOfTheTransformation() {
        return " Lambdas transformation ";
    }


    LambdasTransformation() {
        super();
    }

    public void applyTransformation() {
        // setup input file
        File inputFile = new File(InputOutput.getInstance().getTempDirectory().getPath() + "/retrolambda" + InputOutput.getInstance().generateNewMethodName());
        inputFile.deleteOnExit();
        if (inputFile.exists()) {
            InputOutput.recursiveDelete(inputFile);
        }
        inputFile.mkdirs();
        for (ClassReader cr : listClassReader) {
            try {
                InputOutput.writeClassWriter(InputOutput.fromReaderToWriter(cr), new File(inputFile, cr.getClassName() + ".class"), false);
            } catch (IOException ex) {
                MessageUtil.error(new TransformationFailedException(ex, "Failed to write the ClassReader", getNameOfTheTransformation(), cr)).report().resume();
                setCw.add(InputOutput.fromReaderToWriter(cr));
            }
        }
        try {
            final String retroLambdaPath = new File("contrib/retrolambda.jar").getCanonicalPath();
            final String classPaths = VisibilityTools.getClassPathOtherThanSubProject(sp) + inputFile.getPath();
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-Dretrolambda.inputDir=" + inputFile.getAbsolutePath(),
                    "-Dretrolambda.classpath=" + classPaths,
                    "-Dretrolambda.defaultMethods=true",
                    "-jar", retroLambdaPath,
                    "-Dretrolambda.quiet=true"
            );

            processBuilder.redirectErrorStream(true);
           // if (MessageUtil.isVerbose())
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            ProcessHandler.startProcess(process, 10, TimeUnit.SECONDS);
            process.waitFor();

            File f = inputFile;
            Filter filter = new Filter<File>() {
                @Override
                public boolean accept(File fi) {
                    return fi.toString().endsWith(".class");
                }
            };
            List<File> listFile = InputOutput.walk(f, filter);
            setCw = new HashSet<>();
            for (File file : listFile) {
                ClassReader cr = new ClassReader(new FileInputStream(file));
                String className = cr.getClassName();
                ClassWriter cw = new ClassWriter(Opcodes.ASM6);
                ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, cw) {
                };
                if (isClassGeneratedbyRetroLambda(className)) {
//                    System.out.println("ISGENERATED BY RETROLAMBDA " + className);
                    cv = new ClassVisitor(Opcodes.ASM6, cw) {

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            String[] newInterfaces = (interfaces == null) ? new String[1] : new String[interfaces.length + 1];
                            System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
                            newInterfaces[newInterfaces.length - 1] = "xyz/acygn/mokapot/markers/ComeFromRetroLambda";
                            // Note the signature of a class is as follows
                            // ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* ( visitSuperclass visitInterface* )
                            // The type signature of an object is as follows
                            // TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )
                            if (signature != null) {
                                SignatureWriter sv = new SignatureWriter() {
                                    @Override
                                    public void visitEnd() {
                                        super.visitInterface().visitClassType("xyz/acygn/mokapot/markers/ComeFromRetroLambda");
                                        super.visitEnd();
                                    }
                                };
                                SignatureReader sr = new SignatureReader(signature);
                                sr.accept(sv);
                                signature = sv.toString();
                            }
                            super.visit(version, access, name, signature, superName, newInterfaces);
                        }
                    };
                }

                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                setCw.add(cw);

            }
            isTransformationApplied = true;
            try {
                InputOutput.getInstance().addPath(inputFile);
            } catch (Throwable t) {
                MessageUtil.error(t).report().resume();
            }
        } catch (IOException | InterruptedException ex) {
            MessageUtil.error(ex).report().resume();
            isTransformationApplied = false;
            InputOutput.recursiveDelete(inputFile);
        }
    }

    @Override
    public Collection<ClassWriter> getClassWriter() {
        return setCw;
    }

    @Override
    public Instance getNewInstance(ClassReader cr) {
        return new dummyInstance();
    }

    class dummyInstance extends Instance {

        public dummyInstance() {
            super(null);
        }

        @Override
        ClassVisitor getClassTransformer() {
            return null;
        }

        @Override
        ClassWriter getClassWriter() {
            return null;
        }

        @Override
        ClassVisitor getClassVisitorChecker() {
            return null;
        }

        @Override
        void classTransformerToClassWriter() {

        }
    }

//    static class lambdasTransformationInstance extends Instance {
//
//        private File tempFile;
//        private ClassWriter tempCW;
//        Exception initializationException =  null;
//
//        lambdasTransformationInstance(ClassReader cr) {
//            super(cr);
//            String tempDir = System.getProperty("java.io.tmpdir");
//            tempCW = new ClassWriter(Opcodes.ASM5);
//            cr.accept(tempCW, 0);
//
//            tempFile = new File(tempDir + "/retrolambda/" + cr.getClassName() + ".class");
//            if (tempFile.exists()) {
//                tempFile.delete();
//            }
//            if (!tempFile.getParentFile().exists()) {
//                tempFile.getParentFile().mkdirs();
//            }
//            try {
//                Files.write(tempFile.toPath(), tempCW.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//            } catch (IOException ex) {
//                initializationException = ex;
//            }
//        }
//
//        @Override
//        void applyTransformation() throws TransformationFailedException{
//            if (initializationException!=null){
//                throw new TransformationFailedException(initializationException, "", cr);
//            }
//            super.applyTransformation();
//        }
//
//    }

    public static boolean isClassGeneratedbyRetroLambda(String internalName) {
        String[] parsing = internalName.split("\\$");
        if (isNumber(parsing[parsing.length - 1]) && parsing.length > 1) {
            if (parsing[parsing.length - 2].equals("Lambda")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNumber(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static String getRetroLambdaName(String nameInternalClass, int lambdaNumber) {
        return nameInternalClass + "$$Lambda$" + String.valueOf(lambdaNumber);
    }

    public static Pair<Integer, String> getClassLambdaIsComingFrom(String internalName) {
        if (!isClassGeneratedbyRetroLambda(internalName)) {
            throw new RuntimeException("getClassLambdaIsComingFrom has been called with a class name that has not been generated by retrolambda: " + internalName);
        }
        String[] parsing = internalName.split("\\$");
        Integer number = Integer.valueOf(parsing[parsing.length - 1]);
        StringBuilder returnName = new StringBuilder(internalName);
        while (Character.isDigit(returnName.charAt(returnName.length() - 1))) {
            returnName.deleteCharAt(returnName.length() - 1);
        }
        int lengthLambda = "$$Lambda$".length();
        int lengthString = returnName.length();
        returnName.delete(lengthString - lengthLambda, lengthString);
        return new Pair(number, returnName.toString());
    }
}
