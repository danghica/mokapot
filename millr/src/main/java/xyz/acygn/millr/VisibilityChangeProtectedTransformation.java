package xyz.acygn.millr;

import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

public class VisibilityChangeProtectedTransformation extends Transformation {


    public VisibilityChangeProtectedTransformation(SubProject sp, Map<String, List<MethodParameter>> methodToProtected) {
        super();
        this.methodToProctected = methodToProtected;
        carryConstruction(sp);
    }


    final Map<String, List<MethodParameter>> methodToProctected;

    @Override
    String getNameOfTheTransformation() {
        return "visibility Transformation : change method access to protected.";
    }

    @Override
    Instance getNewInstance(ClassReader cr) {
        return new  ChangeToProtectedInstance(cr);
    }

    class ChangeToProtectedInstance extends Instance {

        /**
         * Construct a new Instance from a given ClassReader.
         *
         * @param cr The ClassReader from which the Instance of this
         *           Transformation will operate.
         */
        ChangeToProtectedInstance(ClassReader cr) {
            super(cr);
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            try {
                if (methodToProctected.containsKey(cr.getClassName())) {
                    cv = new changeToProtectedVisitor(Opcodes.ASM6, cw, methodToProctected.get(cr.getClassName()));
                }
                else{
                    cv = new ClassVisitor(Opcodes.ASM6, cw){

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return super.visitMethod(access & ~Opcodes.ACC_FINAL, name, descriptor, signature, exceptions);
                        }


                    };
                }
            }
            catch(Throwable t){
                t.getMessage();
                throw t;
            }
        }

        ClassVisitor cv;
        ClassWriter cw;

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


        class changeToProtectedVisitor extends ClassVisitor {

            public changeToProtectedVisitor(int api, ClassVisitor cv, List<MethodParameter> methodToBeChanged) {
                super(api, cv);
                this.methodToBeChanged = methodToBeChanged;
            }

            List<MethodParameter> methodToBeChanged;


            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (methodToBeChanged == null) {
                    return super.visitMethod(access & ~Opcodes.ACC_FINAL, name, descriptor, signature, exceptions);
                } else if (methodToBeChanged.stream().anyMatch(mp -> mp.methodName.equals(name) && mp.methodDesc.equals(descriptor))) {
                    if ((access & Opcodes.ACC_PUBLIC) == 0 && (access & Opcodes.ACC_PROTECTED) == 0) {
                        int newAccess = (access & ~Opcodes.ACC_PRIVATE) + Opcodes.ACC_PROTECTED;
                        return super.visitMethod(newAccess & ~Opcodes.ACC_FINAL, name, descriptor, signature, exceptions);
                    }
                }
                return super.visitMethod(access & ~Opcodes.ACC_FINAL, name, descriptor, signature, exceptions);
            }
        }
    }
}
