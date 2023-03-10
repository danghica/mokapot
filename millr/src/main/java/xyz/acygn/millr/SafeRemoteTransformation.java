package xyz.acygn.millr;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;

public class SafeRemoteTransformation extends Transformation {
    @Override
    String getNameOfTheTransformation() {
        return "Safe remote transformation";
    }

    @Override
    Instance getNewInstance(ClassReader cr) {
        return new SafeRemoteInstance(cr);
    }

    class SafeRemoteInstance extends Instance{
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new SafeRemoteVisitor(Opcodes.ASM6, cw);



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

        @Override
        void classTransformerToClassWriter() {

        }

        public SafeRemoteInstance(ClassReader cr){
            super(cr);
       //     ObjectWireFormat.getT
        }




        private class SafeRemoteVisitor extends ClassVisitor {



            public SafeRemoteVisitor(int api, ClassVisitor classVisitor) {
                super(api, classVisitor);
            }





        }
    }
}
