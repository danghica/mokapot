package xyz.acygn.millr;

import org.objectweb.asm.ClassWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import xyz.acygn.millr.messages.NoSuchClassException;
import xyz.acygn.millr.messages.TransformationFailedException;

/**
 *
 * @author Thomas Cuvillier
 *
 * SHALL ONLY BE CALLED ON INTERFACES Every method will be replaced by the same
 * method but with a different name millrName and with (maybe) different types,
 * whereas the original will simply refer to the milled one with a wrapping /
 * unwrapping mechanism.
 *
 * Limitations known. Does not work with parametric exceptions yet. Only work if
 * one of the formal type of the class, say T, was instantiated with
 * arrayWrapper, and we convert it into array. Also, for it to work no method
 * should have argument/return type with T strictly inside it, For instance, it
 * won't work if the return type of one of the method is, for instance, List[T].
 */
public class ArrayInterfaceWrapUnwrapTransformation {

    //The name of the interface.
    final String name;

    //The signature of the original Object.
    final String signatureObject;

    //The signature of the object we want to create.
    final String signatureGoal;

    //The list of Type Parameters. Should be the same for the signature and signatueGoals.
    List<String> listSignatureGoalTypeVariables;

    //The instantiation of the type Parameters of the original Object. Tipically, one of them will be mapped to an ArrayWrapper type.
    final Map<String, String> fromTypeArgToParameter;

    //The instantation of the types Parameters desired.
    final Map<String, String> fromTypeArgToExpectedParameter;

    final ClassWriter cw;
    
        public final static String updateMethodName = "millrUpdate";

    /**
     * Create a new class that implements a generic interface [T] I[T],
     * specified by name, from a current implementation instantiation I[G] such
     * that G contains arrayWrapper.
     *
     * The signature of the interface must contain some ArrayWrapper types,
     * otherwise an exception is generated.
     *
     * @param name the name of the interface to be implemented
     * @param signatureObject the signature with arrayWrapper of the current
     * implementation.
     * @param signatureGoal, the version expected by the interface, without
     * arrayWrapper
     * @throws IOException throws exception if the system cannot find the
     * classfile whose class has internal name the param name.
     *
     *
     */
    public ArrayInterfaceWrapUnwrapTransformation(String name, String signatureObject, String signatureGoal) throws NoSuchClassException, TransformationFailedException {
        if (!signatureObject.contains("ArrayWrapper")) {
            throw new RuntimeException("The signature does not contain arrayWrapper types" + signatureObject);
        }
        

            ClassReader cr = Mill.getInstance().getClassReader(name);
            if ((cr.getAccess() & Opcodes.ACC_INTERFACE) == 0){
                throw new RuntimeException("We are trying to wrap/unwrap a class " + name + "whereas this can only work for interfaces");
            }
        
 

        //I think this does not work then the class is an internal class. Shall be tested.
        this.name = name;
        this.signatureObject = signatureObject;
        this.signatureGoal = TypeUtil.fromSignatureToDesc(signatureGoal);
        listSignatureGoalTypeVariables = new ArrayList<>();

        //Local class that allows us to parse an interface signature to collect the type parameters, type arguements...
        class TypeSignatureCollector extends SignatureVisitor {

            // Note the signature of a class is as follows
            // ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* ( visitSuperclass visitInterface* )
            // The type signature of an object is as follows
            // TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )
            private List<SignatureWriter> listTypeArguementsWriter;
            private List<String> listTypeVariable;

            /**
             * Constructor for a TypeSignatureCollector. This class will be used
             * to parse the signature of a class, in order to collect the list
             * of arguments, and the list of type parameters. Usage consists of
             * passing two lists as inputs to the constructor, that will then be
             * filled with the arguments and type parameters.
             *
             *
             *
             * @param API the API of ASM.
             */
            TypeSignatureCollector(int API) {
                super(API);
                listTypeArguementsWriter = new ArrayList<>();
                listTypeVariable = new ArrayList<>();
            }

            TypeSignatureCollector(int API, List<SignatureWriter> listArguement, List<String> listTypeParameter) {
                super(API);
                this.listTypeArguementsWriter = listArguement;
                this.listTypeVariable = listTypeParameter;
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                SignatureWriter sw = new SignatureWriter();
                listTypeArguementsWriter.add(sw);
                return sw;
            }

            @Override
            public void visitTypeArgument() {
                SignatureWriter sw = new SignatureWriter();
                listTypeArguementsWriter.add(sw);
                // From now on, every instructions should be directed to this classWriter.
            }

            @Override
            public void visitTypeVariable(String name) {
                SignatureWriter sw = new SignatureWriter();
                sw.visitTypeVariable(name);
                listTypeArguementsWriter.add(sw);
                if (listTypeVariable != null) {
                    listTypeVariable.add(name);
                }
            }

            /**
             * get The List of Parameters
             *
             * @return the list of parameters of the original method signature
             */
            public List<String> getListTypeVariable() {
                return listTypeVariable;
            }

            /**
             * get the list of Arguments
             *
             * @return the list of type of the arguments of the original method
             * signature
             */
            public List<String> getListArguments() {
                return listTypeArguementsWriter.stream().map(e -> e.toString()).collect(Collectors.toList());
            }
        }
        SignatureReader sr = new SignatureReader(signatureObject);
        TypeSignatureCollector sv = new TypeSignatureCollector(Opcodes.ASM5);
        sr.accept(sv);
        // We collect the list of arguements of the original object
        List<String> listSignatureArguements = sv.getListArguments();

        sr = new SignatureReader(signatureGoal);
        sv = new TypeSignatureCollector(Opcodes.ASM5);
        sr.accept(sv);

        // We collect the list of arguements of the object that we must create
        final List<String> listSignatureArgExpected = sv.getListArguments();

        //We collect the list of type variables. Note that we could also have 
        // done it on the first signaturevisitor when visiting signatureObject.
        listSignatureGoalTypeVariables = sv.getListTypeVariable();

        //We collect the formal types of the class.
        final List<String> listFormalTypeParameter = new ArrayList<>();
        ClassReader originalInterface = Mill.getInstance().getClassReader(name);
        originalInterface.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                SignatureReader srr = new SignatureReader(signature);
                srr.accept(new SignatureVisitor(Opcodes.ASM5) {
                    //// ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* ( visitSuperclass visitInterface* )

                    @Override
                    public void visitFormalTypeParameter(String name) {
                        listFormalTypeParameter.add(name);
                    }
                });
            }
        }, Opcodes.ASM5);
        fromTypeArgToParameter = new ConcurrentHashMap<>();
        fromTypeArgToExpectedParameter = new ConcurrentHashMap<>();
        for (int i = 0; i < listSignatureArguements.size(); i++) {
            if (listSignatureArguements.get(i).contains("ArrayWrapper")) {
                if (!Arrays.asList(PathConstants.LIST_ARRAY_WRAPPER).contains(Type.getType(listSignatureArguements.get(i)).getInternalName())) {
                    throw new RuntimeException("millring impossible due to signature" + listSignatureArguements.get(i) + " in signature " + signatureObject);
                } else {
                    fromTypeArgToParameter.put(listFormalTypeParameter.get(i), listSignatureArguements.get(i));
                    fromTypeArgToExpectedParameter.put(listFormalTypeParameter.get(i), listSignatureArgExpected.get(i));
                }
            }
        }
        if (!fromTypeArgToExpectedParameter.keySet().equals(fromTypeArgToParameter.keySet())) {
            throw new RuntimeException("discrepancy between the two sets fromTypeArgToExcpectedParameter and fromTypeArgToParameter");
        }
        //The classWriter that will write the proxy class.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        //The classVisitor visiting the original interface.
        ClassVisitor cv = new WrapUnwrapVisitor(Opcodes.ASM5, cw);
        //The classReader reading the interface.
        try {
            cr.accept(cv, Opcodes.ASM5);
        } catch (Exception e) {
            throw new TransformationFailedException(e, "Failed to create the proxy class converting interface of type" +
                    signatureObject + "to" + signatureGoal, "Interface wrap / unwrap Transformation ", cr);
        }
        //We write the classWriter
        this.cw = cw;
    }

    public ClassWriter getClassWriter() {
        return cw;
    }

    /**
     * Return the name of the class generated, that will act as a proxy between
     * the object with a signature @signatureObject belonging to a class @name
     * to make it appears as an object of class @signatureGoal
     *
     * @param name Name of the original class
     * @param signatureObject The signature of the object.
     * @param signatureGoal The desired signature of the proxy.
     * @return The name of the class generated, that acts as a proxy.
     */
    public static String getNameConvertingClass(String name, String signatureObject, String signatureGoal) {
        String hashCode = String.valueOf(signatureObject.hashCode()) + String.valueOf(signatureGoal.hashCode());
        return "millrSpecialClasses/" + name + hashCode.substring(1);
    }

    /**
     * For internal use. Return the name of the class that this object will
     * write.
     *
     * @return Name of the class written.
     */
    public String getNameConvertingClass() {
        return getNameConvertingClass(name, signatureObject, signatureGoal);
    }

    class WrapUnwrapVisitor extends ClassVisitor {

        public WrapUnwrapVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }
        //The name of the proxy class.
        String nameClass;
        //The description of the interface.
        String descInterface;
        // The name of the original interface.
        String nameInterface;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            SignatureWriter signatureClass = new SignatureWriter();
            //ClassSignature:  FormalTypeParametersopt SuperclassSignature SuperinterfaceSignature*
            for (String formalType : listSignatureGoalTypeVariables) {
                signatureClass.visitFormalTypeParameter(formalType);
            }
            signatureClass.visitSuperclass();
            signatureClass.visitClassType("java/lang/Object");
            SignatureVisitor sv = signatureClass.visitInterface();
            sv.visitClassType("xyz/acygn/millr/util/InterfaceWrapUnwrap");
            sv.visitEnd();
            SignatureReader sr = new SignatureReader(signatureGoal);
            sr.accept(signatureClass);
            nameInterface = name;
            nameClass = getNameConvertingClass();
            descInterface = Type.getObjectType(name).getDescriptor();
            access = removeAbstract(access & (~Opcodes.ACC_INTERFACE));
            super.visit(version, access, nameClass, signatureClass.toString(), "java/lang/Object", new String[]{name, "xyz/acygn/millr/util/InterfaceWrapUnwrap"});
            FieldVisitor fw = super.visitField(Opcodes.ACC_PRIVATE, "internal", descInterface, signatureObject, null);
            fw.visitEnd();
            
            FieldVisitor fv = super.visitField(0, nameOfArrayWrapperList, descOfArrayWrapperList, "Ljava/util/List<Lxyz/acygn/millr/util/ArrayWrapper;>;", null);
            fv.visitEnd();

            FieldVisitor fvv = super.visitField(0, nameOfArraylist, descOfArrayList, "Ljava/util/List<Ljava/lang/Object;>;", null);
            fvv.visitEnd();

            //We construct the constructor.
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + descInterface + ")V", "(" + signatureObject + ")V", null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, nameClass, "internal", descInterface);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
            mv.visitFieldInsn(Opcodes.PUTFIELD, nameClass, nameOfArrayWrapperList, descOfArrayWrapperList);
             mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
            mv.visitFieldInsn(Opcodes.PUTFIELD, nameClass, nameOfArraylist, descOfArrayList);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
            udpateTheArrayWrapperMethod(cv);

        }

        /**
         * Given an integer representing the access policy of a class, remove
         * the abstract from it.
         *
         * @param access The access integer of a class
         * @return The same integer if original class was abstract, otw with
         * abstract removed.
         */
        private int removeAbstract(int access) {
            return access & (~Opcodes.ACC_ABSTRACT);
        }

        /**
         * Wrap a method in the case where this one does not need any wrapping
         * unwrapping. That is, it will create a new method that will simply
         * call the original one on the referent object.
         *
         * @param access The access of the method
         * @param name The name of the method
         * @param desc The description of the method
         * @param signature The signature of the method
         * @param Exceptions The exceptions returned by this method.
         */
        public void simpleRedirect(int access, String name, String desc, String signature, String[] Exceptions) {
            MethodVisitor mv = super.visitMethod(removeAbstract(access), name, desc, signature, Exceptions);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, "internal", descInterface);
            Type[] argType = Type.getArgumentTypes(desc);
            int loc = 1;
            for (Type T : argType) {
                mv.visitVarInsn(T.getOpcode(Opcodes.ILOAD), loc);
                loc += T.getSize();
            }
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, nameInterface, name, desc, true);
            mv.visitInsn(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private String nameOfArraylist = "millrListOfArray";
        private String nameOfArrayWrapperList = "millrListOfArrayWrapper";
        private String descOfArrayList = "Ljava/util/List;";
        private String descOfArrayWrapperList = "Ljava/util/List;";

        private void addingToArrayList(MethodVisitor mv) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArraylist, descOfArrayList);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(Opcodes.POP);
        }

        private void addingToArrayWrapperList(MethodVisitor mv) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArrayWrapperList, descOfArrayWrapperList);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(Opcodes.POP);
        }

    

        /**
         * Generate the following method.          
         * <code> public void updateArray(){
         *   for (int i = 0; i <listArrayWrapper.size(); i++){
         *       if (listArrayWrapper.get(i) != null){
         *           listArrayWrapper.get(i).update(listArray.get(i))  ;
         *           }
         *      }
         *   }
         * </code>
         * @param cv
         */
        private void udpateTheArrayWrapperMethod(ClassVisitor cv) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, updateMethodName, "()V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, 1);
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArrayWrapperList, "Ljava/util/List;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true);
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArrayWrapperList, "Ljava/util/List;");
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
            Label l2 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArrayWrapperList, "Ljava/util/List;");
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "xyz/acygn/millr/util/ArrayWrapper");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, nameOfArraylist, "Ljava/util/List;");
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "xyz/acygn/millr/util/ArrayWrapper", "update", "(Ljava/lang/Object;)V", true);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitIincInsn(1, 1);
            mv.visitJumpInsn(Opcodes.GOTO, l0);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] Exceptions) {
            // If the signature of the method is null, then it does not rely of type variable, and we can simply redirect.
            if (signature == null) {
                simpleRedirect(access, name, desc, signature, Exceptions);
                return null;
            } else {
                String[] methodTypes = MethodSignatureParser.getAllParam(signature);
                class booleanWrapper {
                    boolean bool = false;
                }
                final booleanWrapper needComplexRewrite = new booleanWrapper();
                // We start by examining if a complex rewrite is needed, or if we can simply redirect.
                for (String type : methodTypes) {
                    SignatureReader sr = new SignatureReader(type);
                    // The algorithm is as follows:
                    // We examine each type (wether arguement, return, or signature) of the method, looking if it contains one of the typevariable 
                    // whose instantiation needs to be re-examined.
                    // If it is the case, we make sure that the type of the arguement is simply the one of the type-variable,
                    // Otherwise we are not able to millr it, and we throw an exception.
                    SignatureVisitor sv = new SignatureVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitTypeVariable(String name) {
                            if (fromTypeArgToExpectedParameter.keySet().contains(name) && !type.equals("T" + name + ";")) {
                                throw new RuntimeException("impossible to unwrap method whose signature is " + signature + "due to arguement" + type);
                            }
                            needComplexRewrite.bool = true;
                        }
                    };
                    sr.accept(sv);
                }
                //If we do not need complexRewrite, we simply redirect.
                if (needComplexRewrite.bool == false) {
                    simpleRedirect(access, name, desc, signature, Exceptions);
                    return null;
                } else {
                    // Not yet implemented for exceptions.
                    for (String arg : MethodSignatureParser.getExceptions(signature)) {
                        SignatureReader sr = new SignatureReader(arg);
                        SignatureVisitor sv = new SignatureVisitor(Opcodes.ASM5) {
                            @Override
                            public void visitTypeVariable(String name) {
                                if (fromTypeArgToExpectedParameter.keySet().contains(name) && !arg.equals("T" + name)) {
                                    throw new UnsupportedOperationException("Need to wrap / unwrap an exception");
                                }
                            }
                        };
                    }
                    String newSignature;
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter() {
                        @Override
                        public void visitTypeVariable(String name) {
                            if (fromTypeArgToExpectedParameter.keySet().contains(name)) {
                                char[] instatiationOfTheType = fromTypeArgToExpectedParameter.get(name).toCharArray();
                                for (char c : instatiationOfTheType) {
                                    super.visitBaseType(c);
                                }
                            }
                        }
                    };
                    sr.accept(sw);
                    newSignature = sw.toString();
                    //This is the signature of the method we will produce.
                    MethodVisitor mv = super.visitMethod(removeAbstract(access), name, desc, newSignature, Exceptions);
                    String[] arguementsSignature = MethodSignatureParser.getArguements(signature);
                    String[] arguementsDesc = MethodSignatureParser.getArguements(desc);
                    Type[] arguementsType = Type.getArgumentTypes(desc);
                    if (arguementsSignature.length != arguementsDesc.length) {
                        throw new RuntimeException("discrepancy between the lengths of arrays arguementSignature and arrays of arguementDesc");
                    }
                    //We get the referent
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, nameClass, "internal", descInterface);

                    int loc = 1;
                    for (int i = 0; i < arguementsSignature.length; i++) {
                        mv.visitVarInsn(arguementsType[i].getOpcode(Opcodes.ILOAD), loc);
                        Type T = Type.getType(arguementsDesc[i]);
                        if (fromTypeArgToExpectedParameter.containsKey(arguementsSignature[i])) {
                            String typeArg = arguementsSignature[i];
                            if (fromTypeArgToExpectedParameter.get(typeArg).startsWith("[")) {
                                addingToArrayList(mv);
                                ArrayRewriteMethodAdapter.unwrap(fromTypeArgToParameter.get(typeArg), TypeUtil.fromSignatureToDesc(fromTypeArgToExpectedParameter.get(typeArg)), mv);
                                addingToArrayWrapperList(mv);
                            }

                            loc += 1;
                        } else {
                            loc += T.getSize();
                        }
                    }
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, nameInterface, name, desc, true);
                    String returnSignature = MethodSignatureParser.getReturn(signature);
                    sr = new SignatureReader(returnSignature);
                    final StringBuilder returnTypeVariable = new StringBuilder();
                    SignatureVisitor sv = new SignatureVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitTypeVariable(String name) {
                            returnTypeVariable.append(name);
                        }
                    };
                    sr.accept(sv);
                    String typeArg = returnTypeVariable.toString();
                    if (fromTypeArgToExpectedParameter.containsKey(typeArg)) {
                        if (fromTypeArgToExpectedParameter.get(typeArg).startsWith("[")){
                            mv.visitTypeInsn(Opcodes.CHECKCAST, (Type.getType(fromTypeArgToParameter.get(typeArg))).getInternalName());
                            addingToArrayWrapperList(mv);
                            ArrayRewriteMethodAdapter.unwrap(fromTypeArgToParameter.get(typeArg), TypeUtil.fromSignatureToDesc(fromTypeArgToExpectedParameter.get(typeArg)), mv);
                            addingToArrayList(mv);
                        }
                    }
                    mv.visitInsn(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
            }
            return null;
        }

    }

}
