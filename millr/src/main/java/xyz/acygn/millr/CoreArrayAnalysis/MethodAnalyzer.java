/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.CoreArrayAnalysis;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.MethodVisitor;
import xyz.acygn.millr.messages.MessageUtil;


/**
 *
 * @author thomasc
 */
public class MethodAnalyzer extends AnalyzerAdapter {
    
    public MethodAnalyzer(int api, String owner, int access, String name, String desc, MethodVisitor mv) {
        super(api, owner, access, name, desc, mv);
        this.name = name;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }
    
    String name;
    boolean isStatic;
    
    
    @Override 
    public void visitVarInsn(int opcode, int var){
        if (isStatic ){
            super.visitVarInsn(opcode, var);
        }
        else{
            if (var==0 && opcode == Opcodes.AALOAD){
                stack.add("this");
            }
            else{
                super.visitVarInsn(opcode, var);
            }
        }
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String desc){
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD){
            if (!stack.get(stack.size() -1).equals("this")){
                MessageUtil.message("Possible wrong invocation of method coming from " + owner + " with name " + name + " from " + "I don't know yet ").emit();
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
    
}
