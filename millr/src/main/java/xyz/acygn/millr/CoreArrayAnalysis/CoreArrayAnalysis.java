/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.CoreArrayAnalysis;

import org.objectweb.asm.Opcodes;
import xyz.acygn.millr.CoreArrayAnalysis.Tree.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import xyz.acygn.millr.InputOutput;
import xyz.acygn.millr.Mill;
import xyz.acygn.millr.SubProject;
import xyz.acygn.millr.VisibilityTools;

/**
 *
 * @author thomasc
 */
public class CoreArrayAnalysis {

    List<ClassReader> listClassJVM = new ArrayList<>();

    /**
     *
     * Only here for testing purposes: Allows us to run the CoreArrayAnalysis
     * module independently of millr.
     *
     * @param args the command line arguments
     * @throws IOException if it cannot find the jvm.
     */
    public static void main(String[] args) throws IOException, Exception {
        if (args == null || args.length == 0) {
            throw new RuntimeException("Arguement missing");
        }
        Collection<SubProject> colSp = InputOutput.getInstance().readCommandLineIntoSubproject(args);
        CoreArrayAnalysis cor = new CoreArrayAnalysis();
        cor.coreArrayInitialize(colSp);
    }

    public void coreArrayInitialize(Collection<SubProject> col) throws IOException, Exception {
        String locationJVM = System.getProperty("java.home");
        String separator = System.getProperty("file.separator");
        File locationJvm = new File(new File(locationJVM).getCanonicalFile(), "lib/rt.jar");
        List<ClassReader> listClassReader = new ArrayList<>();
        System.out.println(locationJVM.toString());
        // We load all the classes that are present in the ClassLoader classpaths except the one 
        //where the subproject belongs.
        listClassJVM = InputOutput.readClassesFromJar(locationJvm, e -> (e.getName().endsWith(".class") && (e.toString().startsWith("java") || e.toString().startsWith("javax") || e.toString().startsWith("org"))));
        String classPath = VisibilityTools.getClassPathOtherThanSubProject(col);
        classPath = removeMillrMokapotClassPath(classPath);
        String[] paths = classPath.split(System.getProperty("path.separator"));
        for (String path : paths) {
            if (path.endsWith(".jar")) {
                /*
   The java.*, javax.* and org.* packages documented in the Java Platform Standard Edition API Specification make up the official, supported, public interface.
    If a Java program directly calls only API in these packages, it will operate on all Java-compatible platforms, regardless of the underlying OS platform. 

    The sun.* packages are not part of the supported, public interface.
    A Java program that directly calls into sun.* packages is not guaranteed to work on all Java-compatible platforms. In fact, such a program is not guaranteed to work even in future versions on the same platform. 
                 */


                listClassJVM.addAll(InputOutput.readClassesFromJar(new File(path).getCanonicalFile()));
            } else {
                Set<File> listClasses = new HashSet<>();
                listClasses.addAll(InputOutput.walk(new File(path), e -> e.getName().endsWith(".class")));
                for (File file : listClasses) {
                    ClassReader cr = new ClassReader(new FileInputStream(file.getAbsoluteFile()));
                    try {
                        if (listClassJVM.stream().anyMatch(e -> e.getClassName().equals(cr.getClassName()))) {
                            throw new Exception(cr.getClassName());
                        }
                        listClassJVM.add(new ClassReader(new FileInputStream(file.getAbsoluteFile())));
                    } catch (Throwable ex) {
                        System.out.println("Failed at path " + path);
                        throw new IOException("Caused by " + file.getPath(), ex);
                    }
                }
            }

        }
        
        listClassJVM = listClassJVM.stream().filter(e-> ((e.getAccess() & Opcodes.ACC_PUBLIC) != 0)).collect(Collectors.toList());

//        for (SubProject sp : col) {
//            //We create t
//            if (sp.isJar()) {
//                listClassReader = Mill.readClassesFromJar(sp.getInputFile());
//            } else {
//                List<File> listClasses = Mill.walk(sp.getInputFile(), e -> e.getName().endsWith(".class"));
//                for (File file : listClasses) {
//                    listClassReader.add(new ClassReader(new FileInputStream(file)));
//                }
//            }
//        }

        List<ClassReader> ListClassReaderWithJVM = new ArrayList<>();
        ListClassReaderWithJVM.addAll(listClassJVM);
        ListClassReaderWithJVM.addAll(listClassReader);
        hierarchyTree = HierarchyClassBuilder.getHierarchy(ListClassReaderWithJVM);
    }

    HierarchyClassBuilder hierarchyTree;

    public boolean isClassFromJVM(ClassReader cr) {
        return listClassJVM.contains(cr);
    }

    public boolean isClassFromJVM(Node<ClassReader> node) {
        return isClassFromJVM(node.getData());
    }

    public void analyzeMethodCall(int opcode, String Class, String name, String desc) throws ClassNotFoundException {
        MethodInvocationAnalyzer.analyzeMethodCall(opcode, Class, name, desc, this);
    }

    private String removeMillrMokapotClassPath(String classPath) throws IOException {
        String separator = System.getProperty("path.separator");
        String[] paths = classPath.split(separator);
        Path millrPath = new File("classes/").getCanonicalFile().toPath();
        Path mokapotPath = new File("../build-output/classes-common/").getCanonicalFile().toPath();
        StringBuilder outputClassPath = new StringBuilder(classPath.length());
        for (String path : paths) {
            Path pathFile = new File(path).getCanonicalFile().toPath();
            if (!pathFile.startsWith(millrPath) && !pathFile.startsWith(mokapotPath)) {
                outputClassPath.append(path).append(separator);
            }
        }
        return outputClassPath.toString();

    }

}
