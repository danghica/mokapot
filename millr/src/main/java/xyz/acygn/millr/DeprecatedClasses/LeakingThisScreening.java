///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.objectweb.asm.ClassReader;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.Type;
//import org.objectweb.asm.tree.AbstractInsnNode;
//import org.objectweb.asm.tree.ClassNode;
//import org.objectweb.asm.tree.FieldInsnNode;
//import org.objectweb.asm.tree.FieldNode;
//import org.objectweb.asm.tree.InsnList;
//import org.objectweb.asm.tree.InsnNode;
//import org.objectweb.asm.tree.MethodInsnNode;
//import org.objectweb.asm.tree.MethodNode;
//import org.objectweb.asm.tree.VarInsnNode;
//import org.objectweb.asm.tree.analysis.Analyzer;
//import org.objectweb.asm.tree.analysis.AnalyzerException;
//import org.objectweb.asm.tree.analysis.BasicInterpreter;
//import org.objectweb.asm.tree.analysis.BasicValue;
//import org.objectweb.asm.tree.analysis.Frame;
//import org.objectweb.asm.tree.analysis.Interpreter;
//import org.objectweb.asm.tree.analysis.SourceInterpreter;
//import org.objectweb.asm.tree.analysis.SourceValue;
//import org.objectweb.asm.tree.analysis.Value;
//import xyz.acygn.millr.messages.MessageUtil;
//
///**
// *
// * @author thomasc
// */
//public class LeakingThisScreening extends Transformation {
//
//    public LeakingThisScreening(SubProject sb) throws Exception {
//        super(sb);
//    }
//
//    @Override
//    public Instance getNewInstance(ClassReader cr) {
//        return new leakingThisInstance(cr);
//    }
//
//    static Type TYPE_THIS = Type.getType("ourStupidDescriptor");
//    static BasicValue THIS_VALUE = new BasicValue(TYPE_THIS);
//
//    class leakingThisInstance extends Instance {
//
//        public leakingThisInstance(ClassReader cr) {
//            super(cr);
//            cv = new ClassNode(Opcodes.ASM6);
//            cr.accept(cv, 0);
//            for (MethodNode mn : ((ClassNode) cv).methods) {
//                try {
//                    screenForThisLeak(mn, cr.getClassName());
//                } catch (AnalyzerException ex) {
//                    MessageUtil.error(ex);
//                }
//            }
//
//        }
//    }
//
//    public void screenForThisLeak(MethodNode mn, String owner) throws AnalyzerException {
//        Analyzer<SourceValue> a = new Analyzer(new SourceInterpreter());
//        if ((mn.access & Opcodes.ACC_STATIC) != 0) {
//            return;
//        }
//        a.analyze(owner, mn);
//        Frame<SourceValue>[] frames = a.getFrames();
//        InsnList listInstru = mn.instructions;
//        AbstractInsnNode node = listInstru.getFirst();
//        int i = 0;
//        while (node != null) {
//            //"this" is considered leaked if it is returned;
//            if (node instanceof InsnNode && ((InsnNode) node).getType() == Opcodes.ARETURN) {
//                SourceValue returnValue = frames[i].getStack(frames[i].getStackSize() - 1);
//                if (valueMayBeThis(returnValue)) {
//                    MessageUtil.message("THe method blabla leaks");
//                }
//            }
//            //"this" is considered leaked if it is put in a field of another class.
//            if (node instanceof FieldInsnNode) {
//                FieldInsnNode fieldNode = (FieldInsnNode) node;
//                if ((fieldNode.getOpcode() == Opcodes.PUTSTATIC || fieldNode.getOpcode() == Opcodes.PUTFIELD) && !owner.equals(fieldNode.owner)) {
//                    SourceValue fieldValue = frames[i].getStack(frames[i].getStackSize() - 1);
//                    if (valueMayBeThis(fieldValue)) {
//                        MessageUtil.message("THe method blabla leaks");
//                    }
//                }
//            }
//            // this is considered leaked if this is passed as argument to a method of another class.
//            if (node instanceof MethodInsnNode) {
//                MethodInsnNode methodNode = (MethodInsnNode) node;
//                int NumberOfArg = Type.getMethodType(methodNode.desc).getArgumentTypes().length;
//                int stackSize = frames[i].getStackSize();
//                for (int argStack = 0; argStack < NumberOfArg; argStack++) {
//                    if (valueMayBeThis(frames[i].getStack(stackSize - argStack))) {
//                        MessageUtil.message("THe method blabla leaks");
//                    }
//
//                }
//            }
//            i++;
//            node = node.getNext();
//        }
//    }
//
//    private boolean valueMayBeThis(SourceValue source) {
//        return source.insns.stream().anyMatch(this::loadingThisPredicate);
//    }
//
//    private boolean loadingThisPredicate(AbstractInsnNode node) {
//        if (node instanceof VarInsnNode) {
//            VarInsnNode varNode = (VarInsnNode) node;
//            return varNode.var == 0;
//        }
//        return false;
//    }
//
//}
