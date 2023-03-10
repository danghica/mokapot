/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import xyz.acygn.millr.SubProject;
import xyz.acygn.millr.Transformation;

/**
 *
 * @author thomasc
 */
public class ToJavaEightTransfo extends Transformation {
    
    ToJavaEightTransfo(SubProject sb) throws Exception {
        super(sb);
    }

    @Override
    String getNameOfTheTransformation() {
        return "to java 8 Transformation";
    }

    @Override
    Instance getNewInstance(ClassReader cr){
        return new toJavaEightInstance(cr);
    }
    
    class toJavaEightInstance extends Instance{

        @Override
        ClassVisitor getClassTransformer() {
            return cv;
        }

        ClassVisitor cv;
        ClassWriter cw;



        @Override
        ClassWriter getClassWriter() {
            return cw;
        }

        @Override
        ClassVisitor getClassVisitorChecker() {
            return null;
        }

        public toJavaEightInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(0);
            cv = new ClassVisitor(Opcodes.ASM6, cw){
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
                    super.visit(52, access, name, signature, superName, interfaces);
                }
            };

        }

        @Override
        void classTransformerToClassWriter() {

        }

    }
}
