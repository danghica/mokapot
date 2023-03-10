/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.CoreArrayAnalysis;

import org.objectweb.asm.Type;
import xyz.acygn.millr.CoreArrayAnalysis.Tree.Node;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import xyz.acygn.millr.messages.MessageUtil;

/**
 *
 * @author thomasc
 */
public class MethodInvocationAnalyzer {

    public static void analyze(ClassReader cr, final CoreArrayAnalysis cor) {

        class analyzer extends ClassVisitor {

            public analyzer(int api) {
                super(api);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                //    System.out.println("visitingMethod");
                return new MethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {

                    @Override
                    public void visitMethodInsn(int opcode, String Class, String name, String desc, boolean itf) {
                        try {
                            analyzeMethodCall(opcode, Class, name, desc, cor);
                        } catch (ClassNotFoundException ex) {
                            MessageUtil.error(ex).report().resume();
                        }
                    }
                };
            }
        }

        ClassVisitor cv = new analyzer(Opcodes.ASM6);
        try {
            cr.accept(cv, 0);
        } catch (NullPointerException e) {
            if (cr == null) {
                MessageUtil.error(new NullPointerException("The class reader is null")).report().resume();
            }
        }

    }

    public static void analyzeMethodCall(int opcode, String Class, String name, String desc, CoreArrayAnalysis cor) throws ClassNotFoundException {
        try {

            Node<ClassReader> ClassNode = cor.hierarchyTree.getNode(Class);
            Set<Node<ClassReader>> ClassesToAnalyze;
            if (opcode == Opcodes.INVOKESTATIC) {
                ClassesToAnalyze = new HashSet<Node<ClassReader>>(1);
                ClassesToAnalyze.add(ClassNode);
            }
            if (opcode == Opcodes.INVOKEINTERFACE) {
                // We need to fill that out.
                ClassesToAnalyze = new HashSet<Node<ClassReader>>();
            } else {
                ClassesToAnalyze = ClassNode.getRecursiveChildren();
                ClassesToAnalyze.add(ClassNode);
                ClassesToAnalyze.addAll(ClassNode.getRecursiveParents());
            }
            //   System.out.println(ClassesToAnalyze.size());
            ClassesToAnalyze.stream().filter(cor::isClassFromJVM).forEach(e -> getImplementation(opcode, name, desc, e.getData()));
        } catch (IOException ex) {
            throw new ClassNotFoundException(Class, ex);

        } catch (HierarchyClassBuilder.NameNotRegisteredException ex) {
            MessageUtil.error(ex).report("Impossible to find the class" + Class + " in the hierarchyTree").resume();
        }

    }

    private static void getImplementation(int opcode, String name, String desc, ClassReader cr) {
        Set<MethodVisitor> setMethod = new HashSet<>();
        class findImplementationVisitor extends ClassVisitor {

            Set<MethodVisitor> setMethod;
            String nameMethod;
            String descMethod;
            String nameClass;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.nameClass = name;
            }

            public findImplementationVisitor(int api, Set<MethodVisitor> setMethod, String nameMethod, String descMethod) {
                super(api);
                this.nameMethod = nameMethod;
                this.descMethod = descMethod;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] Exceptions) {
                if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                    return null;
                }
                if (opcode == Opcodes.INVOKESTATIC && ((access & Opcodes.ACC_STATIC) == 0)) {
                    return null;
                } else if ((opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) && ((access & Opcodes.ACC_STATIC) != 0)) {
                    return null;
                }
                if (name.equals(nameMethod) && Arrays.deepEquals(Type.getArgumentTypes(desc), Type.getArgumentTypes(descMethod))) {
                    //      System.out.println("methodToAnalyze");
                    MethodVisitor mv = new MethodAnalyzer(api, nameClass, access, name, desc, super.visitMethod(access, name, desc, signature, Exceptions));
                    return mv;
                }
                return null;
            }

        }
        ClassVisitor cv = new findImplementationVisitor(Opcodes.ASM6, setMethod, name, desc);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);

    }

}
